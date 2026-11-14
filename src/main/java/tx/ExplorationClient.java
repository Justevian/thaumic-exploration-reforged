package tx;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.client.render.EssentiaJarRenderer;

@EventBusSubscriber(modid = Exploration.ID, value = Dist.CLIENT)
final class ExplorationClient {
    @SubscribeEvent
    static void candleColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        int[] colors = thaumcraft.registry.ModBlocks.CANDLE_TINTS;
        for (int index = 0; index < Exploration.FLOATING_CANDLES.size(); index++) {
            int color = index < colors.length ? colors[index] : 0x1D1D21;
            event.register(java.util.List.of(net.minecraft.client.color.block.BlockTintSources.constant(
                net.minecraft.util.ARGB.opaque(color))), Exploration.FLOATING_CANDLES.get(index).get().getBlock());
        }
    }

    @SubscribeEvent
    static void itemRenderers(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(Exploration.ID, "think_tank"),
            ThinkTankItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Exploration.MINION.get(), thaumcraft.client.render.entity.taint.TaintMobRenderer.provider(
            thaumcraft.client.render.entity.taint.TaintEntityModel.Kind.TENTACLE_TINY, 0.2F));
        event.registerBlockEntityRenderer(Exploration.JAR_TILE.get(), EssentiaJarRenderer::new);
        event.registerBlockEntityRenderer(Exploration.TRASH_TILE.get(), EssentiaJarRenderer::new);
        event.registerBlockEntityRenderer(Exploration.REPLICATOR_TILE.get(), ReplicatorRenderer::new);
        event.registerBlockEntityRenderer(Exploration.THINK_TANK_TILE.get(), ThinkTankRenderer::new);
        event.registerBlockEntityRenderer(Exploration.FLOATING_CANDLE_TILE.get(), FloatingCandleRenderer::new);
    }
}
