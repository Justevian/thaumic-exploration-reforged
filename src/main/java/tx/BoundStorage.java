package tx;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.HolderLookup;

final class BoundStorage extends SavedData {
    private static final Codec<BoundStorage> CODEC = Codec.unboundedMap(Codec.STRING, ItemContainerContents.CODEC)
        .xmap(BoundStorage::new, BoundStorage::snapshot);
    private static final SavedData.Factory<BoundStorage> TYPE = new SavedData.Factory<>(
        () -> new BoundStorage(Map.of()), (tag, registries) -> CODEC.parse(
            registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("channels")).getOrThrow(), null);

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("channels", CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this).getOrThrow());
        return tag;
    }

    private final Map<String, SimpleContainer> channels = new HashMap<>();

    private BoundStorage(Map<String, ItemContainerContents> saved) {
        saved.forEach((key, contents) -> {
            SimpleContainer inventory = newInventory();
            contents.copyInto(inventory.getItems());
            channels.put(key, inventory);
        });
    }

    private Map<String, ItemContainerContents> snapshot() {
        Map<String, ItemContainerContents> result = new HashMap<>();
        channels.forEach((key, inventory) -> {
            if (!inventory.isEmpty()) result.put(key, ItemContainerContents.fromItems(inventory.getItems()));
        });
        return result;
    }

    static SimpleContainer inventory(ServerLevel level, String channel) {
        return inventory(level.getServer().overworld().getDataStorage(), channel);
    }

    static SimpleContainer inventory(DimensionDataStorage data, String channel) {
        BoundStorage storage = data.computeIfAbsent(TYPE, "thaumicexploration_bound_storage");
        return storage.channels.computeIfAbsent(channel, key -> storage.newInventory());
    }

    private SimpleContainer newInventory() {
        return new SimpleContainer(27) {
            @Override
            public void setChanged() {
                super.setChanged();
                BoundStorage.this.setDirty();
            }
        };
    }
}
