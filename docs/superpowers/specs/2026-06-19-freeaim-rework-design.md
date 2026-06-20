# Переработка Free-Aim для TaCZ оружия — дизайн

Дата: 2026-06-19
Мод: TaCZ Mechanics (`ru.liko.tacz_mechanics`), Minecraft 1.21.1, NeoForge 21.1.219, TaCZ.

## Цель

Переработать систему free-aim (качание/инерция оружия) так, чтобы она:
1. Работала **в ADS/прицелах** (сейчас полностью отключается).
2. Работала **на всём оружии** TaCZ.
3. Давала **точное совпадение выстрела со стволом** (пуля летит туда, куда реально смотрит наклонённая модель).
4. Была **видна от 3-го лица другим игрокам** в мультиплеере.

Модель остаётся **sway/инерция** (оружие отстаёт от камеры и догоняет), НЕ Tarkov-style deadzone.

## Что не так сейчас (база для переработки)

- `FreeAimHandler.tick()` копит дельту поворота камеры в `pitchOffset/yawOffset`, затем `lerpSpeed` **всегда тянет offset к нулю** — это не настоящая инерция, а затухание накопления. Нет "заноса с догоном".
- Реагирует **только на поворот мыши**. Нет качания от ходьбы/бега/прыжков и от отдачи.
  - Отдача TaCZ применяется к камере в фазе рендера (`CameraSetupEvent.applyCameraRecoil` через `ComputeCameraAngles`), а не в логическом `player.getXRot()`, который читает `tick()`. Поэтому отдача не порождает sway. Решается подпиской на `GunFireEvent`.
- Полностью **отключается при ADS** (`disableWhenAiming = true`).
- Совпадение ствол↔пуля приблизительное: «magic numbers» `pitchSens=0.5`, `yawSens=0.4` дублируются в `FreeAimGunModelMixin` и `FreeAimServerHandler` и не гарантируют точность.
- Нет синхронизации offset на других клиентов → 3-е лицо не качается.

## Архитектура (подход A: пружинный движок + источники)

### Физика

Пружинно-демпферная модель, 2 независимые оси (pitch, yaw). Каждая ось имеет `position` (угол отклонения ствола в градусах) и `velocity`. Шаг интеграции за тик (dt = 1 тик):

```
accel = -stiffness * position - damping * velocity
velocity += accel * dt
velocity += impulse          // импульсы от источников за этот тик
position += velocity * dt
position = clamp(position, -maxAngle, +maxAngle)
```

- Под-демпфированная пружина (малый `damping`) → лёгкий overshoot, «живой занос с догоном».
- Критически/над-демпфированная → плавный возврат без отскока.
- Поведение регулируется `stiffness` и `damping` в конфиге.

Рендер использует интерполяцию `position` по `partialTick` (`prevPosition → position`).

### Классы (пакет `ru.liko.tacz_mechanics.client.freeaim`)

- **`SwaySpring`** — чистая физика одной оси. API: `addImpulse(float)`, `update(float dt)`, `getValue()`, `getInterpolated(float pt)`, `reset()`. Без зависимостей от Minecraft → юнит-тестируется.
- **`LookDeltaSource`** — импульс из дельты поворота камеры между тиками (импульс = `-delta * look.sensitivity`; занос противоположен движению).
- **`MovementSource`** — синусоидальное качание от ходьбы/бега (амплитуда зависит от горизонтальной скорости: walk vs sprint), плюс импульсы на прыжок (отрыв от земли) и приземление (по смене `onGround` / `fallDistance`).
- **`RecoilSource`** — слушает `GunFireEvent` (CLIENT, `shooter == локальный игрок`): импульс pitch вверх + небольшой случайный yaw, масштаб из конфига `recoil.scale`.
- **`FreeAimHandler`** (рефактор существующего) — оркестратор: держит два `SwaySpring`, на каждом тике опрашивает источники, добавляет их импульсы, вызывает `update`, применяет ADS-множитель к геттерам, синхронизирует по сети. Предоставляет геттеры потребителям. Перестаёт быть «всё в одном» — физика и источники вынесены.

### ADS-множитель

Вместо полного отключения: итоговый offset, отдаваемый потребителям, умножается на `lerp(1.0, adsMultiplier, aimingProgress)`, где `aimingProgress` из `IClientPlayerGunOperator.getClientAimingProgress(pt)`. `adsMultiplier = 0` воспроизводит прежнее поведение (нет качания в прицеле).

### Потребители offset

Один offset в реальных градусах → всем потребителям (с ADS-множителем):

1. **1-е лицо** — `FreeAimGunModelMixin` (`GunItemRendererWrapper.renderFirstPerson`): наклон модели на offset вокруг pivot у плеча/глаза. Обновляется: убрать раннее отключение при ADS, применять ADS-множитель.
2. **Crosshair** — `FreeAimCrosshairMixin` (`RenderCrosshairEvent.renderCrosshair`): смещение сетки. Масштаб связан с offset через `crosshairScale`.
3. **Выстрел** — `FreeAimShootMixin` (`ModernKineticGunScriptAPI.shootOnce`) + `FreeAimServerHandler`: отклонение pitch/yaw выстрела ровно на offset.
4. **3-е лицо** — см. ниже.

### Совпадение выстрела со стволом

`position` — это **настоящий угол отклонения ствола**. Модель наклоняется ровно на этот угол вокруг pivot, выстрел отклоняется ровно на этот же угол. Совпадение точное по построению; убираем рассинхронные множители `0.5/0.4`. Финальный подбор pivot модели (чтобы ствол визуально смотрел в crosshair) — ин-гейм калибровка (F3+B / Prism).

### Мультиплеер / 3-е лицо (отдельная фаза за флагом `thirdPerson.enabled`)

- `FreeAimSyncPacket` (client→server) — остаётся: offset для выстрела и источник для рассылки.
- **`FreeAimBroadcastPacket`** (server→tracking clients) — новый: `{playerUUID, pitch, yaw}`. Сервер, получив offset от игрока, рассылает наблюдателям через `PacketDistributor.sendToPlayersTrackingEntity`. Клиенты держат `Map<UUID, offset>` с интерполяцией.
- Применение к 3-му лицу — через TaCZ `IThirdPersonAnimation` / хук в `InnerThirdPersonManager`: наклон руки/корпуса на offset наблюдаемого игрока.
- Изолировано флагом, чтобы 1-е лицо можно было довести и оттестировать раньше.

## Конфиг (`Config.FreeAim`)

Добавить/изменить:
- `freeAim.enabled` (есть)
- `freeAim.maxAngle` (есть) — clamp угла
- `freeAim.spring.stiffness` (новый)
- `freeAim.spring.damping` (новый)
- `freeAim.look.sensitivity` (новый)
- `freeAim.movement.enabled`, `movement.walkScale`, `movement.sprintScale`, `movement.jumpScale` (новые)
- `freeAim.recoil.enabled`, `recoil.scale` (новые)
- `freeAim.adsMultiplier` (новый, default ~0.35)
- `freeAim.crosshairScale` (есть)
- `freeAim.disableCrosshairMovement` (есть)
- `freeAim.thirdPerson.enabled` (новый)

Удалить: `freeAim.lerpSpeed` (→ stiffness/damping), `freeAim.disableWhenAiming` (→ adsMultiplier=0).

## Тестирование

- **Юнит-тест `SwaySpring`** (детерминированная физика): импульс затухает к нулю; при критическом damping нет overshoot; clamp по maxAngle соблюдается; интерполяция корректна.
- **Ин-гейм 1-е лицо**: сборка + run client; проверка качания от поворота/ходьбы/отдачи; калибровка совпадения ствол↔crosshair через F3+B / Prism; проверка приглушения в ADS.
- **Ин-гейм 3-е лицо**: два клиента (или наблюдение за другим игроком) — наклон оружия в руках наблюдаемого.

## Версии и точки интеграции TaCZ

- Исходники TaCZ: `/home/liko/Разработка/NeoForge/!libs and references/TACZ-1.21.1/src/main/java/com/tacz/guns/`
- 1-е лицо: `GunItemRendererWrapper.renderFirstPerson`
- Crosshair: `RenderCrosshairEvent.renderCrosshair`
- Выстрел: `ModernKineticGunScriptAPI.shootOnce`
- ADS-прогресс: `IClientPlayerGunOperator.getClientAimingProgress`
- Отдача/выстрел-событие: `GunFireEvent` (NeoForge event bus, CLIENT+SERVER)
- 3-е лицо: `InnerThirdPersonManager.setRotationAnglesHead` / `IThirdPersonAnimation`

## План по фазам (для writing-plans)

1. **Физический движок**: `SwaySpring` + юнит-тест. Рефактор `FreeAimHandler` на пружину (только LookDeltaSource), сохранить текущее поведение 1-го лица.
2. **Источники**: `MovementSource`, `RecoilSource` (+ `GunFireEvent`).
3. **ADS + совпадение выстрела**: ADS-множитель, единый угол для модели и выстрела, калибровка pivot.
4. **Конфиг**: новые параметры, удаление устаревших.
5. **3-е лицо / мультиплеер** (за флагом): `FreeAimBroadcastPacket`, клиентский кэш, хук в third-person анимацию.
