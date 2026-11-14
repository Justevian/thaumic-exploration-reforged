package tx;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

@Mod(Exploration.ID)
public final class Exploration {
    static final String ID = "thaumicexploration";
    private static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, ID);
    static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect>
        BINDING_EFFECT = EFFECTS.register("binding", () -> new ExplorationEnchantments.Status(false));
    static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect>
        WITHDRAWAL = EFFECTS.register("taint_withdrawal", () -> new ExplorationEnchantments.Status(true));
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    static final net.neoforged.neoforge.registries.DeferredBlock<BoundChest> CHEST = BLOCKS.registerBlock(
        "bound_chest", BoundChest::new, p -> p.strength(2.5F));
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<BoundChest.Tile>> CHEST_TILE =
        TILES.register("bound_chest", () -> new BlockEntityType<>(BoundChest.Tile::new, CHEST.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<CometIce> COMET_ICE =
        BLOCKS.registerBlock("comet_ice", CometIce::new, p -> p.strength(0.5F).friction(0.98F).noOcclusion().randomTicks());
    static final net.neoforged.neoforge.registries.DeferredItem<BoundChest.Seal> LINKED_SEAL =
        ITEMS.registerItem("linked_chest_binding_seal", BoundChest.Seal::new);
    static final net.neoforged.neoforge.registries.DeferredItem<BoundChest.Seal> SEAL =
        ITEMS.registerItem("chest_binding_seal", BoundChest.Seal::new);

    static final net.neoforged.neoforge.registries.DeferredItem<Item> BLANK =
        ITEMS.registerSimpleItem("blank_seal");
    static final net.neoforged.neoforge.registries.DeferredBlock<EverfullUrn> URN =
        BLOCKS.registerBlock("everfull_urn", EverfullUrn::new, p -> p.strength(1.5F).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<EverfullUrn.Tile>> URN_TILE =
        TILES.register("everfull_urn", () -> new BlockEntityType<>(EverfullUrn.Tile::new, URN.get()));
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> URN_ITEM =
        ITEMS.registerSimpleBlockItem(URN);
    static final net.neoforged.neoforge.registries.DeferredBlock<EverburnUrn> EVERBURN =
        BLOCKS.registerBlock("everburn_urn", EverburnUrn::new, p -> p.strength(1.5F).noOcclusion().lightLevel(state -> 7));
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> EVERBURN_ITEM =
        ITEMS.registerSimpleBlockItem(EVERBURN);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<EverburnUrn.Tile>> EVERBURN_TILE =
        TILES.register("everburn_urn", () -> new BlockEntityType<>(EverburnUrn.Tile::new, EVERBURN.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<SoulCrucible> SOUL_CRUCIBLE =
        BLOCKS.registerBlock("soul_crucible", SoulCrucible::new, p -> p.strength(2).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SOUL_CRUCIBLE_ITEM =
        ITEMS.registerSimpleBlockItem(SOUL_CRUCIBLE);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulCrucible.Tile>> SOUL_CRUCIBLE_TILE =
        TILES.register("soul_crucible", () -> new BlockEntityType<>(SoulCrucible.Tile::new, SOUL_CRUCIBLE.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<BoundJar> JAR =
        BLOCKS.registerBlock("bound_jar", BoundJar::new, p -> p.strength(0.3F).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<BoundJar.Tile>> JAR_TILE =
        TILES.register("bound_jar", () -> new BlockEntityType<>(BoundJar.Tile::new, JAR.get()));
    static final net.neoforged.neoforge.registries.DeferredItem<BoundJar.Seal> JAR_SEAL =
        ITEMS.registerItem("jar_binding_seal", BoundJar.Seal::new);
    static final net.neoforged.neoforge.registries.DeferredBlock<Sorter> SORTER =
        BLOCKS.registerBlock("auto_sorter", Sorter::new, p -> p.strength(2.5F));
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> SORTER_ITEM =
        ITEMS.registerSimpleBlockItem(SORTER);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<Sorter.Tile>> SORTER_TILE =
        TILES.register("auto_sorter", () -> new BlockEntityType<>(Sorter.Tile::new, SORTER.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<SoulBrazier> BRAZIER =
        BLOCKS.registerBlock("soul_brazier", SoulBrazier::new, p -> p.strength(2.5F).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> BRAZIER_ITEM =
        ITEMS.registerSimpleBlockItem(BRAZIER);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulBrazier.Tile>> BRAZIER_TILE =
        TILES.register("soul_brazier", () -> new BlockEntityType<>(SoulBrazier.Tile::new, BRAZIER.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<Replicator> REPLICATOR =
        BLOCKS.registerBlock("replicator", Replicator::new, p -> p.strength(2.5F));
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> REPLICATOR_ITEM =
        ITEMS.registerSimpleBlockItem(REPLICATOR);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<Replicator.Tile>> REPLICATOR_TILE =
        TILES.register("replicator", () -> new BlockEntityType<>(Replicator.Tile::new, REPLICATOR.get()));
    static final net.neoforged.neoforge.registries.DeferredBlock<ThinkTank> THINK_TANK =
        BLOCKS.registerBlock("think_tank", ThinkTank::new, p -> p.strength(1.5F).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> THINK_TANK_ITEM =
        ITEMS.registerSimpleBlockItem(THINK_TANK);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<ThinkTank.Tile>> THINK_TANK_TILE =
        TILES.register("think_tank", () -> new BlockEntityType<>(ThinkTank.Tile::new, THINK_TANK.get()));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> PURE_BRAIN = ITEMS.registerSimpleItem("pure_zombie_brain");
    static final net.neoforged.neoforge.registries.DeferredItem<ExplorationAccessories.Belt> STABILIZER =
        ITEMS.registerItem("stabilizer_belt", ExplorationAccessories.Belt::new);
    static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<ExplorationAccessories.Ring>> DISCOUNT_RINGS =
        java.util.stream.IntStream.range(0, ExplorationAccessories.RINGS.length).mapToObj(index ->
            ITEMS.registerItem(ExplorationAccessories.RINGS[index] + "_discount_ring",
                p -> new ExplorationAccessories.Ring(p, index))).toList();
    static final net.neoforged.neoforge.registries.DeferredBlock<TrashJar> TRASH =
        BLOCKS.registerBlock("trash_jar", TrashJar::new, p -> p.strength(0.3F).noOcclusion());
    static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem> TRASH_ITEM =
        ITEMS.registerSimpleBlockItem(TRASH);
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<TrashJar.Tile>> TRASH_TILE =
        TILES.register("trash_jar", () -> new BlockEntityType<>(TrashJar.Tile::new, TRASH.get()));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> METEOR = ITEMS.registerItem(
        "meteor_boots", Item::new, p -> ExplorationBoots.properties(p, "meteor_boots"));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> COMET = ITEMS.registerItem(
        "comet_boots", Item::new, p -> ExplorationBoots.properties(p, "comet_boots"));
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ID);
    static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
        net.minecraft.world.entity.EntityType<TentacleMinion>> MINION =
        ENTITIES.registerEntityType("tentacle_minion", TentacleMinion::new, net.minecraft.world.entity.MobCategory.MISC,
            builder -> builder.sized(0.22F, 1.0F).clientTrackingRange(8));
    static final net.neoforged.neoforge.registries.DeferredItem<ExplorationAccessories.TentacleRing> TENTACLE_RING =
        ITEMS.registerItem("tentacle_ring", ExplorationAccessories.TentacleRing::new);
    static final net.neoforged.neoforge.registries.DeferredItem<Item> TAINT_BERRY =
        ITEMS.registerSimpleItem("taint_berry", p -> thaumcraft.platform.Registration.food(p, 1, 0.3F));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> DREAMCATCHER =
        ITEMS.registerSimpleItem("dreamcatcher", p -> p.stacksTo(1));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> TAINT_CHARM =
        ITEMS.registerSimpleItem("taint_charm", p -> p.stacksTo(1));
    static final net.neoforged.neoforge.registries.DeferredItem<Item> NECROMANCER_STAFF =
        ITEMS.registerSimpleItem("necromancer_staff_core");
    static final net.neoforged.neoforge.registries.DeferredItem<Item> AMBER_ROD = ITEMS.registerSimpleItem("amber_rod");
    static final net.neoforged.neoforge.registries.DeferredItem<Item> AMBER_STAFF = ITEMS.registerSimpleItem("amber_staff_core");
    static final net.neoforged.neoforge.registries.DeferredItem<Item> TRANSMUTATION_ROD =
        ITEMS.registerSimpleItem("transmutation_rod");
    static final net.neoforged.neoforge.registries.DeferredItem<Item> TRANSMUTATION_STAFF =
        ITEMS.registerSimpleItem("transmutation_staff_core");
    static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<Item>> CAPS =
        java.util.List.of("sojourner_wand_cap", "mechanist_wand_cap", "uncharged_sojourner_wand_cap",
            "uncharged_mechanist_wand_cap").stream().map(ITEMS::registerSimpleItem).toList();
    static final net.neoforged.neoforge.registries.DeferredItem<FoodTalisman> FOOD_TALISMAN =
        ITEMS.registerItem("food_talisman", FoodTalisman::new);
    static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem>>
        FLOATING_CANDLES = java.util.Arrays.stream(thaumcraft.registry.ModBlocks.CANDLE_COLORS).map(color ->
            ITEMS.registerSimpleBlockItem(BLOCKS.registerBlock(color + "_floating_candle", FloatingCandle::new,
                p -> p.strength(0.1F).noCollision().noOcclusion().lightLevel(state -> 14)))).toList();
    static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<FloatingCandle.Tile>>
        FLOATING_CANDLE_TILE = TILES.register("floating_candle", () -> new BlockEntityType<>(FloatingCandle.Tile::new,
            FLOATING_CANDLES.stream().map(item -> item.get().getBlock()).toArray(net.minecraft.world.level.block.Block[]::new)));
    static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    static {
        TABS.register("exploration", () -> net.minecraft.world.item.CreativeModeTab.builder()
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.thaumicexploration"))
            .icon(() -> new net.minecraft.world.item.ItemStack(SEAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(BLANK.get());
                output.accept(SEAL.get());
                output.accept(JAR_SEAL.get());
                output.accept(URN_ITEM.get());
                output.accept(EVERBURN_ITEM.get());
                output.accept(SOUL_CRUCIBLE_ITEM.get());
                output.accept(TRASH_ITEM.get());
                output.accept(REPLICATOR_ITEM.get());
                output.accept(SORTER_ITEM.get());
                output.accept(BRAZIER_ITEM.get());
                output.accept(THINK_TANK_ITEM.get());
                output.accept(PURE_BRAIN.get());
                output.accept(STABILIZER.get());
                DISCOUNT_RINGS.forEach(ring -> output.accept(ring.get()));
                output.accept(FOOD_TALISMAN.get());
                output.accept(METEOR.get());
                output.accept(COMET.get());
                output.accept(DREAMCATCHER.get());
                output.accept(TENTACLE_RING.get());
                output.accept(TAINT_BERRY.get());
                output.accept(TAINT_CHARM.get());
                output.accept(NECROMANCER_STAFF.get());
                output.accept(AMBER_ROD.get());
                output.accept(AMBER_STAFF.get());
                output.accept(TRANSMUTATION_ROD.get());
                output.accept(TRANSMUTATION_STAFF.get());
                CAPS.forEach(cap -> output.accept(cap.get()));
                FLOATING_CANDLES.forEach(candle -> output.accept(candle.get()));
            }).build());
    }

    public Exploration(IEventBus bus) {
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        bus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) -> event.register(SoulBrazier.TICKETS));
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
            event.put(MINION.get(), TentacleMinion.createAttributes().build()));
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TILES.register(bus);
        TABS.register(bus);
        bus.addListener(this::capabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> { ExplorationCaps.register(); ExplorationRods.register(); ExplorationResearch.register(); }));
    }

    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, EVERBURN_TILE.get(), (tile, side) ->
            side == net.minecraft.core.Direction.UP ? tile.lava : null);
        event.registerBlock(Capabilities.Fluid.BLOCK, (level, pos, state, tile, side) ->
            side == net.minecraft.core.Direction.UP ? new EverfullUrn.WaterOutput() : null, URN.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, THINK_TANK_TILE.get(), (tile, side) ->
            side == net.minecraft.core.Direction.UP ? VanillaContainerWrapper.of(tile.input)
                : VanillaContainerWrapper.of(tile.output));
        event.registerBlockEntity(Capabilities.Item.BLOCK, CHEST_TILE.get(), (tile, side) ->
            tile.getLevel() instanceof net.minecraft.server.level.ServerLevel && tile.linked()
                ? VanillaContainerWrapper.of(tile.inventory()) : null);
    }
}
