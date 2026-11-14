package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.ItemGeometry;

final class ThinkTankRenderer implements BlockEntityRenderer<ThinkTank.Tile, ThinkTankRenderer.State> {
    private static final Identifier GLASS = Identifier.parse("thaumicexploration:textures/model/largejar.png");
    private static final Identifier BRINE = Identifier.parse("thaumicexploration:textures/model/largejarbrine.png");
    private static final Identifier LID = Identifier.parse("thaumcraft:textures/model/jar.png");
    private static final Identifier BRAIN = Identifier.parse("thaumcraft:textures/model/brain2.png");

    ThinkTankRenderer(BlockEntityRendererProvider.Context context) {}

    static final class State extends BlockEntityRenderState {
        float time;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(ThinkTank.Tile tile, State state, float partialTick, Vec3 camera,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, overlay);
        state.time = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime() % 72000 + partialTick;
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        renderModel(pose, collector, state.lightCoords, state.time);
    }

    static void renderModel(PoseStack pose, SubmitNodeCollector collector, int light, float time) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.rotateDegrees(Axis.XP, 180);
        ItemGeometry.solid(collector.order(0), pose, LID, false, sink ->
            ModelBoxGeometry.box(sink, -3, -18, -3, 6, 2, 6, 0, 24, 64, 32, true, -1, light));
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.translate(0, -0.73 + Math.sin(time / 15 + i * 2) * 0.045, 0);
            pose.rotateDegrees(Axis.YP, i * 120 + time * 0.65F);
            pose.translate(0, 0, -0.25);
            pose.scale(0.3F, 0.3F, 0.3F);
            ItemGeometry.solid(collector.order(0), pose, BRAIN, false, sink -> {
                ModelBoxGeometry.box(sink, -6, 8, -8, 12, 10, 16, 0, 0, 128, 64, true, -1, light);
                ModelBoxGeometry.box(sink, -4, 18, 0, 8, 3, 7, 64, 0, 128, 64, true, -1, light);
                ModelBoxGeometry.box(sink, -1, 18, -2, 2, 6, 2, 0, 32, 128, 64, true, -1, light);
            });
            pose.popPose();
        }
        ItemGeometry.solid(collector.order(1), pose, BRINE, true, sink ->
            ModelBoxGeometry.box(sink, -7, -15, -7, 14, 14, 14, 0, 0, 64, 32, true, -1, light));
        ItemGeometry.solid(collector.order(2), pose, GLASS, true, sink ->
            ModelBoxGeometry.box(sink, -8, -16, -8, 16, 16, 16, 0, 0, 64, 32, true, -1, light));
        pose.popPose();
    }

    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ThinkTank.Tile tile) {
        return new net.minecraft.world.phys.AABB(tile.getBlockPos()).expandTowards(0, 0.125, 0);
    }
}
