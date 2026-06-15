package ru.liko.tacz_mechanics.client;

import ru.liko.tacz_mechanics.Config;

/**
 * Эффективные флаги твиков прицела/хитмаркеров на клиенте.
 * На выделенном сервере значения приходят с сервера (см. {@link ru.liko.tacz_mechanics.network.TweaksSyncPayload}),
 * т.к. серверный конфиг не подставляется в клиентский при подключении.
 */
public final class ClientTweakSettings {
    private static volatile boolean hideGunCrosshair;
    private static volatile boolean hideHitMarkers;

    static {
        applyFromLocalConfig();
    }

    private ClientTweakSettings() {
    }

    public static boolean hideGunCrosshair() {
        return hideGunCrosshair;
    }

    public static boolean hideHitMarkers() {
        return hideHitMarkers;
    }

    public static void applyFromLocalConfig() {
        hideGunCrosshair = Config.Tweaks.hideGunCrosshair;
        hideHitMarkers = Config.Tweaks.hideHitMarkers;
    }

    public static void applyFromServer(boolean hideCrosshair, boolean hideMarkers) {
        hideGunCrosshair = hideCrosshair;
        hideHitMarkers = hideMarkers;
    }
}
