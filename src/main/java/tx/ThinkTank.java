package tx;

import net.minecraft.core.BlockPos;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.registry.ModItems;

final class ThinkTank extends Block implements EntityBlock {
    ThinkTank(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.THINK_TANK_TILE.get()
            ? (world, pos, current, entity) -> ((Tile) entity).tick(world, pos) : null;
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile tank) || !Tile.isBook(stack)) return InteractionResult.PASS;
        if (!level.isClientSide() && tank.input.addItem(stack.copyWithCount(1)).isEmpty() && !player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
        Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile tank)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            var inventory = player.isShiftKeyDown() ? tank.input : tank.output;
            var stack = inventory.removeItem(0, 64);
            if (!stack.isEmpty()) {
                player.getInventory().add(stack);
                if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,
                    player.getX(), player.getY(), player.getZ(), stack);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        var items = new java.util.ArrayList<ItemStack>();
        items.add(new ItemStack(Exploration.THINK_TANK_ITEM.get()));
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof Tile tank) {
            if (!tank.input.isEmpty()) items.add(tank.input.getItem(0).copy());
            if (!tank.output.isEmpty()) items.add(tank.output.getItem(0).copy());
        }
        return items;
    }

    static final class Tile extends BlockEntity {
        final SimpleContainer input = new SimpleContainer(1) {
            @Override public void setChanged() { super.setChanged(); Tile.this.setChanged(); }
            @Override public boolean canPlaceItem(int index, ItemStack stack) { return isBook(stack); }
        };
        final SimpleContainer output = new SimpleContainer(1) {
            @Override public void setChanged() { super.setChanged(); Tile.this.setChanged(); }
            @Override public boolean canPlaceItem(int index, ItemStack stack) { return false; }
        };
        private int progress;

        Tile(BlockPos pos, BlockState state) {
            super(Exploration.THINK_TANK_TILE.get(), pos, state);


        }

        static boolean isBook(ItemStack stack) { return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK); }

        void tick(Level level, BlockPos pos) {
            ItemStack book = input.getItem(0);
            ItemStack fragments = output.getItem(0);
            if (!isBook(book) || fragments.getCount() > 62 || !spaceAvailable(level, pos)) {
                if (progress != 0) {
                    progress = 0;
                    setChanged();
                }
                return;
            }
            if (++progress == 240) {
                progress = 0;
                float roll = level.getRandom().nextFloat();
                int count = book.is(Items.ENCHANTED_BOOK) ? (roll > 0.66F ? 1 : roll > 0.33F ? 2 : 0)
                    : (roll <= 0.1F ? 1 : 0);
                if (count > 0) output.setItem(0, new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get(), fragments.getCount() + count));
                input.removeItem(0, 1);
            }
            setChanged();
        }

        private boolean spaceAvailable(Level level, BlockPos pos) {
            if (!level.getBlockState(pos.below()).is(Blocks.BOOKSHELF)) return false;
            for (BlockPos adjacent : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (!adjacent.equals(pos) && !adjacent.equals(pos.below()) && !level.getBlockState(adjacent).canBeReplaced()) return false;
            }
            return true;
        }

        @Override protected void loadAdditional(ValueInput inputData) {
            super.loadAdditional(inputData);
            input.setItem(0, inputData.read("books", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
            output.setItem(0, inputData.read("fragments", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
            progress = inputData.getIntOr("progress", 0);
        }

        @Override protected void saveAdditional(ValueOutput data) {
            super.saveAdditional(data);
            data.store("books", ItemStack.OPTIONAL_CODEC, input.getItem(0));
            data.store("fragments", ItemStack.OPTIONAL_CODEC, output.getItem(0));
            data.putInt("progress", progress);
        }
    }
}
