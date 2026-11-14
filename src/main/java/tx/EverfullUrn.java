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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.BUCKET) && !stack.is(Items.GLASS_BOTTLE)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack result = stack.is(Items.BUCKET) ? new ItemStack(Items.WATER_BUCKET)
                : net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
        }
        return InteractionResult.SUCCESS;
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

    static final class WaterOutput implements ResourceHandler<FluidResource> {
        private static final int CAPACITY = 9999;
        private static final FluidResource WATER = FluidResource.of(Fluids.WATER);

        @Override public int size() { return 1; }
        @Override public FluidResource getResource(int index) { return index == 0 ? WATER : FluidResource.EMPTY; }
        @Override public long getAmountAsLong(int index) { return index == 0 ? CAPACITY : 0; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return index == 0 && (resource.equals(WATER) || resource.isEmpty()) ? CAPACITY : 0;
        }
        @Override public boolean isValid(int index, FluidResource resource) { return false; }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 && resource.equals(WATER) ? Math.clamp(amount, 0, CAPACITY) : 0;
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
