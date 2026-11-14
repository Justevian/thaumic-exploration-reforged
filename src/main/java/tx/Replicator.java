package tx;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.EssentiaHandler;
import thaumcraft.wand.WandInteractable;
import thaumcraft.wand.Wands;

final class Replicator extends Block implements EntityBlock, WandInteractable {
    Replicator(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.REPLICATOR_TILE.get()
            ? (world, pos, current, entity) -> ((Tile) entity).tick(world.hasNeighborSignal(pos)) : null;
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (Wands.isWand(stack)) return onWandUseOn(level, pos, player, stack, hit.getDirection());
        if (!(level.getBlockEntity(pos) instanceof Tile tile)) return InteractionResult.PASS;
        if (!tile.output.isEmpty()) return useWithoutItem(state, level, pos, player, hit);
        if (!allowed(stack)) return InteractionResult.PASS;
        if (!level.isClientSide()) tile.setSample(stack);
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
        Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile tile)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!tile.output.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, tile.output);
                tile.output = ItemStack.EMPTY;
                tile.sync();
            } else {
                tile.setSample(ItemStack.EMPTY);
            }
            tile.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof Tile tile) || tile.sample.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) tile.start();
        return InteractionResult.SUCCESS;
    }

    @Override protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var tile = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        ItemStack block = new ItemStack(Exploration.REPLICATOR_ITEM.get());
        return tile instanceof Tile replicator && !replicator.output.isEmpty()
            ? List.of(block, replicator.output.copy()) : List.of(block);
    }

    private static boolean allowed(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(ItemTags.LEAVES) || stack.is(ItemTags.PLANKS)
            || stack.is(ItemTags.WOODEN_SLABS) || stack.is(ItemTags.WOODEN_STAIRS) || stack.is(ItemTags.SAND)
            || List.of(Items.STONE, Items.COBBLESTONE, Items.MOSSY_COBBLESTONE, Items.STONE_SLAB,
                Items.COBBLESTONE_SLAB, Items.COBBLESTONE_STAIRS, Items.SANDSTONE, Items.SANDSTONE_STAIRS,
                Items.SANDSTONE_SLAB, Items.BRICKS, Items.BRICK_STAIRS, Items.BRICK_SLAB, Items.STONE_BRICKS,
                Items.STONE_BRICK_STAIRS, Items.STONE_BRICK_SLAB).contains(stack.getItem());
    }

    static final class Tile extends thaumcraft.blockentity.TcBlockEntity {
        private ItemStack sample = ItemStack.EMPTY;
        private ItemStack output = ItemStack.EMPTY;
        private AspectList remaining = new AspectList();
        private int progress;
        private int interval;
        private boolean powered;

        Tile(BlockPos pos, BlockState state) { super(Exploration.REPLICATOR_TILE.get(), pos, state); }

        ItemStack displayStack() {
            return output.isEmpty() ? sample : output;
        }

        void setSample(ItemStack stack) {
            sample = stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem());
            progress = 0;
            interval = 0;
            remaining = new AspectList();
            sync();
        }

        boolean start() {
            if (sample.isEmpty() || !output.isEmpty() || progress > 0) return false;
            var cost = AspectHelper.getBonusTags(sample, AspectHelper.getObjectTags(sample));
            if (cost == null || cost.isEmpty()) return false;
            remaining = cost.copy();
            progress = 100;
            interval = 0;
            setChanged();
            return true;
        }

        void tick(boolean signal) {
            if (signal != powered) {
                powered = signal;
                if (signal) start();
                setChanged();
            }
            if (progress <= 0) return;
            if (!remaining.isEmpty()) {
                if (++interval >= 50) {
                    interval = 0;
                    for (var aspect : remaining.getAspects()) {
                        if (EssentiaHandler.drainEssentia(this, aspect, null, 8)) remaining.remove(aspect, 1);
                    }
                }
            } else if (--progress == 0) {
                output = sample.copy();
                sync();
            }
            setChanged();
        }

        @Override protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            sample = input.read("sample", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            output = input.read("output", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            remaining = input.read("remaining", AspectList.CODEC).orElseGet(AspectList::new);
            progress = input.getIntOr("progress", 0);
            interval = input.getIntOr("interval", 0);
            powered = input.getBooleanOr("powered", false);
        }

        @Override protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store("sample", ItemStack.OPTIONAL_CODEC, sample);
            output.store("output", ItemStack.OPTIONAL_CODEC, this.output);
            output.store("remaining", AspectList.CODEC, remaining);
            output.putInt("progress", progress);
            output.putInt("interval", interval);
            output.putBoolean("powered", powered);
        }
    }
}
