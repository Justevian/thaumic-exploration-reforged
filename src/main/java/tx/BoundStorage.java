package tx;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

final class BoundStorage extends SavedData {
    private static final Codec<BoundStorage> CODEC = Codec.unboundedMap(Codec.STRING, ItemContainerContents.CODEC)
        .xmap(BoundStorage::new, BoundStorage::snapshot);
    private static final SavedDataType<BoundStorage> TYPE = new SavedDataType<>(
        Identifier.parse("thaumicexploration:bound_storage"), () -> new BoundStorage(Map.of()), CODEC);
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
        BoundStorage storage = level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
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
