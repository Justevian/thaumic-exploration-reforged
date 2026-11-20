package tx;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.material.Fluids;
import thaumcraft.alchemy.CrucibleBlockEntity;
import thaumcraft.aspect.Aspect;
import thaumcraft.network.EssentiaSourceFxPayload;
import thaumcraft.platform.ModNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

final class EverfullUrn extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 9, 14),
        box(5, 9, 5, 11, 13, 11), box(4, 13, 4, 12, 16, 12));

    EverfullUrn(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET) && !stack.is(Items.GLASS_BOTTLE)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide()) {
            ItemStack result = stack.is(Items.BUCKET) ? new ItemStack(Items.WATER_BUCKET)
                : net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Exploration.URN_TILE.get()) return null;
        return (world, pos, current, entity) -> ((Tile) entity).tick((ServerLevel) world, pos);
    }

    static final class WaterOutput implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        private static final int CAPACITY = 9999;
        @Override public int getTanks() { return 1; }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            return tank == 0 ? drain(CAPACITY, FluidAction.SIMULATE) : net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? CAPACITY : 0; }
        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack fluid) { return false; }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack fluid, FluidAction action) { return 0; }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack fluid, FluidAction action) {
            return net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid, getFluidInTank(0))
                ? drain(fluid.getAmount(), action) : net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int amount, FluidAction action) {
            return amount > 0 ? new net.neoforged.neoforge.fluids.FluidStack(Fluids.WATER, Math.min(amount, CAPACITY))
                : net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
    }

    static final class Tile extends BlockEntity {
        private BlockPos target;
        private int ticks;

        Tile(BlockPos pos, BlockState state) {
            super(Exploration.URN_TILE.get(), pos, state);
        }

        void tick(ServerLevel level, BlockPos pos) {
            ticks++;
            if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) return;
            if (target != null && (!level.hasChunkAt(target) || !needsWater(level, target))) target = null;
            if (target == null && ticks % 20 == 0) {
                int closest = Integer.MAX_VALUE;
                for (BlockPos next : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
                    int distance = (int) next.distSqr(pos);
                    if (distance < closest && level.hasChunkAt(next) && needsWater(level, next)) {
                        target = next.immutable();
                        closest = distance;
                    }
                }
            }
            if (target == null) return;
            if (level.getBlockEntity(target) instanceof CrucibleBlockEntity crucible) {
                crucible.fillWater(10);
            } else if (ticks % 20 == 0) {
                BlockState cauldron = level.getBlockState(target);
                int amount = cauldron.is(Blocks.CAULDRON) ? 0 : cauldron.getValue(LayeredCauldronBlock.LEVEL);
                level.setBlockAndUpdate(target, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, amount + 1));
            } else {
                return;
            }
            if (ticks % 4 == 0) {
                BlockPos destination = target.above();
                ModNetwork.sendToNear(level, null, target.getX(), target.getY(), target.getZ(), 32,
                    new EssentiaSourceFxPayload(destination, (byte) (target.getX() - pos.getX()),
                        (byte) (target.getY() - pos.getY()), (byte) (target.getZ() - pos.getZ()), Aspect.WATER.getColor()));
            }
        }

        private static boolean needsWater(ServerLevel level, BlockPos pos) {
            if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
                return crucible.getWaterAmount() < CrucibleBlockEntity.TANK_CAPACITY;
            }
            BlockState state = level.getBlockState(pos);
            return state.is(Blocks.CAULDRON)
                || state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3;
        }
    }
}
