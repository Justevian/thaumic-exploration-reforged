package tx;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectHelper;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.registry.ModBlocks;

final class SoulCrucible extends Block implements EntityBlock {
    private static final net.minecraft.world.level.block.state.properties.BooleanProperty WORKING =
        net.minecraft.world.level.block.state.properties.BooleanProperty.create("working");

    SoulCrucible(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override protected void createBlockStateDefinition(
        net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(WORKING);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.SOUL_CRUCIBLE_TILE.get()
            ? (world, pos, current, entity) -> ((Tile) entity).tick((ServerLevel) world, pos) : null;
    }

    static final class Tile extends BlockEntity implements EssentiaTransport, thaumcraft.aspect.AspectSource {
        private AspectList contents = new AspectList();
        private float flux;
        private LivingEntity target;
        private int ticks;

        Tile(BlockPos pos, BlockState state) { super(Exploration.SOUL_CRUCIBLE_TILE.get(), pos, state); }

        void tick(ServerLevel world, BlockPos pos) {
            var above = world.getBlockState(pos.above());
            if (!above.isAir() && !above.is(ModBlocks.FLUX_GAS.get()) && !above.is(ModBlocks.FLUX_GOO.get())) {
                target = null;
                updateAppearance(world, pos);
                return;
            }
            if (flux > 1 && above.isAir()) {
                world.setBlockAndUpdate(pos.above(), (world.getRandom().nextBoolean() ? ModBlocks.FLUX_GAS.get()
                    : ModBlocks.FLUX_GOO.get()).defaultBlockState());
                flux--;
                setChanged();
            }
            ticks++;
            if (target != null && !eligible(target, pos)) target = null;
            if (target == null && ticks % 5 == 0) {
                target = world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(5), entity -> eligible(entity, pos))
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)))).orElse(null);
            }
            updateAppearance(world, pos);
            if (target == null || ticks % 10 != 0) return;
            var victim = target;
            var tags = AspectHelper.getEntityTags(victim);
            float damage = victim.getHealth() <= 1 ? 100 : 1;
            if (victim.hurtServer(world, world.damageSources().magic(), damage) && !victim.isAlive() && tags != null) {
                harvest(world, tags);
                target = null;
            }
        }

        private void updateAppearance(ServerLevel world, BlockPos pos) {
            var state = getBlockState();
            boolean working = target != null;
            if (state.getValue(WORKING) != working) {
                world.setBlock(pos, state.setValue(WORKING, working), Block.UPDATE_CLIENTS);
            }
        }

        private boolean eligible(LivingEntity entity, BlockPos pos) {
            return entity.isAlive() && !(entity instanceof Player)
                && !(entity instanceof thaumcraft.entity.golem.GolemBase)
                && !(entity instanceof thaumcraft.entity.monster.ThaumicSlime)
                && entity.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 36;
        }

        void harvest(ServerLevel world, AspectList tags) {
            var random = world.getRandom();
            for (var aspect : tags.getAspects()) {
                for (int i = 0; i < tags.getAmount(aspect); i++) {
                    if (random.nextInt(3) == 0) contents.add(aspect, 1);
                    else if (random.nextBoolean()) {
                        var parts = aspect.getComponents();
                        contents.add(parts == null || parts.length == 0 ? aspect : parts[random.nextInt(parts.length)], 1);
                    } else flux += 0.4F;
                }
            }
            setChanged();
        }

        @Override public AspectList getAspects() { return contents.copy(); }
        @Override public void setAspects(AspectList aspects) { contents = aspects.copy(); setChanged(); }
        @Override public boolean doesContainerAccept(Aspect aspect) { return false; }
        @Override public int addToContainer(Aspect aspect, int amount) { return amount; }
        @Override public boolean takeFromContainer(Aspect aspect, int amount) {
            return amount > 0 && doesContainerContainAmount(aspect, amount)
                && takeEssentia(aspect, amount, Direction.DOWN) == amount;
        }
        @Override public boolean doesContainerContainAmount(Aspect aspect, int amount) {
            return contents.getAmount(aspect) >= amount;
        }
        @Override public int containerContains(Aspect aspect) { return contents.getAmount(aspect); }

        @Override public boolean isConnectable(Direction face) { return face != Direction.UP; }
        @Override public boolean canInputFrom(Direction face) { return false; }
        @Override public boolean canOutputTo(Direction face) { return isConnectable(face); }
        @Override public void setSuction(Aspect aspect, int amount) {}
        @Override public Aspect getSuctionType(Direction face) { return null; }
        @Override public int getSuctionAmount(Direction face) { return 0; }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return false; }
        @Override public int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }
        @Override public Aspect getEssentiaType(Direction face) { return contents.getAspects().stream().findFirst().orElse(null); }
        @Override public int getEssentiaAmount(Direction face) { return contents.getAmount(getEssentiaType(face)); }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction face) {
            if (!canOutputTo(face) || amount <= 0) return 0;
            int taken = Math.min(amount, contents.getAmount(aspect));
            if (taken > 0) {
                contents.remove(aspect, taken);
                setChanged();
            }
            return taken;
        }

        @Override protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            contents = input.read("essentia", AspectList.CODEC).orElseGet(AspectList::new);
            flux = input.getFloatOr("flux", 0);
        }

        @Override protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.store("essentia", AspectList.CODEC, contents);
            output.putFloat("flux", flux);
        }
    }
}
