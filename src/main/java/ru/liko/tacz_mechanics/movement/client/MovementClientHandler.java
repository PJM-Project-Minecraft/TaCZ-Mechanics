package ru.liko.tacz_mechanics.movement.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.movement.LeanCollision;
import ru.liko.tacz_mechanics.movement.MovementPosture;
import ru.liko.tacz_mechanics.movement.MovementStateManager;
import ru.liko.tacz_mechanics.movement.PlayerState;
import ru.liko.tacz_mechanics.movement.network.MovementNetworkHandler;

/**
 * Client-side handler for player leaning input and camera/model effects.
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT)
public class MovementClientHandler {

    private static PlayerState clientState = new PlayerState();
    private static int lastSentCode = 0;

    // Camera offset for leaning
    public static float cameraProbeOffset = 0;
    private static long lastSyncTime = System.currentTimeMillis();

    // Input lock flag for lean keys
    private static boolean probeKeyLock = false;

    public static PlayerState getClientState() {
        return clientState;
    }

    /**
     * Откат/принудительная синхронизация с сервером (невалидное состояние и т.п.).
     */
    public static void applySyncedStateFromServer(int code) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        clientState.readCode(code);
        lastSentCode = code;
        MovementStateManager.updateState(mc.player.getUUID(), code);
        mc.player.refreshDimensions();
    }

    public static PlayerState getStateForPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) {
            return clientState;
        }
        return MovementStateManager.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Config.Movement.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetState();
            return;
        }

        // Update client state offsets for smooth animation
        clientState.updateOffset();
        clampLeanForCollision(mc.player);

        // Push state to server + refresh dimensions only on change.
        flushStateChange(mc.player);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!Config.Movement.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        handleLeanInput(mc, event);
    }

    private static void handleLeanInput(Minecraft mc, InputEvent.Key event) {
        KeyMapping leftLean = MovementKeyBindings.LEAN_LEFT_KEY;
        KeyMapping rightLean = MovementKeyBindings.LEAN_RIGHT_KEY;

        if (!clientState.canProbe()) return;

        // Left lean
        if (leftLean.isDown() && !probeKeyLock) {
            probeKeyLock = true;
            if (clientState.getProbe() != -1) {
                clientState.leftProbe();
            } else {
                clientState.resetProbe();
            }
        }

        // Right lean
        if (rightLean.isDown() && !probeKeyLock) {
            probeKeyLock = true;
            if (clientState.getProbe() != 1) {
                clientState.rightProbe();
            } else {
                clientState.resetProbe();
            }
        }

        // Reset lean lock
        if (!leftLean.isDown() && !rightLean.isDown()) {
            probeKeyLock = false;
            if (!Config.Movement.leanAutoHold && clientState.getProbe() != 0) {
                clientState.resetProbe();
            }
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!Config.Movement.enabled) return;

        if (clientState.getProbe() != 0) {
            event.getInput().forwardImpulse *= 0.9f;
            event.getInput().leftImpulse *= 0.9f;
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (!Config.Movement.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Update camera probe offset here for smooth per-frame animation
        double amplifier = (System.currentTimeMillis() - lastSyncTime) * (60 / 1000d);
        lastSyncTime = System.currentTimeMillis();

        // Slower speed for more realistic leaning (0.05 instead of 0.1)
        double speed = 0.05;

        if (clientState.getProbe() == -1) {
            if (cameraProbeOffset > -1) {
                cameraProbeOffset -= speed * amplifier;
            }
            if (cameraProbeOffset < -1) {
                cameraProbeOffset = -1;
            }
        } else if (clientState.getProbe() == 1) {
            if (cameraProbeOffset < 1) {
                cameraProbeOffset += speed * amplifier;
            }
            if (cameraProbeOffset > 1) {
                cameraProbeOffset = 1;
            }
        } else {
            if (Math.abs(cameraProbeOffset) <= speed * amplifier) {
                cameraProbeOffset = 0;
            } else if (cameraProbeOffset < 0) {
                cameraProbeOffset += speed * amplifier;
            } else if (cameraProbeOffset > 0) {
                cameraProbeOffset -= speed * amplifier;
            }
        }

        float maxLeft = LeanCollision.maxLeanMagnitude(mc.player, -1f);
        float maxRight = LeanCollision.maxLeanMagnitude(mc.player, 1f);
        cameraProbeOffset = Mth.clamp(cameraProbeOffset, -maxLeft, maxRight);

        // Apply lean rotation
        float roll = (float) event.getRoll();
        event.setRoll(roll + 10 * cameraProbeOffset);
    }

    private static void flushStateChange(LocalPlayer player) {
        int code = clientState.writeCode();
        if (code == lastSentCode) return;
        MovementStateManager.updateState(player.getUUID(), code);
        player.refreshDimensions();
        MovementPosture.logHitbox("CLIENT", player, clientState);
        MovementNetworkHandler.sendStateToServer(code);
        lastSentCode = code;
    }

    private static void clampLeanForCollision(LocalPlayer player) {
        if (player == null) {
            return;
        }
        float maxLeft = LeanCollision.maxLeanMagnitude(player, -1f);
        float maxRight = LeanCollision.maxLeanMagnitude(player, 1f);
        clientState.clampProbeOffset(maxLeft, maxRight);
        cameraProbeOffset = Mth.clamp(cameraProbeOffset, -maxLeft, maxRight);
    }

    private static void resetState() {
        clientState.reset();
        cameraProbeOffset = 0;
        lastSentCode = 0;
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!Config.Movement.enabled) return;

        Player player = event.getEntity();
        PlayerState state = getStateForPlayer(player);
        if (state == null) return;

        PoseStack poseStack = event.getPoseStack();
        // Lerp with partial tick for smooth render between tick-driven updates
        float probeOffset = Mth.lerp(event.getPartialTick(), state.getProbeOffsetOld(), state.getProbeOffset());

        // Apply leaning rotation
        if (probeOffset != 0) {
            float partialTick = event.getPartialTick();

            // Interpolate yaws for smooth rendering
            float headYaw = player.yHeadRotO + (player.yHeadRot - player.yHeadRotO) * partialTick;
            float bodyYaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * partialTick;

            // Translate sideways based on head yaw (World Space)
            double headRadians = Math.toRadians(headYaw);
            // Use same formula as camera for consistency
            double offsetX = -probeOffset * 0.1 * Math.cos(headRadians);
            double offsetZ = -probeOffset * 0.1 * Math.sin(headRadians);
            poseStack.translate(offsetX, 0, offsetZ);

            // Apply lean rotation using conjugation
            // 1. Rotate to Body Frame
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

            // 2. Apply Lean (Rotation around Z)
            poseStack.mulPose(Axis.ZP.rotationDegrees(probeOffset * -20.0F));

            // 3. Rotate back to World Frame
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - bodyYaw)));
        }
    }
}
