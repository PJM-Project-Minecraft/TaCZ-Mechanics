package ru.liko.tacz_mechanics.movement.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.movement.MovementPosture;
import ru.liko.tacz_mechanics.movement.PlayerState;

/**
 * Отладочный контур реального ПОВЁРНУТОГО бокса тела лёжа (OBB), по которому считаются попадания
 * пуль ({@code CrawlObb}/{@code TaczBulletObbMixin}). Рисуется зелёным поверх ванильного белого
 * AABB, когда включён показ хитбоксов (F3+B). Видно, что пуля бьёт по тонкому телу, а не по углам.
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT)
public final class CrawlObbDebugRenderer {

    private CrawlObbDebugRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!Config.Movement.enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) return; // только при F3+B

        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffer.getBuffer(RenderType.lines());

        for (Player player : mc.level.players()) {
            PlayerState state = MovementClientHandler.getStateForPlayer(player);
            if (state == null || !state.isCrawling()) continue;
            renderObb(poseStack, vc, player, cam, partial);
        }

        buffer.endBatch(RenderType.lines());
    }

    private static void renderObb(PoseStack poseStack, VertexConsumer vc, Player player, Vec3 cam, float partial) {
        double px = Mth.lerp(partial, player.xOld, player.getX());
        double py = Mth.lerp(partial, player.yOld, player.getY());
        double pz = Mth.lerp(partial, player.zOld, player.getZ());
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);

        double[] center = MovementPosture.crawlCenterXZ(px, pz, yaw);
        double cx = center[0];
        double cy = py + MovementPosture.CRAWL_HEIGHT / 2.0;
        double cz = center[1];

        double halfAlong = MovementPosture.CRAWL_LENGTH / 2.0;
        double halfAcross = MovementPosture.CRAWL_WIDTH / 2.0;
        double halfUp = MovementPosture.CRAWL_HEIGHT / 2.0;

        poseStack.pushPose();
        poseStack.translate(cx - cam.x, cy - cam.y, cz - cam.z);
        // local +Z → ось тела u=(-sin,cos); local +X → поперечная v=(cos,sin)
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        LevelRenderer.renderLineBox(
            poseStack, vc,
            -halfAcross, -halfUp, -halfAlong,
            halfAcross, halfUp, halfAlong,
            0.1f, 1.0f, 0.1f, 1.0f
        );
        poseStack.popPose();
    }
}
