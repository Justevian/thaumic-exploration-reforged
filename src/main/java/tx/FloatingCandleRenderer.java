package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import thaumcraft.client.geom.ModelBoxGeometry;
import thaumcraft.client.platform.ItemGeometry;
import thaumcraft.registry.ModBlocks;

final class FloatingCandleRenderer implements BlockEntityRenderer<FloatingCandle.Tile, FloatingCandleRenderer.State> {
    private static final Identifier WAX = Identifier.parse("thaumicexploration:textures/model/floatycandle.png");
    private static final Identifier WICK = Identifier.parse("thaumicexploration:textures/model/floatycandlestub.png");

    FloatingCandleRenderer(BlockEntityRendererProvider.Context context) {}

    static final class State extends BlockEntityRenderState {
        double hover;
        int color;
        int frame;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(FloatingCandle.Tile tile, State state, float partialTick, Vec3 camera,
        ModelFeatureRenderer.@Nullable CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, overlay);
        long time = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime();
        state.hover = FloatingCandle.hover(tile.getBlockPos(), time + partialTick);
        state.frame = (int) (time % 8);
        int[] colors = ModBlocks.CANDLE_TINTS;
        state.color = ARGB.opaque(0x1D1D21);
        for (int i = 0; i < colors.length; i++) {
            if (tile.getBlockState().is(Exploration.FLOATING_CANDLES.get(i).get().getBlock())) {
                state.color = ARGB.opaque(colors[i]);
                break;
            }
        }
    }

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0, state.hover, 0);
        ItemGeometry.solid(collector.order(0), pose, WAX, false, sink ->
            ModelBoxGeometry.box(sink, 6, 4, 6, 4, 8, 4, 0, state.frame * 12, 16, 96, false, state.color, state.lightCoords));
        ItemGeometry.solid(collector.order(0), pose, WICK, false, sink ->
            ModelBoxGeometry.box(sink, 7.5F, 12, 7.5F, 1, 2, 1, 0, 0, 16, 16, false, -1, state.lightCoords));
        pose.popPose();
    }
}
