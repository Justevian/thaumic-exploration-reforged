package tx;

import java.util.ArrayList;
import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import thaumcraft.item.golem.GolemBellItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@EventBusSubscriber(modid = Exploration.ID)
final class Sorter extends Block implements EntityBlock {
    private static final String LINK = "tx_sorter";

    Sorter(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.SORTER_TILE.get()
            ? (world, pos, current, tile) -> { if (world.getGameTime() % 8 == 0) ((Tile) tile).transfer((ServerLevel) world); }
            : null;
    }

    @SubscribeEvent
    static void link(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof GolemBellItem) || event.getFace() == null) return;
        var player = event.getEntity();
        var stack = event.getItemStack();
        var pos = event.getPos();
        var level = event.getLevel();
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (level.getBlockState(pos).is(Exploration.SORTER.get())) {
            if (!level.isClientSide()) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                    if (player.isShiftKeyDown()) tag.remove(LINK);
                    else tag.putLong(LINK, pos.asLong());
                    tag.putString(LINK + "_dimension", level.dimension().location().toString());
                });
                player.sendSystemMessage(Component.translatable(player.isShiftKeyDown()
                    ? "message.thaumicexploration.sorter_cleared" : "message.thaumicexploration.sorter_selected"));
            }
        } else {
            if (!data.contains(LINK) || !level.dimension().location().toString().equals(PortNbt.getStringOr(data, LINK + "_dimension", ""))) return;
            var origin = BlockPos.of(PortNbt.getLongOr(data, LINK, 0));
            if (!level.hasChunkAt(origin) || !(level.getBlockEntity(origin) instanceof Tile tile)
                || origin.distSqr(pos) > 256 || level.getCapability(Capabilities.ItemHandler.BLOCK, pos, event.getFace()) == null) return;
            if (level instanceof ServerLevel server && server.mayInteract(player, origin) && server.mayInteract(player, pos)) {
                boolean input = player.isShiftKeyDown();
                boolean linked = tile.toggle(new Endpoint(pos, event.getFace(), input));
                player.sendSystemMessage(Component.translatable(linked ? input
                    ? "message.thaumicexploration.sorter_input" : "message.thaumicexploration.sorter_output"
                    : "message.thaumicexploration.sorter_unlinked"));
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    record Endpoint(BlockPos pos, Direction side, boolean input) {
        static final Codec<Endpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Endpoint::pos),
            Direction.CODEC.fieldOf("side").forGetter(Endpoint::side),
            Codec.BOOL.fieldOf("input").forGetter(Endpoint::input)
        ).apply(instance, Endpoint::new));
    }

    static final class Tile extends BlockEntity {
        private final List<Endpoint> endpoints = new ArrayList<>();

        Tile(BlockPos pos, BlockState state) { super(Exploration.SORTER_TILE.get(), pos, state); }

        boolean toggle(Endpoint endpoint) {
            boolean removed = endpoints.remove(endpoint);
            if (!removed) {
                endpoints.removeIf(other -> other.pos.equals(endpoint.pos));
                endpoints.add(endpoint);
            }
            setChanged();
            return !removed;
        }

        boolean transfer(ServerLevel level) {
            for (var from : endpoints) {
                if (!from.input || !level.hasChunkAt(from.pos)) continue;
                var source = level.getCapability(Capabilities.ItemHandler.BLOCK, from.pos, from.side);
                if (source == null) continue;
                for (int slot = 0; slot < source.getSlots(); slot++) {
                    var item = source.extractItem(slot, 1, true);
                    if (item.isEmpty()) continue;
                    for (var to : endpoints) {
                        if (to.input || !level.hasChunkAt(to.pos)) continue;
                        var destination = level.getCapability(Capabilities.ItemHandler.BLOCK, to.pos, to.side);
                        if (destination == null || destination == source) continue;
                        boolean matching = false;
                        for (int index = 0; index < destination.getSlots(); index++) {
                            if (ItemStack.isSameItemSameComponents(item, destination.getStackInSlot(index))) { matching = true; break; }
                        }
                        if (!matching) continue;
                        if (!ItemHandlerHelper.insertItemStacked(destination, item, true).isEmpty()) continue;
                        ItemStack extracted = source.extractItem(slot, 1, false);
                        if (extracted.isEmpty()) continue;
                        ItemStack remainder = ItemStack.isSameItemSameComponents(item, extracted)
                            ? ItemHandlerHelper.insertItemStacked(destination, extracted, false) : extracted;
                        if (remainder.isEmpty()) return true;
                        remainder = source.insertItem(slot, remainder, false);
                        if (!remainder.isEmpty()) remainder = ItemHandlerHelper.insertItemStacked(source, remainder, false);
                        if (!remainder.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(level,
                                from.pos.getX() + 0.5, from.pos.getY() + 0.5, from.pos.getZ() + 0.5, remainder);
                        }
                        return false;
                    }
                }
            }
            return false;
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            var input = new PortNbt.Input(tag, registries);
            super.loadAdditional(tag, registries);
            endpoints.clear();
            endpoints.addAll(input.read("endpoints", Endpoint.CODEC.listOf()).orElse(List.of()));
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            var output = new PortNbt.Output(tag, registries);
            super.saveAdditional(tag, registries);
            output.store("endpoints", Endpoint.CODEC.listOf(), endpoints);
        }
    }
}
