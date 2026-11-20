package tx;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.client.render.EssentiaJarRenderer;

@EventBusSubscriber(modid = Exploration.ID, value = Dist.CLIENT)
final class ExplorationClient {
    @SubscribeEvent
    static void candleColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        int[] colors = thaumcraft.registry.ModBlocks.CANDLE_TINTS;
        for (int index = 0; index < Exploration.FLOATING_CANDLES.size(); index++) {
            int color = index < colors.length ? colors[index] : 0x1D1D21;
            event.register((state, level, pos, layer) -> layer == 0 ? 0xFF000000 | color : -1,
                Exploration.FLOATING_CANDLES.get(index).get().getBlock());
        }
    }

    @SubscribeEvent
    static void candleItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        int[] colors = thaumcraft.registry.ModBlocks.CANDLE_TINTS;
        for (int index = 0; index < Exploration.FLOATING_CANDLES.size(); index++) {
            int color = index < colors.length ? colors[index] : 0x1D1D21;
            event.register((stack, layer) -> layer == 0 ? 0xFF000000 | color : -1,
                Exploration.FLOATING_CANDLES.get(index).get());
        }
    }

    @SubscribeEvent
    static void itemRenderers(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private ThinkTankItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ThinkTankItemRenderer();
                return renderer;
            }
        }, Exploration.THINK_TANK_ITEM.get());
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
