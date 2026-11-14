package tx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.JarBlock;
import thaumcraft.essentia.JarBlockEntity;

final class TrashJar extends JarBlock {
    TrashJar(Properties properties) {
        super(false, properties);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return !level.isClientSide() && type == Exploration.TRASH_TILE.get()
            ? (world, pos, current, entity) -> JarBlockEntity.serverTick(world, pos, current, (Tile) entity)
            : null;
    }

    @Override protected java.util.List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return java.util.List.of(new ItemStack(Exploration.TRASH_ITEM.get()));
    }

    static final class Tile extends JarBlockEntity {
        private int ticks;

        Tile(BlockPos pos, BlockState state) {
            super(Exploration.TRASH_TILE.get(), pos, state);
        }

        @Override protected void tick() {
            if (getAmount() == 0) {
                ticks = 0;
            } else if (++ticks == 6) {
                ticks = 0;
                super.takeFromContainer(getAspect(), 1);
            }
            super.tick();
        }

        @Override public boolean canOutputTo(Direction face) { return false; }
        @Override public boolean takeFromContainer(Aspect aspect, int amount) { return false; }
        @Override public int getMinimumSuction() { return 8; }
        @Override public int getSuctionAmount(Direction face) { return getAmount() < getMaxAmount() ? 8 : 0; }
    }
}
