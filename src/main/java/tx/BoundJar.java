package tx;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.HolderLookup;
import thaumcraft.compat.ValueInput;
import thaumcraft.compat.ValueOutput;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.essentia.JarBlock;
import thaumcraft.essentia.JarBlockEntity;
import thaumcraft.registry.ModBlocks;

final class BoundJar extends JarBlock {
    BoundJar(Properties properties) {
        super(false, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == Exploration.JAR_TILE.get()
            ? (world, pos, current, entity) -> JarBlockEntity.serverTick(world, pos, current, (Tile) entity) : null;
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return java.util.List.of(new ItemStack(ModBlocks.WARDED_JAR.get()));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
        net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof Tile tile && tile.getAspectFilter() == null) {
            if (level instanceof ServerLevel server && server.mayInteract(player, pos)) {
                Aspect aspect = tile.getAspect();
                if (aspect != null) tile.takeFromContainer(aspect, tile.getAmount());
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    static final class Tile extends JarBlockEntity {
        private String channel = "";

        Tile(BlockPos pos, BlockState state) {
            super(Exploration.JAR_TILE.get(), pos, state);
        }

        private Storage storage() {
            return level instanceof ServerLevel server && !channel.isEmpty()
                ? server.getServer().overworld().getDataStorage().computeIfAbsent(Storage.TYPE, "thaumicexploration_bound_essentia") : null;
        }

        private void refresh() {
            Storage data = storage();
            if (data == null) return;
            AspectList contents = data.channels.get(channel);
            Aspect aspect = contents == null || contents.isEmpty() ? null : contents.getAspects().getFirst();
            int amount = aspect == null ? 0 : contents.getAmount(aspect);
            if (super.getAspect() != aspect || super.getAmount() != amount) {
                setAspect(aspect);
                setAmount(amount);
                sync();
            }
        }

        private void store() {
            Storage data = storage();
            if (data != null) {
                if (super.getAmount() == 0 || super.getAspect() == null) data.channels.remove(channel);
                else data.channels.put(channel, new AspectList().add(super.getAspect(), super.getAmount()));
                data.setDirty();
            }
        }

        @Override protected void tick() { refresh(); super.tick(); }
        @Override public Aspect getAspect() { refresh(); return super.getAspect(); }
        @Override public int getAmount() { refresh(); return super.getAmount(); }
        @Override public AspectList getAspects() { refresh(); return super.getAspects(); }
        @Override public Aspect getEssentiaType(net.minecraft.core.Direction side) { return getAspect(); }
        @Override public int getEssentiaAmount(net.minecraft.core.Direction side) { return getAmount(); }
        @Override public int addToContainer(Aspect aspect, int amount) {
            refresh();
            if (amount <= 0 || !doesContainerAccept(aspect)) return amount;
            int remaining = super.addToContainer(aspect, amount);
            if (amount != remaining) store();
            return remaining;
        }

        @Override public boolean takeFromContainer(Aspect aspect, int amount) {
            refresh();
            if (amount <= 0 || !super.takeFromContainer(aspect, amount)) return false;
            store();
            return true;
        }

        @Override protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            channel = input.getStringOr("channel", "");
        }

        @Override protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putString("channel", channel);
        }
    }

    private static final class Storage extends SavedData {
        private static final Codec<Storage> CODEC = Codec.unboundedMap(Codec.STRING, AspectList.CODEC)
            .xmap(Storage::new, data -> data.channels);
        private static final SavedData.Factory<Storage> TYPE = new SavedData.Factory<>(
            () -> new Storage(Map.of()), (tag, registries) -> CODEC.parse(
                registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("channels")).getOrThrow(), null);

        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("channels", CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow());
            return tag;
        }
        private final Map<String, AspectList> channels;

        private Storage(Map<String, AspectList> channels) {
            this.channels = new HashMap<>(channels);
        }
    }

    static final class Seal extends Item {
        Seal(Properties properties) { super(properties.stacksTo(1)); }

        @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            var level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            if (!level.getBlockState(pos).is(ModBlocks.WARDED_JAR.get())) return InteractionResult.PASS;
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
            if (!server.mayInteract(player, pos) || !(server.getBlockEntity(pos) instanceof JarBlockEntity original)
                || original.getAmount() != 0 || original.getAspectFilter() != null) return InteractionResult.FAIL;
            var key = net.minecraft.core.component.DataComponents.CUSTOM_DATA;
            var channel = stack.getOrDefault(key, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("channel");
            String link = channel.isEmpty() ? UUID.randomUUID().toString() : channel;
            if (!server.setBlockAndUpdate(pos, Exploration.JAR.get().defaultBlockState())
                || !(server.getBlockEntity(pos) instanceof Tile tile)) return InteractionResult.FAIL;
            tile.channel = link;
            tile.refresh();
            tile.setChanged();
            net.minecraft.world.item.component.CustomData.update(key, stack, tag -> tag.putString("channel", link));
            return InteractionResult.SUCCESS;
        }
    }
}
