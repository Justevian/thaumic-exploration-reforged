package tx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import thaumcraft.aspect.Aspect;
import thaumcraft.vis.VisNet;

final class EverburnUrn extends Block implements EntityBlock {
    EverburnUrn(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.EVERBURN_TILE.get()
            ? (world, pos, current, entity) -> ((Tile) entity).tick((ServerLevel) world, pos) : null;
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET) || !(level.getBlockEntity(pos) instanceof Tile tile)) return InteractionResult.PASS;
        if (!level.isClientSide() && tile.lava.getAmountAsInt(0) >= 1000) {
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                if (tile.lava.extract(0, FluidResource.of(Fluids.LAVA), 1000, transaction) != 1000) return InteractionResult.FAIL;
                transaction.commit();
            }
            tile.setChanged();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.LAVA_BUCKET)));
        }
        return InteractionResult.SUCCESS;
    }

    static final class Tile extends BlockEntity {
        private static final int CAPACITY = 4000;
        final FluidStacksResourceHandler lava = new FluidStacksResourceHandler(1, CAPACITY) {
            @Override public boolean isValid(int index, FluidResource resource) { return false; }
            @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) { return 0; }
            @Override protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack previous) {
                Tile.this.setChanged();
            }
        };

        Tile(BlockPos pos, BlockState state) { super(Exploration.EVERBURN_TILE.get(), pos, state); }

        void tick(ServerLevel world, BlockPos pos) {
            int stored = lava.getAmountAsInt(0);
            if (world.getGameTime() % 5 == 0 && stored <= CAPACITY - 250
                && VisNet.drainVis(world, pos, Aspect.FIRE, 1) == 1) {
                lava.set(0, FluidResource.of(Fluids.LAVA), stored + 250);
                setChanged();
            }
        }

        @Override protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            lava.set(0, FluidResource.of(Fluids.LAVA), Math.clamp(input.getIntOr("lava", 0), 0, CAPACITY));
        }

        @Override protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("lava", lava.getAmountAsInt(0));
        }
    }
}
