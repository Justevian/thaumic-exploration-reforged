package tx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Loads real client models; excluded from the published artifact. */
@EventBusSubscriber(modid = Exploration.ID, value = Dist.CLIENT)
public final class ClientPortSmoke {
    private static boolean checked;
    private static int worldTicks;
    private static long opened;
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("thaumicexploration.clientSmoke")) return;
        if (checked) {
            var game=Minecraft.getInstance();
            if (game.level!=null && game.player!=null && ++worldTicks>40) {
                checked=false; // completion exits the client before another tick
                try { drawWorldModels(game); }
                catch(Throwable failure) { com.mojang.logging.LogUtils.getLogger().error("TX_CLIENT_SMOKE_FAILED",failure); }
                finally {game.stop();}
            } else if (System.nanoTime()-opened>120_000_000_000L) {
                com.mojang.logging.LogUtils.getLogger().error("TX_CLIENT_SMOKE_FAILED: world load timeout; screen={}",game.screen);
                game.stop();
            }
            return;
        }
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || mc.screen == null) return;
        checked = true;
        try {
            Class.forName("thaumcraft.client.render.WandSpecialRenderer");
            int items = 0;
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                if (!entry.getKey().location().getNamespace().equals(Exploration.ID)) continue;
                var stack = new ItemStack(entry.getValue());
                var model = mc.getItemRenderer().getModel(stack, null, null, 0);
                if (model == mc.getModelManager().getMissingModel()) throw new AssertionError("Missing item model: " + entry.getKey().location());
                if (model.getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY).contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
                    throw new AssertionError("Missing item texture: " + entry.getKey().location());
                }
                for (var quad : model.getQuads(null, null, net.minecraft.util.RandomSource.create(0))) {
                    if (quad.isTinted() && (mc.getItemColors().getColor(stack, quad.getTintIndex()) >>> 24) != 255) {
                        throw new AssertionError("Non-opaque item tint: " + entry.getKey().location());
                    }
                }
                items++;
            }
            com.mojang.logging.LogUtils.getLogger().info("TX_CLIENT_MODELS_PASSED: {} item models and client mixin targets", items);
            opened=System.nanoTime();
            mc.createWorldOpenFlows().openWorld("exploration-port-smoke", () -> {
                com.mojang.logging.LogUtils.getLogger().error("TX_CLIENT_SMOKE_FAILED: could not open world"); mc.stop();
            });
        } catch (Throwable failure) {
            com.mojang.logging.LogUtils.getLogger().error("TX_CLIENT_SMOKE_FAILED", failure); mc.stop();
        }
    }
    private static void drawWorldModels(Minecraft mc) {
        int renderers=0;
        var buffers=mc.renderBuffers().bufferSource();
        for (var entry:BuiltInRegistries.BLOCK.entrySet()) {
            if (!entry.getKey().location().getNamespace().equals(Exploration.ID) || !(entry.getValue() instanceof net.minecraft.world.level.block.EntityBlock factory)) continue;
            var state=entry.getValue().defaultBlockState();
            var tile=factory.newBlockEntity(new net.minecraft.core.BlockPos(0,80,0),state);
            if (tile==null) continue;
            tile.setLevel(mc.level);
            var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(tile);
            if (renderer!=null) {
                renderer.render(tile,0.5F,new com.mojang.blaze3d.vertex.PoseStack(),buffers,0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                buffers.endBatch();renderers++;
            }
        }
        var stack = new ItemStack(Exploration.THINK_TANK_ITEM.get());
        var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        if (!model.isCustomRenderer()) throw new AssertionError("Think Tank custom item renderer not selected");
        net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(
            stack, net.minecraft.world.item.ItemDisplayContext.GUI, new com.mojang.blaze3d.vertex.PoseStack(), buffers, 0xF000F0, 0);
        buffers.endBatch();
        if (renderers < 19) throw new AssertionError("Too few block entity renderers: " + renderers);
        com.mojang.logging.LogUtils.getLogger().info("TX_CLIENT_SMOKE_PASSED: integrated world, {} block entity renderer calls and Think Tank item renderer", renderers);
    }
}
