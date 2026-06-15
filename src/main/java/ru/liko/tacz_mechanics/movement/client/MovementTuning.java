package ru.liko.tacz_mechanics.movement.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.movement.MovementPosture;

/**
 * Живая подстройка геометрии поз прямо в игре (только при {@code movement.debug}).
 *
 * <p>Клавиши: K — следующий параметр, ] — увеличить, [ — уменьшить, P — вывести все
 * текущие значения в чат (чтобы скопировать и зашить как дефолты). Текущий параметр и
 * значение показываются на actionbar. Размерные параметры сразу применяются через
 * {@code refreshDimensions()}, оффсеты/рендер — на следующем кадре.</p>
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT)
public final class MovementTuning {

    private static final float STEP = 0.05f;
    private static int selected = 0;

    private MovementTuning() {
    }

    private enum Param {
        SIT_HEIGHT("sit.height", true) {
            float get() { return MovementPosture.SIT_HEIGHT; } void set(float v) { MovementPosture.SIT_HEIGHT = v; }
        },
        SIT_EYE_HEIGHT("sit.eyeHeight", true) {
            float get() { return MovementPosture.SIT_EYE_HEIGHT; } void set(float v) { MovementPosture.SIT_EYE_HEIGHT = v; }
        },
        SIT_Y_OFFSET("sit.yOffset", false) {
            float get() { return MovementPosture.SIT_Y_OFFSET; } void set(float v) { MovementPosture.SIT_Y_OFFSET = v; }
        },
        SIT_MODEL_Y_OFFSET("sit.modelYOffset", false) {
            float get() { return MovementPosture.SIT_MODEL_Y_OFFSET; } void set(float v) { MovementPosture.SIT_MODEL_Y_OFFSET = v; }
        },
        CRAWL_WIDTH("crawl.width", true) {
            float get() { return MovementPosture.CRAWL_WIDTH; } void set(float v) { MovementPosture.CRAWL_WIDTH = v; }
        },
        CRAWL_LENGTH("crawl.length", true) {
            float get() { return MovementPosture.CRAWL_LENGTH; } void set(float v) { MovementPosture.CRAWL_LENGTH = v; }
        },
        CRAWL_BOX_FORWARD("crawl.boxForward", true) {
            float get() { return MovementPosture.CRAWL_BOX_FORWARD; } void set(float v) { MovementPosture.CRAWL_BOX_FORWARD = v; }
        },
        CRAWL_HEIGHT("crawl.height", true) {
            float get() { return MovementPosture.CRAWL_HEIGHT; } void set(float v) { MovementPosture.CRAWL_HEIGHT = v; }
        },
        CRAWL_EYE_HEIGHT("crawl.eyeHeight", true) {
            float get() { return MovementPosture.CRAWL_EYE_HEIGHT; } void set(float v) { MovementPosture.CRAWL_EYE_HEIGHT = v; }
        },
        CRAWL_Y_OFFSET("crawl.yOffset", false) {
            float get() { return MovementPosture.CRAWL_Y_OFFSET; } void set(float v) { MovementPosture.CRAWL_Y_OFFSET = v; }
        },
        CRAWL_CAMERA_FORWARD("crawl.cameraForward", false) {
            float get() { return MovementPosture.CRAWL_CAMERA_FORWARD; } void set(float v) { MovementPosture.CRAWL_CAMERA_FORWARD = v; }
        },
        CRAWL_MODEL_FORWARD("crawl.modelForward", false) {
            float get() { return MovementPosture.CRAWL_MODEL_FORWARD; } void set(float v) { MovementPosture.CRAWL_MODEL_FORWARD = v; }
        },
        CRAWL_MODEL_Z("crawl.modelZ", false) {
            float get() { return MovementPosture.CRAWL_MODEL_Z; } void set(float v) { MovementPosture.CRAWL_MODEL_Z = v; }
        };

        final String label;
        final boolean affectsHitbox;

        Param(String label, boolean affectsHitbox) {
            this.label = label;
            this.affectsHitbox = affectsHitbox;
        }

        abstract float get();
        abstract void set(float v);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.Movement.debug) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Param[] params = Param.values();
        boolean hitboxChanged = false;

        while (MovementKeyBindings.TUNE_SELECT_KEY.consumeClick()) {
            selected = (selected + 1) % params.length;
            actionbar(mc, "▶ " + params[selected].label + " = " + fmt(params[selected].get()));
        }
        while (MovementKeyBindings.TUNE_INC_KEY.consumeClick()) {
            Param p = params[selected];
            p.set(round(p.get() + STEP));
            hitboxChanged |= p.affectsHitbox;
            actionbar(mc, p.label + " = " + fmt(p.get()));
        }
        while (MovementKeyBindings.TUNE_DEC_KEY.consumeClick()) {
            Param p = params[selected];
            p.set(round(p.get() - STEP));
            hitboxChanged |= p.affectsHitbox;
            actionbar(mc, p.label + " = " + fmt(p.get()));
        }
        while (MovementKeyBindings.TUNE_DUMP_KEY.consumeClick()) {
            dump(mc);
        }

        if (hitboxChanged) {
            mc.player.refreshDimensions();
        }
    }

    private static void dump(Minecraft mc) {
        if (mc.player == null) return;
        mc.player.displayClientMessage(Component.literal("§e=== TaCZ Mechanics: значения поз ==="), false);
        for (Param p : Param.values()) {
            mc.player.displayClientMessage(Component.literal("  " + p.label + " = " + fmt(p.get())), false);
        }
    }

    private static void actionbar(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), true);
        }
    }

    private static float round(float v) {
        return Math.round(v * 100.0f) / 100.0f;
    }

    private static String fmt(float v) {
        return String.format("%.2f", v);
    }
}
