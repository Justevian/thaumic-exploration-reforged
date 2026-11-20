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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.aspect.Aspect;
import thaumcraft.vis.VisNet;

final class EverburnUrn extends Block implements EntityBlock {
    EverburnUrn(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.EVERBURN_TILE.get()
            ? (world, pos, current, entity) -> ((Tile) entity).tick((ServerLevel) world, pos) : null;
    }

    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET) || !(level.getBlockEntity(pos) instanceof Tile tile)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide() && tile.lava.getFluidAmount() >= 1000) {
            tile.lava.drain(1000, FluidAction.EXECUTE);
            tile.setChanged();
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.LAVA_BUCKET)));
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    static final class Tile extends BlockEntity {
        private static final int CAPACITY = 4000;
        final FluidTank lava = new FluidTank(CAPACITY, fluid -> fluid.is(Fluids.LAVA)) {
            @Override public int fill(FluidStack fluid, FluidAction action) { return 0; }
            @Override protected void onContentsChanged() { Tile.this.setChanged(); }
        };

        Tile(BlockPos pos, BlockState state) { super(Exploration.EVERBURN_TILE.get(), pos, state); }

        void tick(ServerLevel world, BlockPos pos) {
            int stored = lava.getFluidAmount();
            if (world.getGameTime() % 5 == 0 && stored <= CAPACITY - 250
                && VisNet.drainVis(world, pos, Aspect.FIRE, 1) == 1) {
                lava.setFluid(new FluidStack(Fluids.LAVA, stored + 250));
                setChanged();
            }
        }

        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            var input = new PortNbt.Input(tag, registries);
            super.loadAdditional(tag, registries);
            lava.setFluid(new FluidStack(Fluids.LAVA, Math.clamp(input.getIntOr("lava", 0), 0, CAPACITY)));
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            var output = new PortNbt.Output(tag, registries);
            super.saveAdditional(tag, registries);
            output.putInt("lava", lava.getFluidAmount());
        }
    }
}
