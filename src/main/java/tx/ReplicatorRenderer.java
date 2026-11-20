package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import thaumcraft.client.platform.ImmediateGeometry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import thaumcraft.client.platform.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class ReplicatorRenderer extends PortBlockRenderer<Replicator.Tile, ReplicatorRenderer.State> {
    private final net.minecraft.client.renderer.entity.ItemRenderer models;

    ReplicatorRenderer(BlockEntityRendererProvider.Context context) {
        models = context.getItemRenderer();
    }

    static final class State extends PortBlockRenderer.State {
        net.minecraft.world.item.ItemStack item = net.minecraft.world.item.ItemStack.EMPTY;
        float time;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(Replicator.Tile entity, State state, float partialTick, Vec3 camera,
        Object overlay) {
        super.extractRenderState(entity, state, partialTick, camera, overlay);
        state.item = net.minecraft.world.item.ItemStack.EMPTY;
        var level = entity.getLevel();
        var sample = entity.displayStack();
        if (level == null || sample.isEmpty()) return;
        state.time = level.getGameTime() % 3600 + partialTick;
        state.item = sample.copy();
    }

    @Override public void submit(State state, PoseStack pose, ImmediateGeometry collector, CameraRenderState camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1.33 + Math.sin(state.time / 16) * 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.time * 2));
        pose.scale(1.4F, 1.4F, 1.4F);
        models.renderStatic(state.item, ItemDisplayContext.GROUND, state.lightCoords, OverlayTexture.NO_OVERLAY, pose, collector.buffers, net.minecraft.client.Minecraft.getInstance().level, 0);
        pose.popPose();
    }

    @Override public AABB getRenderBoundingBox(Replicator.Tile entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0, 1, 0);
    }
}
