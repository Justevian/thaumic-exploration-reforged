package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class ReplicatorRenderer implements BlockEntityRenderer<Replicator.Tile, ReplicatorRenderer.State> {
    private final ItemModelResolver models;

    ReplicatorRenderer(BlockEntityRendererProvider.Context context) {
        models = context.itemModelResolver();
    }

    static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(Replicator.Tile entity, State state, float partialTick, Vec3 camera,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.item.clear();
        var level = entity.getLevel();
        var sample = entity.displayStack();
        if (level == null || sample.isEmpty()) return;
        state.time = level.getGameTime() % 3600 + partialTick;
        models.updateForTopItem(state.item, sample, ItemDisplayContext.GROUND, level, null,
            (int) entity.getBlockPos().asLong());
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1.33 + Math.sin(state.time / 16) * 0.05, 0.5);
        pose.rotateDegrees(Axis.YP, state.time * 2);
        pose.scale(1.4F, 1.4F, 1.4F);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override public AABB getRenderBoundingBox(Replicator.Tile entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0, 1, 0);
    }
}
