package tx;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = Exploration.ID)
public final class PortSmoke {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("thaumicexploration.smoke")) return;
        var server = event.getServer();
        server.execute(() -> {
            try {
                var level = server.overworld();
                var registries = level.registryAccess();
                int blocks = 0;
                for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
                    if (!entry.getKey().location().getNamespace().equals(Exploration.ID)
                        || !(entry.getValue() instanceof EntityBlock factory)) continue;
                    var state = entry.getValue().defaultBlockState();
                    var tile = factory.newBlockEntity(BlockPos.ZERO, state);
                    if (tile == null) continue;
                    var saved = tile.saveWithFullMetadata(registries);
                    var restored = BlockEntity.loadStatic(BlockPos.ZERO, state, saved, registries);
                    if (restored == null || restored.getType() != tile.getType()) throw new AssertionError(entry.getKey());
                    blocks++;
                }
                var inventory = BoundStorage.inventory(level, "port-smoke-restart");
                if (Boolean.getBoolean("thaumicexploration.verifySavedStorage")) {
                    if (!inventory.getItem(3).is(Items.DIAMOND) || inventory.getItem(3).getCount() != 4)
                        throw new AssertionError("Bound inventory was not restored after restart");
                    LogUtils.getLogger().info("TX_BOUND_STORAGE_RELOADED");
                } else {
                    inventory.setItem(3, new ItemStack(Items.DIAMOND, 4));
                }
                testSorter(level);
                var ringTag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.parse("curios:ring"));
                if (!new ItemStack(Exploration.TENTACLE_RING.get()).is(ringTag)) throw new AssertionError("Curios ring tag");
                if (!(Exploration.METEOR.get() instanceof net.minecraft.world.item.ArmorItem boots)
                    || boots.getEquipmentSlot() != net.minecraft.world.entity.EquipmentSlot.FEET) throw new AssertionError("Boots slot");
                var water = new EverfullUrn.WaterOutput();
                var action = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
                if (water.drain(1000, action).getAmount() != 1000 || water.drain(1000, action).getAmount() != 1000)
                    throw new AssertionError("Everfull urn output");
                if (Exploration.MINION.get().create(level) == null) throw new AssertionError("Tentacle minion");
                Class.forName("thaumcraft.wand.WandItem");
                Class.forName("thaumcraft.crafting.ArcaneWandRecipe");
                LogUtils.getLogger().info("TX_PORT_SMOKE_PASSED: {} block entities, linked inventory, sorter transfer, equipment slots, urn and minion", blocks);
            } catch (Throwable error) {
                LogUtils.getLogger().error("TX_PORT_SMOKE_FAILED", error);
            } finally {
                server.halt(false);
            }
        });
    }
    private static void testSorter(net.minecraft.server.level.ServerLevel level) {
        var origin = new BlockPos(0, 80, 0);
        var input = origin.west();
        var output = origin.east();
        var state = net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState();
        level.setBlockAndUpdate(input, state);
        level.setBlockAndUpdate(output, state);
        level.removeBlock(origin, false);
        level.setBlockAndUpdate(origin, Exploration.SORTER.get().defaultBlockState());
        var from = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(input);
        var to = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(output);
        var sorter = (Sorter.Tile) level.getBlockEntity(origin);
        var up = net.minecraft.core.Direction.UP;
        sorter.toggle(new Sorter.Endpoint(input, up, true));
        sorter.toggle(new Sorter.Endpoint(output, up, false));
        from.setItem(0, new ItemStack(Items.DIAMOND, 4));
        to.setItem(0, new ItemStack(Items.DIAMOND, 7));
        if (!sorter.transfer(level) || from.getItem(0).getCount() != 3 || to.getItem(0).getCount() != 8)
            throw new AssertionError("Sorter should move exactly one item");
        for (int i = 0; i < to.getContainerSize(); i++) to.setItem(i, new ItemStack(Items.DIAMOND, 64));
        if (sorter.transfer(level) || from.getItem(0).getCount() != 3)
            throw new AssertionError("A full destination must leave the source untouched");
    }
}
