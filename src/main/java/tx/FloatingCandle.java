package tx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.block.CandleBlock;

final class FloatingCandle extends CandleBlock implements net.minecraft.world.level.block.EntityBlock {
    private static final VoxelShape SHAPE = box(6, 1, 6, 10, 16, 10);

    FloatingCandle(Properties properties) {
        super(properties);
    }

    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.9 + hover(pos, level.getGameTime());
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
    }

    static double hover(BlockPos pos, double time) {
        return 0.2 * Math.sin(((pos.getX() + pos.getY() + pos.getZ()) * 10.0 + time) / 30.0);
    }

    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    static final class Tile extends net.minecraft.world.level.block.entity.BlockEntity {
        Tile(BlockPos pos, BlockState state) {
            super(Exploration.FLOATING_CANDLE_TILE.get(), pos, state);
        }
    }
}
