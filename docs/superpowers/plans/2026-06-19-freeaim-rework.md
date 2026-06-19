# Free-Aim Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Переработать free-aim TaCZ-оружия в пружинный движок качания, работающий в ADS, на всём оружии, с точным совпадением выстрела со стволом и видимостью от 3-го лица.

**Architecture:** Пружинно-демпферная физика (`SwaySpring`) на двух осях (pitch/yaw), питаемая источниками-импульсами (поворот камеры, движение, отдача). Один эффективный угол отклонения (с ADS-множителем) применяется ко всем потребителям — модели 1-го лица, прицелу, направлению выстрела и анимации 3-го лица.

**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.219, SpongePowered Mixin 0.8, JUnit 5 (добавляется), TaCZ (`com.tacz.guns`).

## Global Constraints

- Minecraft 1.21.1 / NeoForge 21.1.219 / Java 21 — без изменений.
- Mixin-таргеты TaCZ объявляются с `remap = false` (как в существующих миксинах).
- Новые миксины регистрируются в `src/main/resources/tacz_mechanics.mixins.json` (`mixins` — общие, `client` — клиентские).
- Конфиг — `ModConfig.Type.SERVER` через `Config.SERVER_BUILDER`; статические поля грузятся в `FreeAim.load()`.
- Сетевые пакеты регистрируются в `ModNetworking.register(...)`; client→server = `playToServer`, server→clients = `playToClient`.
- Все обращения к TaCZ API оборачивать в try/catch (как в существующем `FreeAimHandler`), мод должен переживать отсутствие/изменение TaCZ API.
- Угол offset в градусах = реальный угол отклонения ствола; модель и выстрел используют ОДИН И ТОТ ЖЕ угол (без рассинхронных множителей).
- TaCZ-исходники для справки: `/home/liko/Разработка/NeoForge/!libs and references/TACZ-1.21.1/src/main/java/com/tacz/guns/`.

---

## File Structure

**Создаются:**
- `src/test/java/ru/liko/tacz_mechanics/freeaim/SwaySpringTest.java` — юнит-тесты физики.
- `src/main/java/ru/liko/tacz_mechanics/client/freeaim/SwaySpring.java` — чистая физика одной оси.
- `src/main/java/ru/liko/tacz_mechanics/client/freeaim/RecoilSource.java` — слушатель `GunFireEvent` → импульс отдачи.
- `src/main/java/ru/liko/tacz_mechanics/client/freeaim/MovementSource.java` — импульсы ходьбы/бега/прыжков.
- `src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimClientCache.java` — кэш offset других игроков (3-е лицо).
- `src/main/java/ru/liko/tacz_mechanics/network/FreeAimBroadcastPacket.java` — server→clients рассылка offset.
- `src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimThirdPersonMixin.java` — наклон модели 3-го лица.

**Изменяются:**
- `build.gradle` — JUnit 5.
- `src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimHandler.java` — оркестратор на пружинах.
- `src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimGunModelMixin.java` — единый угол + ADS-множитель.
- `src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimCrosshairMixin.java` — без правок API, проверить совместимость геттеров.
- `src/main/java/ru/liko/tacz_mechanics/server/FreeAimServerHandler.java` — единый угол, убрать множители.
- `src/main/java/ru/liko/tacz_mechanics/Config.java` — новые параметры, удалить устаревшие.
- `src/main/java/ru/liko/tacz_mechanics/network/ModNetworking.java` — регистрация broadcast-пакета.
- `src/main/resources/tacz_mechanics.mixins.json` — новый миксин 3-го лица.

---

## Task 1: Тестовая инфраструктура (JUnit 5)

**Files:**
- Modify: `build.gradle`
- Create: `src/test/java/ru/liko/tacz_mechanics/SmokeTest.java`

**Interfaces:**
- Produces: рабочая задача Gradle `test` на JUnit Platform.

- [ ] **Step 1: Добавить JUnit 5 в `build.gradle`**

В блок `dependencies { ... }` (рядом со строкой `compileOnly 'com.tacz.guns:tacz'`, ~строка 99) добавить:

```gradle
    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

В конец файла `build.gradle` добавить:

```gradle
test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Создать smoke-тест**

`src/test/java/ru/liko/tacz_mechanics/SmokeTest.java`:

```java
package ru.liko.tacz_mechanics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeTest {
    @Test
    void junitWorks() {
        assertEquals(4, 2 + 2);
    }
}
```

- [ ] **Step 3: Запустить тесты — убедиться, что проходят**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, `SmokeTest.junitWorks` PASSED.

- [ ] **Step 4: Commit**

```bash
git add build.gradle src/test/java/ru/liko/tacz_mechanics/SmokeTest.java
git commit -m "test: add JUnit 5 test infrastructure"
```

---

## Task 2: `SwaySpring` — пружинная физика одной оси

**Files:**
- Create: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/SwaySpring.java`
- Test: `src/test/java/ru/liko/tacz_mechanics/freeaim/SwaySpringTest.java`

**Interfaces:**
- Produces:
  - `new SwaySpring()` — создаёт пружину в нуле.
  - `void setParams(float stiffness, float damping, float maxAngle)`
  - `void addImpulse(float impulse)` — добавляет к скорости.
  - `void update(float dt)` — шаг интегрирования (semi-implicit Euler), сохраняет prev для интерполяции.
  - `float getValue()` — текущая позиция (градусы).
  - `float getInterpolated(float pt)` — lerp(prev, current).
  - `void reset()` — обнуляет позицию, скорость, prev.

- [ ] **Step 1: Написать падающий тест**

`src/test/java/ru/liko/tacz_mechanics/freeaim/SwaySpringTest.java`:

```java
package ru.liko.tacz_mechanics.freeaim;

import org.junit.jupiter.api.Test;
import ru.liko.tacz_mechanics.client.freeaim.SwaySpring;
import static org.junit.jupiter.api.Assertions.*;

class SwaySpringTest {

    @Test
    void startsAtZero() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 5f);
        assertEquals(0f, s.getValue(), 1e-6);
    }

    @Test
    void impulseDecaysBackToZero() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 10f);
        s.addImpulse(3f);
        for (int i = 0; i < 500; i++) {
            s.update(1f);
        }
        assertEquals(0f, s.getValue(), 0.01f);
    }

    @Test
    void doesNotDivergeWithSaneParams() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.3f, 0.6f, 10f);
        s.addImpulse(5f);
        for (int i = 0; i < 1000; i++) {
            s.update(1f);
            assertTrue(Math.abs(s.getValue()) <= 10f + 1e-3,
                    "position must stay within clamp, was " + s.getValue());
        }
    }

    @Test
    void clampsToMaxAngle() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.0f, 0.0f, 2f); // нет возврата, нет демпфирования
        s.addImpulse(100f);
        s.update(1f);
        assertEquals(2f, s.getValue(), 1e-6);
        s.addImpulse(-100f);
        s.update(1f);
        assertEquals(-2f, s.getValue(), 1e-6);
    }

    @Test
    void interpolationBlendsPrevAndCurrent() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.0f, 0.0f, 100f);
        s.addImpulse(10f);
        s.update(1f); // prev=0, current=10
        assertEquals(0f, s.getInterpolated(0f), 1e-6);
        assertEquals(10f, s.getInterpolated(1f), 1e-6);
        assertEquals(5f, s.getInterpolated(0.5f), 1e-6);
    }

    @Test
    void resetZeroesEverything() {
        SwaySpring s = new SwaySpring();
        s.setParams(0.2f, 0.5f, 10f);
        s.addImpulse(5f);
        s.update(1f);
        s.reset();
        assertEquals(0f, s.getValue(), 1e-6);
        assertEquals(0f, s.getInterpolated(1f), 1e-6);
    }
}
```

- [ ] **Step 2: Запустить тест — убедиться, что падает**

Run: `./gradlew test --tests "ru.liko.tacz_mechanics.freeaim.SwaySpringTest"`
Expected: FAIL компиляции — `SwaySpring` не существует.

- [ ] **Step 3: Реализовать `SwaySpring`**

`src/main/java/ru/liko/tacz_mechanics/client/freeaim/SwaySpring.java`:

```java
package ru.liko.tacz_mechanics.client.freeaim;

/**
 * Spring-damper physics for one rotation axis (degrees).
 * Semi-implicit Euler integration for stability. No Minecraft dependencies (unit-testable).
 */
public final class SwaySpring {

    private float position;
    private float velocity;
    private float prevPosition;

    private float stiffness = 0.2f;
    private float damping = 0.5f;
    private float maxAngle = 5f;

    public void setParams(float stiffness, float damping, float maxAngle) {
        this.stiffness = stiffness;
        this.damping = damping;
        this.maxAngle = maxAngle;
    }

    public void addImpulse(float impulse) {
        velocity += impulse;
    }

    public void update(float dt) {
        prevPosition = position;
        float accel = -stiffness * position - damping * velocity;
        velocity += accel * dt;
        position += velocity * dt;
        if (position > maxAngle) {
            position = maxAngle;
        } else if (position < -maxAngle) {
            position = -maxAngle;
        }
    }

    public float getValue() {
        return position;
    }

    public float getInterpolated(float pt) {
        return prevPosition + (position - prevPosition) * pt;
    }

    public void reset() {
        position = 0f;
        velocity = 0f;
        prevPosition = 0f;
    }
}
```

- [ ] **Step 4: Запустить тест — убедиться, что проходит**

Run: `./gradlew test --tests "ru.liko.tacz_mechanics.freeaim.SwaySpringTest"`
Expected: PASS — все 6 тестов зелёные.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/client/freeaim/SwaySpring.java src/test/java/ru/liko/tacz_mechanics/freeaim/SwaySpringTest.java
git commit -m "feat: add SwaySpring spring-damper physics"
```

---

## Task 3: Конфиг пружины + рефактор `FreeAimHandler` на `SwaySpring` (только look-источник)

Сохраняет текущее поведение «оружие отстаёт от поворота камеры», но через пружину. ADS пока не трогаем (оставляем приглушение как было визуально), точное совпадение — в Task 7.

**Files:**
- Modify: `src/main/java/ru/liko/tacz_mechanics/Config.java` (секция `FreeAim`, ~строки 344-385)
- Modify: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimHandler.java`

**Interfaces:**
- Consumes: `SwaySpring` (Task 2).
- Produces (публичный API `FreeAimHandler`, используется миксинами и Task 4/5/6/7):
  - `static FreeAimHandler getInstance()`
  - `void tick()`
  - `float getEffectivePitch(float pt)` — градусы, ADS-множитель уже применён.
  - `float getEffectiveYaw(float pt)`
  - `float getCrosshairX(float pt)`
  - `float getCrosshairY(float pt)`
  - `boolean isActive()` — enabled && держит оружие (НЕ зависит от ADS).
  - `void addRecoilImpulse(float pitchImpulse, float yawImpulse)` — внешний импульс отдачи (Task 4).
  - `void reset()`

- [ ] **Step 1: Обновить конфиг `FreeAim` в `Config.java`**

Заменить блок `public static final class FreeAim { ... }` (целиком) на:

```java
    public static final class FreeAim {
        private static final ModConfigSpec.BooleanValue ENABLED = SERVER_BUILDER
                .comment("Enable free aim - gun sways with spring physics behind camera/movement/recoil")
                .define("freeAim.enabled", true);
        private static final ModConfigSpec.DoubleValue MAX_ANGLE = SERVER_BUILDER
                .comment("Maximum angle (degrees) the gun barrel can deviate from view direction")
                .defineInRange("freeAim.maxAngle", 4.0, 0.5, 25.0);
        private static final ModConfigSpec.DoubleValue STIFFNESS = SERVER_BUILDER
                .comment("Spring stiffness - how strongly the gun is pulled back to center (higher = snappier)")
                .defineInRange("freeAim.spring.stiffness", 0.25, 0.01, 1.0);
        private static final ModConfigSpec.DoubleValue DAMPING = SERVER_BUILDER
                .comment("Spring damping - resistance to motion (higher = less overshoot/wobble)")
                .defineInRange("freeAim.spring.damping", 0.55, 0.05, 2.0);
        private static final ModConfigSpec.DoubleValue LOOK_SENSITIVITY = SERVER_BUILDER
                .comment("How strongly camera rotation pushes the gun (impulse per degree turned)")
                .defineInRange("freeAim.look.sensitivity", 0.6, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue ADS_MULTIPLIER = SERVER_BUILDER
                .comment("Sway multiplier while aiming down sights (0 = no sway in ADS, 1 = full)")
                .defineInRange("freeAim.adsMultiplier", 0.35, 0.0, 1.0);
        private static final ModConfigSpec.DoubleValue CROSSHAIR_SCALE = SERVER_BUILDER
                .comment("Scale factor for converting free aim angle to crosshair screen pixels")
                .defineInRange("freeAim.crosshairScale", 10.0, 1.0, 50.0);
        private static final ModConfigSpec.BooleanValue DISABLE_CROSSHAIR_MOVEMENT = SERVER_BUILDER
                .comment("Disable crosshair movement with free aim (crosshair stays centered)")
                .define("freeAim.disableCrosshairMovement", false);

        public static boolean enabled;
        public static double maxAngle;
        public static double stiffness;
        public static double damping;
        public static double lookSensitivity;
        public static double adsMultiplier;
        public static double crosshairScale;
        public static boolean disableCrosshairMovement;

        private static void load() {
            enabled = ENABLED.get();
            maxAngle = MAX_ANGLE.get();
            stiffness = STIFFNESS.get();
            damping = DAMPING.get();
            lookSensitivity = LOOK_SENSITIVITY.get();
            adsMultiplier = ADS_MULTIPLIER.get();
            crosshairScale = CROSSHAIR_SCALE.get();
            disableCrosshairMovement = DISABLE_CROSSHAIR_MOVEMENT.get();
        }

        static void init() {
        }

        private FreeAim() {
        }
    }
```

(Удалены: `LERP_SPEED`/`lerpSpeed`, `DISABLE_WHEN_AIMING`/`disableWhenAiming`. Параметры `movement.*`, `recoil.*`, `thirdPerson.enabled` добавятся в Task 5/4/8.)

- [ ] **Step 2: Переписать `FreeAimHandler.java`**

Полностью заменить содержимое на:

```java
package ru.liko.tacz_mechanics.client.freeaim;

import com.mojang.logging.LogUtils;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.network.FreeAimSyncPacket;

/**
 * Free Aim orchestrator: spring-driven weapon sway.
 * Holds two SwaySpring axes (pitch/yaw), feeds them impulses from sources
 * (look delta now; movement/recoil added later) and exposes the effective
 * (ADS-scaled) offset to all consumers.
 */
public class FreeAimHandler {

    private static final FreeAimHandler INSTANCE = new FreeAimHandler();

    private final SwaySpring pitchSpring = new SwaySpring();
    private final SwaySpring yawSpring = new SwaySpring();

    // Previous player rotation (for look-delta source)
    private float lastPitch = Float.NaN;
    private float lastYaw = Float.NaN;

    // Pending recoil impulse (added by RecoilSource between ticks)
    private float pendingRecoilPitch = 0f;
    private float pendingRecoilYaw = 0f;

    private int syncTimer = 0;

    public static FreeAimHandler getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (!Config.FreeAim.enabled || player == null || mc.isPaused() || !isHoldingGun(player)) {
            reset();
            return;
        }

        // Sync spring params from config every tick (cheap, allows live reload)
        float stiffness = (float) Config.FreeAim.stiffness;
        float damping = (float) Config.FreeAim.damping;
        float max = (float) Config.FreeAim.maxAngle;
        pitchSpring.setParams(stiffness, damping, max);
        yawSpring.setParams(stiffness, damping, max);

        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();

        if (Float.isNaN(lastPitch)) {
            lastPitch = currentPitch;
            lastYaw = currentYaw;
        }

        // === Source: look delta ===
        float deltaPitch = currentPitch - lastPitch;
        float deltaYaw = currentYaw - lastYaw;
        while (deltaYaw > 180) deltaYaw -= 360;
        while (deltaYaw < -180) deltaYaw += 360;
        lastPitch = currentPitch;
        lastYaw = currentYaw;

        float lookSens = (float) Config.FreeAim.lookSensitivity;
        // Gun lags behind: impulse opposite to camera movement
        pitchSpring.addImpulse(-deltaPitch * lookSens);
        yawSpring.addImpulse(-deltaYaw * lookSens);

        // === Source: recoil (queued by RecoilSource) ===
        if (pendingRecoilPitch != 0f || pendingRecoilYaw != 0f) {
            pitchSpring.addImpulse(pendingRecoilPitch);
            yawSpring.addImpulse(pendingRecoilYaw);
            pendingRecoilPitch = 0f;
            pendingRecoilYaw = 0f;
        }

        // === Integrate (dt = 1 tick) ===
        pitchSpring.update(1f);
        yawSpring.update(1f);

        // === Sync effective offset (ADS already applied) to server every 2 ticks ===
        if (++syncTimer >= 2) {
            syncTimer = 0;
            float effPitch = getEffectivePitch(1f);
            float effYaw = getEffectiveYaw(1f);
            if (effPitch != 0f || effYaw != 0f) {
                try {
                    PacketDistributor.sendToServer(new FreeAimSyncPacket(effPitch, effYaw));
                } catch (Exception e) {
                    LogUtils.getLogger().warn("Failed to send FreeAim sync packet", e);
                }
            }
        }
    }

    public void addRecoilImpulse(float pitchImpulse, float yawImpulse) {
        pendingRecoilPitch += pitchImpulse;
        pendingRecoilYaw += yawImpulse;
    }

    private boolean isHoldingGun(LocalPlayer player) {
        try {
            return IGun.mainHandHoldGun(player);
        } catch (Exception e) {
            return false;
        }
    }

    private float aimingProgress(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0f;
        try {
            return IClientPlayerGunOperator.fromLocalPlayer(mc.player).getClientAimingProgress(pt);
        } catch (Exception e) {
            return 0f;
        }
    }

    /** ADS scale: lerp(1.0, adsMultiplier, aimingProgress). */
    private float adsFactor(float pt) {
        float ads = (float) Config.FreeAim.adsMultiplier;
        float p = aimingProgress(pt);
        return 1f + (ads - 1f) * p;
    }

    public float getEffectivePitch(float pt) {
        return pitchSpring.getInterpolated(pt) * adsFactor(pt);
    }

    public float getEffectiveYaw(float pt) {
        return yawSpring.getInterpolated(pt) * adsFactor(pt);
    }

    public float getCrosshairX(float pt) {
        if (!isActive() || Config.FreeAim.disableCrosshairMovement) return 0f;
        return -getEffectiveYaw(pt) * (float) Config.FreeAim.crosshairScale;
    }

    public float getCrosshairY(float pt) {
        if (!isActive() || Config.FreeAim.disableCrosshairMovement) return 0f;
        return -getEffectivePitch(pt) * (float) Config.FreeAim.crosshairScale;
    }

    public boolean isActive() {
        if (!Config.FreeAim.enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        return player != null && isHoldingGun(player);
    }

    public void reset() {
        pitchSpring.reset();
        yawSpring.reset();
        lastPitch = Float.NaN;
        lastYaw = Float.NaN;
        pendingRecoilPitch = 0f;
        pendingRecoilYaw = 0f;
    }
}
```

- [ ] **Step 3: Обновить вызовы геттеров в `FreeAimGunModelMixin.java`**

В `FreeAimGunModelMixin.java` заменить тело инъекции HEAD (метод `taczMechanics$pushAndRotate`) — использовать новый единый угол вместо `getInterpolatedPitch/Yaw` и убрать собственный `aimFactor`/`pitchSens`/`yawSens` (ADS уже в effective; угол = реальные градусы):

Заменить строки внутри `try { ... }` (от `FreeAimHandler handler = ...` до конца translate-блока) на:

```java
            FreeAimHandler handler = FreeAimHandler.getInstance();
            float pitchOffset = handler.getEffectivePitch(partialTick);
            float yawOffset = handler.getEffectiveYaw(partialTick);
            if (Math.abs(pitchOffset) < 0.001f && Math.abs(yawOffset) < 0.001f) {
                return;
            }

            float pivotZ = 0.3f;
            float pivotY = -0.1f;

            poseStack.pushPose();
            taczMechanics$pushed = true;
            poseStack.translate(0, pivotY, pivotZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitchOffset));
            poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));
            poseStack.translate(0, -pivotY, -pivotZ);
```

Удалить из этого файла ставший ненужным импорт `com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator` (если IDE/компилятор не ругается на неиспользуемый — допустимо оставить, но предпочтительно удалить).

- [ ] **Step 4: `FreeAimCrosshairMixin.java` — проверить совместимость**

Метод уже вызывает `handler.getCrosshairX(partialTicks)` / `getCrosshairY(...)` — сигнатуры сохранены, правок не требуется. Убедиться, что обращений к удалённым методам нет (`getInterpolatedPitch`, `getPitchOffset`, `hasOffset`, `getInterpolatedYaw`):

Run: `grep -rn "getInterpolatedPitch\|getInterpolatedYaw\|getPitchOffset\|getYawOffset\|hasOffset\|lerpSpeed\|disableWhenAiming" src/main/java`
Expected: пусто (никаких ссылок на удалённые члены).

- [ ] **Step 5: Собрать — убедиться, что компилируется**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Ин-гейм проверка**

Run: `./gradlew runClient` (или через IDE). Взять любое TaCZ-оружие, резко крутить камерой.
Expected: модель оружия плавно «заносит» в сторону, противоположную повороту, и плавно догоняет центр (пружинное затухание, без рывков/расхождения). Прицел смещается синхронно.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/Config.java \
        src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimHandler.java \
        src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimGunModelMixin.java
git commit -m "feat: rebuild FreeAimHandler on spring physics with look source"
```

---

## Task 4: `RecoilSource` — импульс отдачи через `GunFireEvent`

**Files:**
- Create: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/RecoilSource.java`
- Modify: `src/main/java/ru/liko/tacz_mechanics/Config.java` (добавить `recoil.*`)

**Interfaces:**
- Consumes: `FreeAimHandler.addRecoilImpulse(float, float)` (Task 3); `com.tacz.guns.api.event.common.GunFireEvent`.
- Produces: побочный эффект — импульс отдачи в springs при выстреле локального игрока.

- [ ] **Step 1: Добавить конфиг `recoil.*` в `Config.FreeAim`**

В `Config.java`, в класс `FreeAim`, добавить поля (рядом с остальными `private static final`):

```java
        private static final ModConfigSpec.BooleanValue RECOIL_ENABLED = SERVER_BUILDER
                .comment("Add an upward sway kick to the gun model on each shot")
                .define("freeAim.recoil.enabled", true);
        private static final ModConfigSpec.DoubleValue RECOIL_SCALE = SERVER_BUILDER
                .comment("Strength of the recoil sway impulse (degrees of velocity per shot)")
                .defineInRange("freeAim.recoil.scale", 0.8, 0.0, 10.0);
```

Добавить public-поля:

```java
        public static boolean recoilEnabled;
        public static double recoilScale;
```

В `load()` добавить:

```java
            recoilEnabled = RECOIL_ENABLED.get();
            recoilScale = RECOIL_SCALE.get();
```

- [ ] **Step 2: Создать `RecoilSource.java`**

`src/main/java/ru/liko/tacz_mechanics/client/freeaim/RecoilSource.java`:

```java
package ru.liko.tacz_mechanics.client.freeaim;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;

/**
 * Feeds a recoil impulse into the sway springs when the local player fires.
 * TaCZ already applies recoil to the camera; this adds a visual gun-model kick.
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT)
public final class RecoilSource {

    private RecoilSource() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!Config.FreeAim.enabled || !Config.FreeAim.recoilEnabled) {
            return;
        }
        if (event.getLogicalSide() != LogicalSide.CLIENT) {
            return;
        }
        if (Minecraft.getInstance().player != event.getShooter()) {
            return;
        }
        float scale = (float) Config.FreeAim.recoilScale;
        // Pitch up (negative camera pitch is up) -> push barrel up via positive impulse,
        // small alternating horizontal kick using game time parity for variety.
        float yawKick = (Minecraft.getInstance().player.tickCount % 2 == 0) ? 0.25f : -0.25f;
        FreeAimHandler.getInstance().addRecoilImpulse(scale, yawKick * scale);
    }
}
```

- [ ] **Step 3: Собрать**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Ин-гейм проверка**

Run: `./gradlew runClient`. Стрелять очередью.
Expected: при каждом выстреле модель оружия слегка подбрасывает вверх и качает по горизонтали, затем пружина возвращает её. Параметр `freeAim.recoil.scale` влияет на силу.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/client/freeaim/RecoilSource.java \
        src/main/java/ru/liko/tacz_mechanics/Config.java
git commit -m "feat: add recoil sway impulse on GunFireEvent"
```

---

## Task 5: `MovementSource` — качание от ходьбы/бега/прыжков

**Files:**
- Create: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/MovementSource.java`
- Modify: `src/main/java/ru/liko/tacz_mechanics/Config.java` (добавить `movement.*`)
- Modify: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimHandler.java` (вызвать источник в `tick()`)

**Interfaces:**
- Consumes: `LocalPlayer`, `SwaySpring.addImpulse` через переданные springs.
- Produces:
  - `MovementSource` (stateful, по экземпляру на handler).
  - `void apply(LocalPlayer player, SwaySpring pitchSpring, SwaySpring yawSpring)` — добавляет импульсы движения за тик.

- [ ] **Step 1: Добавить конфиг `movement.*` в `Config.FreeAim`**

В `Config.java`, класс `FreeAim`, добавить:

```java
        private static final ModConfigSpec.BooleanValue MOVEMENT_ENABLED = SERVER_BUILDER
                .comment("Add gun sway from walking/sprinting/jumping")
                .define("freeAim.movement.enabled", true);
        private static final ModConfigSpec.DoubleValue MOVEMENT_WALK_SCALE = SERVER_BUILDER
                .comment("Sway amplitude while walking")
                .defineInRange("freeAim.movement.walkScale", 0.15, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue MOVEMENT_SPRINT_SCALE = SERVER_BUILDER
                .comment("Sway amplitude while sprinting")
                .defineInRange("freeAim.movement.sprintScale", 0.35, 0.0, 5.0);
        private static final ModConfigSpec.DoubleValue MOVEMENT_JUMP_SCALE = SERVER_BUILDER
                .comment("Sway impulse on jump/land")
                .defineInRange("freeAim.movement.jumpScale", 1.2, 0.0, 10.0);
```

Public-поля:

```java
        public static boolean movementEnabled;
        public static double movementWalkScale;
        public static double movementSprintScale;
        public static double movementJumpScale;
```

В `load()`:

```java
            movementEnabled = MOVEMENT_ENABLED.get();
            movementWalkScale = MOVEMENT_WALK_SCALE.get();
            movementSprintScale = MOVEMENT_SPRINT_SCALE.get();
            movementJumpScale = MOVEMENT_JUMP_SCALE.get();
```

- [ ] **Step 2: Создать `MovementSource.java`**

`src/main/java/ru/liko/tacz_mechanics/client/freeaim/MovementSource.java`:

```java
package ru.liko.tacz_mechanics.client.freeaim;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ru.liko.tacz_mechanics.Config;

/**
 * Produces walk/sprint bob and jump/land kicks as spring impulses.
 * Stateful: tracks bob phase and previous ground state.
 */
public final class MovementSource {

    private float bobPhase = 0f;
    private boolean wasOnGround = true;

    public void apply(LocalPlayer player, SwaySpring pitchSpring, SwaySpring yawSpring) {
        if (!Config.FreeAim.movementEnabled) {
            return;
        }

        // Horizontal speed (blocks/tick) from this tick's movement
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);

        // Walk/sprint bob: advance phase by speed, emit a gentle figure-eight sway
        if (speed > 0.005f && player.onGround()) {
            float amp = player.isSprinting()
                    ? (float) Config.FreeAim.movementSprintScale
                    : (float) Config.FreeAim.movementWalkScale;
            bobPhase += speed * 8f;
            // Vertical bob twice per horizontal cycle, horizontal once
            pitchSpring.addImpulse(Mth.sin(bobPhase * 2f) * amp * 0.05f);
            yawSpring.addImpulse(Mth.cos(bobPhase) * amp * 0.05f);
        }

        // Jump / land kick
        boolean onGround = player.onGround();
        if (wasOnGround && !onGround) {
            // Just left ground (jumped): barrel dips
            pitchSpring.addImpulse(-(float) Config.FreeAim.movementJumpScale);
        } else if (!wasOnGround && onGround) {
            // Just landed: barrel kicks up
            pitchSpring.addImpulse((float) Config.FreeAim.movementJumpScale);
        }
        wasOnGround = onGround;
    }

    public void reset() {
        bobPhase = 0f;
        wasOnGround = true;
    }
}
```

- [ ] **Step 3: Подключить `MovementSource` в `FreeAimHandler`**

В `FreeAimHandler.java`:

Добавить поле рядом со springs:

```java
    private final MovementSource movementSource = new MovementSource();
```

В `tick()`, после блока look-источника и перед блоком recoil (после строк `lastYaw = currentYaw;` … `yawSpring.addImpulse(-deltaYaw * lookSens);`), добавить:

```java
        // === Source: movement ===
        movementSource.apply(player, pitchSpring, yawSpring);
```

В методе `reset()` добавить (в конец):

```java
        movementSource.reset();
```

- [ ] **Step 4: Собрать**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Ин-гейм проверка**

Run: `./gradlew runClient`. Ходить, бегать (спринт), прыгать с оружием в руках.
Expected: при ходьбе лёгкое ритмичное качание, при спринте сильнее, при прыжке ствол ныряет вниз, при приземлении подбрасывает вверх. Стоя на месте — без качания от движения.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/client/freeaim/MovementSource.java \
        src/main/java/ru/liko/tacz_mechanics/Config.java \
        src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimHandler.java
git commit -m "feat: add walk/sprint/jump movement sway source"
```

---

## Task 6: Точное совпадение выстрела со стволом (сервер)

ADS-множитель и единый угол уже введены (Task 3). Теперь синхронизируем серверный расчёт выстрела с тем же углом (клиент шлёт уже-эффективный offset), убираем рассинхронные множители `0.5/0.4`.

**Files:**
- Modify: `src/main/java/ru/liko/tacz_mechanics/server/FreeAimServerHandler.java`

**Interfaces:**
- Consumes: эффективный offset из `FreeAimSyncPacket` (Task 3 уже шлёт `getEffectivePitch/Yaw`).
- Produces: `getAdjustedPitch/Yaw` применяют offset 1:1 (1 градус offset = 1 градус отклонения выстрела).

- [ ] **Step 1: Убрать множители чувствительности**

В `FreeAimServerHandler.java` удалить:

```java
    // Sensitivity multipliers - must match FreeAimGunModelMixin for visual accuracy
    private static final float PITCH_SENSITIVITY = 0.5f;
    private static final float YAW_SENSITIVITY = 0.4f;
```

Заменить методы `getAdjustedPitch` / `getAdjustedYaw` на 1:1 применение:

```java
    /**
     * Adjusted pitch = base pitch minus free-aim offset (offset is the real barrel deviation).
     */
    public static float getAdjustedPitch(ServerPlayer player, float basePitch) {
        return basePitch - getPitchOffset(player);
    }

    /**
     * Adjusted yaw = base yaw plus free-aim offset.
     */
    public static float getAdjustedYaw(ServerPlayer player, float baseYaw) {
        return baseYaw + getYawOffset(player);
    }
```

(Знаки `-pitch` / `+yaw` соответствуют наклону модели в `FreeAimGunModelMixin`: `Axis.XP.rotationDegrees(-pitchOffset)`, `Axis.YP.rotationDegrees(yawOffset)`. Если ин-гейм калибровка покажет инверсию — синхронно поменять знак здесь и в миксине.)

- [ ] **Step 2: Собрать**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ин-гейм калибровка совпадения**

Run: `./gradlew runClient`. Включить `F3+B` (хитбоксы) для ориентира, либо стрелять по близкой стене/блокам.
Проверка (как в цикле тестирования проекта — Prism/F3+B/логи):
1. Резко повернуть камеру, чтобы оружие «занесло» в сторону, и выстрелить, удерживая занос (стрелять во время движения).
2. Пуля должна уходить туда, куда визуально смотрит ствол / где стоит смещённый прицел — НЕ в центр экрана.
Expected: точка попадания совпадает с положением смещённого прицела и направлением ствола. Если расходится — подобрать `pivotY/pivotZ` в `FreeAimGunModelMixin` (визуальная привязка) либо знак offset; угловое соответствие 1:1 при этом сохраняется.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/server/FreeAimServerHandler.java
git commit -m "feat: exact bullet/barrel alignment (unified 1:1 offset angle)"
```

---

## Task 7: `FreeAimBroadcastPacket` + клиентский кэш offset других игроков

**Files:**
- Create: `src/main/java/ru/liko/tacz_mechanics/network/FreeAimBroadcastPacket.java`
- Create: `src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimClientCache.java`
- Modify: `src/main/java/ru/liko/tacz_mechanics/network/FreeAimSyncPacket.java` (рассылка наблюдателям)
- Modify: `src/main/java/ru/liko/tacz_mechanics/network/ModNetworking.java` (регистрация + обработчик)
- Modify: `src/main/java/ru/liko/tacz_mechanics/Config.java` (`thirdPerson.enabled`)

**Interfaces:**
- Consumes: `FreeAimServerHandler.updatePlayerFreeAim` (уже вызывается в `FreeAimSyncPacket.handle`).
- Produces:
  - `FreeAimBroadcastPacket(java.util.UUID playerId, float pitch, float yaw)` с `TYPE`, `STREAM_CODEC`, `static void handle(...)`.
  - `FreeAimClientCache.update(UUID, float pitch, float yaw)`, `FreeAimClientCache.getPitch(UUID)`, `getYaw(UUID)`, `remove(UUID)`.

- [ ] **Step 1: Добавить конфиг `thirdPerson.enabled`**

В `Config.java`, класс `FreeAim`, добавить:

```java
        private static final ModConfigSpec.BooleanValue THIRD_PERSON_ENABLED = SERVER_BUILDER
                .comment("Show free-aim gun sway on other players / third-person model")
                .define("freeAim.thirdPerson.enabled", true);
```

Public-поле:

```java
        public static boolean thirdPersonEnabled;
```

В `load()`:

```java
            thirdPersonEnabled = THIRD_PERSON_ENABLED.get();
```

- [ ] **Step 2: Создать `FreeAimClientCache.java`**

`src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimClientCache.java`:

```java
package ru.liko.tacz_mechanics.client.freeaim;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of other players' free-aim offsets (for third-person rendering).
 */
public final class FreeAimClientCache {

    private record Entry(float pitch, float yaw, long timestamp) {}

    private static final Map<UUID, Entry> OFFSETS = new ConcurrentHashMap<>();
    private static final long TTL_MS = 1000L;

    private FreeAimClientCache() {
    }

    public static void update(UUID id, float pitch, float yaw) {
        OFFSETS.put(id, new Entry(pitch, yaw, System.currentTimeMillis()));
    }

    public static float getPitch(UUID id) {
        Entry e = OFFSETS.get(id);
        if (e == null || System.currentTimeMillis() - e.timestamp() > TTL_MS) {
            return 0f;
        }
        return e.pitch();
    }

    public static float getYaw(UUID id) {
        Entry e = OFFSETS.get(id);
        if (e == null || System.currentTimeMillis() - e.timestamp() > TTL_MS) {
            return 0f;
        }
        return e.yaw();
    }

    public static void remove(UUID id) {
        OFFSETS.remove(id);
    }
}
```

- [ ] **Step 3: Создать `FreeAimBroadcastPacket.java`**

`src/main/java/ru/liko/tacz_mechanics/network/FreeAimBroadcastPacket.java`:

```java
package ru.liko.tacz_mechanics.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.client.freeaim.FreeAimClientCache;

import java.util.UUID;

/**
 * Server -> tracking clients: broadcasts a player's effective free-aim offset
 * so observers can render third-person gun sway.
 */
public record FreeAimBroadcastPacket(UUID playerId, float pitch, float yaw) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FreeAimBroadcastPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TaczMechanics.MODID, "free_aim_broadcast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FreeAimBroadcastPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FreeAimBroadcastPacket::playerId,
            ByteBufCodecs.FLOAT, FreeAimBroadcastPacket::pitch,
            ByteBufCodecs.FLOAT, FreeAimBroadcastPacket::yaw,
            FreeAimBroadcastPacket::new);

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FreeAimBroadcastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FreeAimClientCache.update(packet.playerId(), packet.pitch(), packet.yaw()));
    }
}
```

- [ ] **Step 4: Рассылать наблюдателям в `FreeAimSyncPacket.handle`**

В `FreeAimSyncPacket.java`, метод `handle`, после `FreeAimServerHandler.updatePlayerFreeAim(...)` добавить рассылку (нужны импорты `net.neoforged.neoforge.network.PacketDistributor` и `ru.liko.tacz_mechanics.Config`):

```java
    public static void handle(FreeAimSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                FreeAimServerHandler.updatePlayerFreeAim(serverPlayer, packet.pitchOffset(), packet.yawOffset());
                if (Config.FreeAim.thirdPersonEnabled) {
                    PacketDistributor.sendToPlayersTrackingEntity(serverPlayer,
                            new FreeAimBroadcastPacket(serverPlayer.getUUID(), packet.pitchOffset(), packet.yawOffset()));
                }
            }
        });
    }
```

- [ ] **Step 5: Зарегистрировать broadcast-пакет в `ModNetworking`**

В `ModNetworking.register(...)`, после блока регистрации `FreeAimSyncPacket` (`playToServer`), добавить:

```java
        registrar.playToClient(
                FreeAimBroadcastPacket.TYPE,
                FreeAimBroadcastPacket.STREAM_CODEC,
                FreeAimBroadcastPacket::handle);
```

- [ ] **Step 6: Собрать**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/network/FreeAimBroadcastPacket.java \
        src/main/java/ru/liko/tacz_mechanics/client/freeaim/FreeAimClientCache.java \
        src/main/java/ru/liko/tacz_mechanics/network/FreeAimSyncPacket.java \
        src/main/java/ru/liko/tacz_mechanics/network/ModNetworking.java \
        src/main/java/ru/liko/tacz_mechanics/Config.java
git commit -m "feat: broadcast free-aim offset to tracking clients for third-person"
```

---

## Task 8: Наклон модели 3-го лица (`FreeAimThirdPersonMixin`)

**Files:**
- Create: `src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimThirdPersonMixin.java`
- Modify: `src/main/resources/tacz_mechanics.mixins.json` (добавить миксин в `client`)

**Interfaces:**
- Consumes: `FreeAimClientCache.getPitch/getYaw(UUID)` (Task 7); TaCZ `InnerThirdPersonManager.setRotationAnglesHead(LivingEntity, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head, float limbSwingAmount)`.
- Produces: визуальный наклон руки/корпуса наблюдаемого игрока на его free-aim offset.

- [ ] **Step 1: Создать миксин**

`src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimThirdPersonMixin.java`:

```java
package ru.liko.tacz_mechanics.mixin;

import com.tacz.guns.client.animation.third.InnerThirdPersonManager;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.client.freeaim.FreeAimClientCache;

/**
 * Applies free-aim sway to the third-person gun pose of any player,
 * using their broadcast offset from FreeAimClientCache.
 */
@Mixin(value = InnerThirdPersonManager.class, remap = false)
public class FreeAimThirdPersonMixin {

    @Inject(method = "setRotationAnglesHead", at = @At("TAIL"), require = 0)
    private static void taczMechanics$applyFreeAim(LivingEntity entityIn, ModelPart rightArm, ModelPart leftArm,
                                                   ModelPart body, ModelPart head, float limbSwingAmount,
                                                   CallbackInfo ci) {
        if (!Config.FreeAim.enabled || !Config.FreeAim.thirdPersonEnabled || entityIn == null) {
            return;
        }
        float pitch = FreeAimClientCache.getPitch(entityIn.getUUID());
        float yaw = FreeAimClientCache.getYaw(entityIn.getUUID());
        if (Math.abs(pitch) < 0.001f && Math.abs(yaw) < 0.001f) {
            return;
        }
        // Convert degrees to radians; tilt right arm (gun) and body slightly.
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);
        rightArm.xRot += -pitchRad;
        rightArm.yRot += yawRad;
        body.yRot += yawRad * 0.3f;
    }
}
```

- [ ] **Step 2: Зарегистрировать миксин**

В `src/main/resources/tacz_mechanics.mixins.json`, в массив `"client"`, добавить элемент `"FreeAimThirdPersonMixin"` (рядом с `"FreeAimGunModelMixin"`):

```json
    "FreeAimCrosshairMixin",
    "FreeAimGunModelMixin",
    "FreeAimThirdPersonMixin",
```

- [ ] **Step 3: Собрать**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Ин-гейм проверка (2 клиента)**

Run: запустить выделенный сервер или LAN, подключить второй клиент (или наблюдать за другим игроком от 3-го лица).
Проверка:
1. Игрок A резко крутит камерой/бежит/стреляет.
2. Игрок B (наблюдатель) видит, что оружие в руках A наклоняется/качается в ту же сторону.
Expected: наклон модели 3-го лица соответствует качанию у владельца (с задержкой синхронизации ≤2 тика, сглаживается TTL-кэшем). При `freeAim.thirdPerson.enabled=false` эффект 3-го лица отсутствует.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/ru/liko/tacz_mechanics/mixin/FreeAimThirdPersonMixin.java \
        src/main/resources/tacz_mechanics.mixins.json
git commit -m "feat: third-person free-aim sway via broadcast offset"
```

---

## Task 9: Финальная регрессия и очистка

**Files:**
- Verify only (правки по результатам).

- [ ] **Step 1: Полная сборка и тесты**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL, юнит-тесты `SwaySpringTest` зелёные.

- [ ] **Step 2: Проверка отсутствия ссылок на удалённый API**

Run: `grep -rn "lerpSpeed\|disableWhenAiming\|PITCH_SENSITIVITY\|YAW_SENSITIVITY\|getInterpolatedPitch\|getInterpolatedYaw" src/main/java`
Expected: пусто.

- [ ] **Step 3: Ин-гейм матрица регрессии**

Run: `./gradlew runClient`. Проверить по чеклисту:
- Поворот камеры → занос+догон (1-е лицо). ✔
- Ходьба/спринт/прыжок → качание. ✔
- Выстрел → подброс модели. ✔
- ADS → качание приглушено (не отключено), точка прицеливания живая. ✔
- Выстрел совпадает с положением ствола/прицела. ✔
- 3-е лицо у другого игрока → наклон оружия. ✔
- `freeAim.enabled=false` → всё поведение отключено, оружие статично. ✔

- [ ] **Step 4: Финальный commit (если были правки калибровки)**

```bash
git add -A
git commit -m "chore: free-aim rework final regression pass"
```

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** физика (Task 2), источники look/recoil/movement (Task 3/4/5), ADS-множитель (Task 3), точное совпадение выстрела (Task 6), мультиплеер-рассылка + 3-е лицо (Task 7/8), конфиг (распределён по Task 3/4/5/7), тесты (Task 1/2 + ин-гейм). Все разделы спеки покрыты.
- **Плейсхолдеры:** отсутствуют — каждый шаг содержит конкретный код/команды.
- **Согласованность типов:** `FreeAimHandler` API (`getEffectivePitch/Yaw`, `getCrosshairX/Y`, `isActive`, `addRecoilImpulse`, `reset`) определён в Task 3 и используется в Task 4/5/8 без расхождений; `FreeAimClientCache` (`update/getPitch/getYaw/remove`) и `FreeAimBroadcastPacket(UUID,float,float)` согласованы между Task 7 и Task 8.
