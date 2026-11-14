package tx;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.essentia.EssentiaTransport;
import thaumcraft.research.Warp;
import thaumcraft.research.WarpType;
import thaumcraft.taint.TaintBiomes;
import thaumcraft.vis.VisNet;

@EventBusSubscriber(modid = Exploration.ID)
final class SoulBrazier extends Block implements EntityBlock {
    static final net.neoforged.neoforge.common.world.chunk.TicketController TICKETS =
        new net.neoforged.neoforge.common.world.chunk.TicketController(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(Exploration.ID, "soul_brazier"));
    private static final String WARP = "tx_brazier_warp";
    private static final String POS = "tx_brazier_pos";
    private static final String DIMENSION = "tx_brazier_dimension";

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
        net.minecraft.world.phys.shapes.Shapes.or(box(3, 0, 3, 13, 12, 13),
            box(6, 8, 1, 10, 16, 3), box(6, 8, 13, 10, 16, 15),
            box(1, 8, 6, 3, 16, 10), box(13, 8, 6, 15, 16, 10));

    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
        net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    SoulBrazier(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Tile(pos, state); }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.BRAZIER_TILE.get()
            ? (world, pos, current, tile) -> ((Tile) tile).tick((ServerLevel) world) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack item) {
        if (placer != null && level.getBlockEntity(pos) instanceof Tile tile) {
            tile.owner = placer.getUUID();
            tile.setChanged();
        }
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof Tile tile) tile.activate(server);
        return InteractionResult.SUCCESS;
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        forceChunk(level, pos, false);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    private static void forceChunk(ServerLevel level, BlockPos pos, boolean force) {
        TICKETS.forceChunk(level, pos, pos.getX() >> 4, pos.getZ() >> 4, force, true);
    }

    @SubscribeEvent
    static void restoreWarp(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        var data = player.getPersistentData();
        int warp = data.getIntOr(WARP, 0);
        if (warp <= 0) return;
        BlockPos pos = BlockPos.of(data.getLongOr(POS, 0));
        String dimension = data.getStringOr(DIMENSION, "");
        for (var level : player.level().getServer().getAllLevels()) {
            if (!level.dimension().identifier().toString().equals(dimension)) continue;
            level.getChunk(pos);
            if (level.getBlockEntity(pos) instanceof Tile tile && tile.active && player.getUUID().equals(tile.owner)) return;
            break;
        }
        Warp.add(player, warp, WarpType.PERMANENT);
        data.remove(WARP);
        data.remove(POS);
        data.remove(DIMENSION);
    }

    static final class Tile extends BlockEntity implements EssentiaTransport {
        private UUID owner;
        private boolean active;
        private int death;
        private int fire;
        private int ticks;

        Tile(BlockPos pos, BlockState state) { super(Exploration.BRAZIER_TILE.get(), pos, state); }

        void activate(ServerPlayer player) {
            if (owner == null) owner = player.getUUID();
            int warp = Warp.get(player, WarpType.PERMANENT);
            String error = !owner.equals(player.getUUID()) ? "owner" : active || player.getPersistentData().contains(WARP) ? "active"
                : warp <= 0 ? "warp" : death <= 0 || fire <= 0 ? "fuel" : null;
            if (error != null) {
                player.sendSystemMessage(Component.translatable("message.thaumicexploration.brazier_" + error));
                return;
            }
            var data = player.getPersistentData();
            data.putInt(WARP, warp);
            data.putLong(POS, worldPosition.asLong());
            data.putString(DIMENSION, level.dimension().identifier().toString());
            Warp.set(player, 0, WarpType.PERMANENT);
            active = true;
            forceChunk((ServerLevel) level, worldPosition, true);
            setChanged();
        }

        void tick(ServerLevel world) {
            ticks++;
            if (ticks % 5 == 0 && fire < 16) {
                int gained = VisNet.drainVis(world, worldPosition, Aspect.FIRE, 1);
                if (gained > 0) { fire += gained; setChanged(); }
            }
            if (ticks % 5 == 0 && death < 16) {
                var source = thaumcraft.essentia.Essentia.getConnectable(world, worldPosition, Direction.DOWN);
                if (source != null && source.canOutputTo(Direction.UP) && source.getEssentiaType(Direction.UP) == Aspect.DEATH
                    && source.getSuctionAmount(Direction.UP) < 128 && source.getMinimumSuction() <= 128) {
                    addEssentia(Aspect.DEATH, source.takeEssentia(Aspect.DEATH, 1, Direction.UP), Direction.DOWN);
                }
            }
            if (!active) return;
            if (ticks % 50 == 0) {
                var random = world.getRandom();
                var pos = worldPosition.offset(random.nextInt(16) - random.nextInt(16), 0,
                    random.nextInt(16) - random.nextInt(16));
                if (world.hasChunkAt(pos)) TaintBiomes.set(world, pos, TaintBiomes.TAINTED_LAND);
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                    8, 0.17, 0.22, 0.17, 0.02);
            }
            if (ticks % 60 == 0) {
                death = Math.max(0, death - 1);
                fire = Math.max(0, fire - 3);
                if (death == 0 || fire == 0) {
                    active = false;
                    forceChunk(world, worldPosition, false);
                }
                setChanged();
            }
        }

        @Override public boolean isConnectable(Direction side) { return side == Direction.DOWN; }
        @Override public boolean canInputFrom(Direction side) { return isConnectable(side); }
        @Override public boolean canOutputTo(Direction side) { return false; }
        @Override public void setSuction(Aspect aspect, int amount) {}
        @Override public Aspect getSuctionType(Direction side) { return Aspect.DEATH; }
        @Override public int getSuctionAmount(Direction side) { return canInputFrom(side) && death < 16 ? 128 : 0; }
        @Override public int takeEssentia(Aspect aspect, int amount, Direction side) { return 0; }
        @Override public int addEssentia(Aspect aspect, int amount, Direction side) {
            if (aspect != Aspect.DEATH || !canInputFrom(side) || amount <= 0) return 0;
            int accepted = Math.min(amount, 16 - death);
            death += accepted;
            if (accepted > 0) setChanged();
            return accepted;
        }
        @Override public Aspect getEssentiaType(Direction side) { return Aspect.DEATH; }
        @Override public int getEssentiaAmount(Direction side) { return death; }
        @Override public int getMinimumSuction() { return 0; }
        @Override public boolean renderExtendedTube() { return true; }

        @Override protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
            active = input.getBooleanOr("active", false);
            death = input.getIntOr("death", 0);
            fire = input.getIntOr("fire", 0);
        }

        @Override protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (owner != null) output.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
            output.putBoolean("active", active);
            output.putInt("death", death);
            output.putInt("fire", fire);
        }
    }
}
