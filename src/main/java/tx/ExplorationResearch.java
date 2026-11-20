package tx;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.CompoundRecipe;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import net.minecraft.resources.ResourceLocation;

final class ExplorationResearch {
    private static final String PREFIX = Exploration.ID + ":";


    private ExplorationResearch() {}

    static void register() {
        ResearchCategories.registerCategory(Exploration.ID,
            ResourceLocation.parse(PREFIX + "textures/tabs/tx_tab.png"),
            ResourceLocation.parse("thaumcraft:textures/gui/gui_researchback.png"));
        ExplorationResearchLayout.registerLinks();
        String blank = PREFIX + "blank_seal";
        var clay = TcIngredient.item("minecraft:clay_ball");
        var sealGrid = List.of(TcIngredient.EMPTY, clay, TcIngredient.EMPTY, clay, TcIngredient.item("minecraft:ink_sac"),
            clay, TcIngredient.EMPTY, clay, TcIngredient.EMPTY);
        TcRecipes.register(new ShapedArcaneRecipe(blank, "TX.CHEST", TcStack.of(blank),
            new AspectList().add(Aspect.ORDER, 5), 3, 3, sealGrid));
        TcRecipes.register(new ShapedArcaneRecipe(blank + "_jar", "TX.JAR", TcStack.of(blank),
            new AspectList().add(Aspect.ORDER, 5), 3, 3, sealGrid));
        seal("TX.CHEST", "chest_binding_seal", Aspect.VOID, true);
        seal("TX.JAR", "jar_binding_seal", Aspect.CRYSTAL, false);
        String urn = PREFIX + "everfull_urn";
        var bucket = TcIngredient.item("minecraft:water_bucket");
        var brick = TcIngredient.item("minecraft:brick");
        TcRecipes.register(new InfusionRecipe(urn, "TX.URN", TcStack.of(urn), 2,
            new AspectList().add(Aspect.WATER, 20).add(Aspect.VOID, 8).add(Aspect.MAGIC, 4),
            TcIngredient.item("minecraft:flower_pot"), List.of(bucket, brick, bucket, brick, bucket, brick)));
        add("TX.URN", "everfull_urn", ResearchPage.infusion(urn));
        String fireUrn = PREFIX + "everburn_urn";
        var magma = TcIngredient.item("minecraft:magma_block");
        var lava = TcIngredient.item("minecraft:lava_bucket");
        TcRecipes.register(new InfusionRecipe(fireUrn, "TX.EVERBURN", TcStack.of(fireUrn), 4,
            new AspectList().add(Aspect.FIRE, 24).add(Aspect.ENERGY, 16).add(Aspect.VOID, 8),
            TcIngredient.item(urn), List.of(lava, magma, lava, magma)));
        add("TX.EVERBURN", "everburn_urn", ResearchPage.infusion(fireUrn));
        String souls = PREFIX + "soul_crucible";
        TcRecipes.register(new InfusionRecipe(souls, "TX.CRUCSOULS", TcStack.of(souls), 5,
            new AspectList().add(Aspect.DEATH, 40).add(Aspect.UNDEAD, 10).add(Aspect.HUNGER, 20)
                .add(Aspect.TRAP, 20).add(Aspect.WEAPON, 5).add(Aspect.SOUL, 30),
            TcIngredient.item("thaumcraft:arcane_pedestal"), List.of(TcIngredient.item("thaumcraft:crucible"),
                TcIngredient.item("minecraft:rotten_flesh"), TcIngredient.item("minecraft:bone"),
                TcIngredient.item("minecraft:ghast_tear"), TcIngredient.item("minecraft:soul_sand"),
                TcIngredient.item("thaumcraft:alembic"))));
        add("TX.CRUCSOULS", "soul_crucible", ExplorationResearchLayout.page("TX.CRUCSOULS", 2), ResearchPage.infusion(souls));
        registerEnchantments();
        registerCandles();
        registerInfusions();
        String replicator = PREFIX + "replicator";
        var gold = TcIngredient.item("minecraft:gold_ingot");
        var thaumium = TcIngredient.item("thaumcraft:thaumium_ingot");
        TcRecipes.register(new InfusionRecipe(replicator, "TX.REPLICATOR", TcStack.of(replicator), 9,
            new AspectList().add(Aspect.CRAFT, 50).add(Aspect.MECHANISM, 30).add(Aspect.TOOL, 30).add(Aspect.ORDER, 20),
            TcIngredient.item("thaumcraft:runic_matrix"), List.of(TcIngredient.item("thaumcraft:arcane_worktable"),
                gold, thaumium, gold, thaumium, gold, thaumium)));
        add("TX.REPLICATOR", "replicator", ExplorationResearchLayout.page("TX.REPLICATOR", 2), ResearchPage.infusion(replicator));
        registerFleshCure();
        registerGear();
        registerRods();
        registerAccessoriesAndResearch();
        registerTaint();
        String brazier = PREFIX + "soul_brazier";
        var seed = TcIngredient.item("thaumcraft:void_seed");
        TcRecipes.register(new InfusionRecipe(brazier, "TX.SOULBRAZIER", TcStack.of(brazier), 7,
            new AspectList().add(Aspect.DEATH, 32).add(Aspect.DARKNESS, 32).add(Aspect.AURA, 32),
            TcIngredient.item("minecraft:cauldron"), List.of(TcIngredient.item("thaumcraft:zombie_brain"), seed, seed)));
        add("TX.SOULBRAZIER", "soul_brazier", ResearchPage.infusion(brazier));
        String sorter = PREFIX + "auto_sorter";
        TcRecipes.register(new InfusionRecipe(sorter, "TX.SORTER", TcStack.of(sorter), 4,
            new AspectList().add(Aspect.ORDER, 16).add(Aspect.MECHANISM, 16).add(Aspect.MIND, 8),
            TcIngredient.item("minecraft:hopper"), List.of(TcIngredient.item("thaumcraft:vis_filter"),
                TcIngredient.item("thaumcraft:zombie_brain"), TcIngredient.item("minecraft:comparator"))));
        add("TX.SORTER", "auto_sorter", ResearchPage.infusion(sorter));
    }

    private static void registerEnchantments() {
        enchantment("binding", new AspectList().add(Aspect.TRAP, 8).add(Aspect.ENTROPY, 4)
            .add(Aspect.TRAVEL, 4), "minecraft:soul_sand", 3);
        enchantment("disarming", new AspectList().add(Aspect.WEAPON, 4).add(Aspect.SLIME, 8)
            .add(Aspect.TRAP, 4), "minecraft:slime_ball", 5);
        enchantment("night_vision", new AspectList().add(Aspect.SENSES, 16).add(Aspect.DARKNESS, 8)
            .add(Aspect.LIGHT, 16), "minecraft:golden_carrot", 5);
    }

    private static void enchantment(String name, AspectList cost, String component, int instability) {
        String id = PREFIX + name;
        String research = "TX.ENCHANT." + name;
        TcRecipes.register(new thaumcraft.crafting.InfusionEnchantmentRecipe(id, research, ResourceLocation.parse(id),
            instability, cost, List.of(TcIngredient.item("thaumcraft:vis_filter"), TcIngredient.item(component))));
        add(research, null, ResearchPage.infusionEnchantment(id));
    }

    private static void registerTaint() {
        String ring = PREFIX + "tentacle_ring";
        var taintedGoo = TcIngredient.item("thaumcraft:tainted_goo");
        var taintTendril = TcIngredient.item("thaumcraft:taint_tendril");
        TcRecipes.register(new InfusionRecipe(ring, "TX.TENTACLE", TcStack.of(ring), 4,
            new AspectList().add(Aspect.TAINT, 32).add(Aspect.WEAPON, 8).add(Aspect.ARMOR, 16).add(Aspect.BEAST, 8),
            TcIngredient.item("thaumcraft:mundane_ring"), List.of(taintedGoo, taintTendril, taintedGoo, taintTendril)));
        add("TX.TENTACLE", "tentacle_ring", ResearchPage.infusion(ring));
        String dream = PREFIX + "dreamcatcher";
        String charm = PREFIX + "taint_charm";
        String core = PREFIX + "necromancer_staff_core";
        var goo = TcIngredient.item("thaumcraft:tainted_goo");
        var plank = TcIngredient.item("thaumcraft:greatwood_planks");
        var tendril = TcIngredient.item("thaumcraft:taint_tendril");
        for (String material : List.of("minecraft:feather", "thaumcraft:taint_tendril")) {
            var feather = TcIngredient.item(material);
            String key = material.startsWith("minecraft:") ? dream : dream + "_tendril";
            TcRecipes.register(new ShapedArcaneRecipe(key, "TX.DREAMCATCHER", TcStack.of(dream),
                new AspectList().add(Aspect.ORDER, 15).add(Aspect.ENTROPY, 15), 3, 3,
                List.of(goo, plank, goo, plank, tendril, plank, feather, plank, feather)));
        }
        add("TX.DREAMCATCHER", "dreamcatcher", ResearchPage.arcane(dream));
        TcRecipes.register(new CrucibleRecipe(charm, "TX.TAINTCHARM", TcStack.of(charm), TcIngredient.item(dream),
            new AspectList().add(Aspect.TAINT, 16).add(Aspect.LIFE, 8)));
        add("TX.TAINTCHARM", "taint_charm", ResearchPage.crucible(charm));

        var flesh = TcIngredient.item("minecraft:rotten_flesh");
        var bone = TcIngredient.item("minecraft:bone");
        TcRecipes.register(new InfusionRecipe(core, "ROD_necromancer_staff", TcStack.of(core), 6,
            new AspectList().add(Aspect.DEATH, 32).add(Aspect.ENTROPY, 16).add(Aspect.SOUL, 16).add(Aspect.AURA, 8),
            TcIngredient.item("thaumcraft:bone_staff_core"), List.of(TcIngredient.item("minecraft:wither_skeleton_skull"),
                flesh, bone, TcIngredient.item(PREFIX + "pure_zombie_brain"), bone, flesh)));
        add("ROD_necromancer_staff", "necromancer_staff_core", ResearchPage.infusion(core));
    }

    private static void registerAccessoriesAndResearch() {
        String brain = PREFIX + "pure_zombie_brain";
        String tank = PREFIX + "think_tank";
        String belt = PREFIX + "stabilizer_belt";
        var pureBrain = TcIngredient.item(brain);
        var water = TcIngredient.item("minecraft:water_bucket");
        String potionKey = PREFIX + "weakness";
        TcStack.registerDataHandler(potionKey, (stack, ignored) -> stack.set(
            net.minecraft.core.component.DataComponents.POTION_CONTENTS,
            new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WEAKNESS)));
        TcStack.registerDataMatcher(potionKey, (stack, ignored) -> {
            var potion = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY);
            return potion.is(net.minecraft.world.item.alchemy.Potions.WEAKNESS)
                || potion.is(net.minecraft.world.item.alchemy.Potions.LONG_WEAKNESS);
        });
        var potion = java.util.Map.of(potionKey, "true");
        TcRecipes.register(new InfusionRecipe(brain, "TX.BRAINCURE", TcStack.of(brain), 3,
            new AspectList().add(Aspect.MAN, 4).add(Aspect.MIND, 6).add(Aspect.HEAL, 2),
            TcIngredient.item("thaumcraft:zombie_brain"), List.of(
                TcIngredient.anyOf(TcIngredient.of(TcStack.withData("minecraft:potion", 1, potion)),
                    TcIngredient.of(TcStack.withData("minecraft:splash_potion", 1, potion))),
                TcIngredient.item("minecraft:golden_apple"), water)));
        TcRecipes.register(new InfusionRecipe(tank, "TX.THINKTANK", TcStack.of(tank), 6,
            new AspectList().add(Aspect.MIND, 40).add(Aspect.SENSES, 20).add(Aspect.UNDEAD, 30),
            TcIngredient.item("thaumcraft:warded_jar"), List.of(pureBrain, TcIngredient.item("thaumcraft:scribing_tools"),
                pureBrain, TcIngredient.item("thaumcraft:thaumonomicon"), pureBrain, TcIngredient.item("thaumcraft:thaumometer"))));
        var iron = TcIngredient.item("minecraft:iron_ingot");
        var gold = TcIngredient.item("minecraft:gold_ingot");
        TcRecipes.register(new InfusionRecipe(belt, "TX.BELT", TcStack.of(belt), 3,
            new AspectList().add(Aspect.ORDER, 12).add(Aspect.EARTH, 12).add(Aspect.ARMOR, 4).add(Aspect.TRAVEL, 8),
            TcIngredient.item("thaumcraft:mundane_belt"), List.of(iron, iron, gold, gold,
                TcIngredient.item("thaumcraft:order_shard"), TcIngredient.item("thaumcraft:earth_shard"))));
        add("TX.BRAINCURE", "pure_zombie_brain", ResearchPage.infusion(brain));
        var tankLayout = new ArrayList<>(Collections.nCopies(27, TcStack.NONE));
        tankLayout.set(13, TcStack.of(tank));
        tankLayout.set(22, TcStack.of("minecraft:bookshelf"));
        String tankDiagram = PREFIX + "build_think_tank";
        TcRecipes.register(new CompoundRecipe(tankDiagram, "TX.THINKTANK", new AspectList(), 3, 3, 3, tankLayout));
        add("TX.THINKTANK", "think_tank", ResearchPage.infusion(tank),
            ExplorationResearchLayout.page("TX.THINKTANK", 2), ResearchPage.compound(tankDiagram));
        add("TX.BELT", "stabilizer_belt", ResearchPage.infusion(belt));

        String rings = "TX.RINGS";
        var pages = new java.util.ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + rings + ".1"));
        var empty = TcIngredient.EMPTY;
        for (int i = 0; i < ExplorationAccessories.RINGS.length; i++) {
            String element = ExplorationAccessories.RINGS[i];
            String id = PREFIX + element + "_discount_ring";
            var shard = TcIngredient.item("thaumcraft:" + element + "_shard");
            TcRecipes.register(new ShapedArcaneRecipe(id, rings, TcStack.of(id),
                new AspectList().add(ExplorationAccessories.ASPECTS[i], 15), 3, 3,
                List.of(empty, shard, empty, shard, empty, shard, empty, shard, empty)));
            pages.add(ResearchPage.arcane(id));
        }
        ExplorationResearchLayout.add(rings, PREFIX + "water_discount_ring", pages.toArray(ResearchPage[]::new));
    }

    private static void registerRods() {
        String amber = PREFIX + "amber_rod";
        String staff = PREFIX + "amber_staff_core";
        var filter = TcIngredient.item("thaumcraft:vis_filter");
        TcRecipes.register(new InfusionRecipe(amber, "ROD_amber", TcStack.of(amber), 5,
            new AspectList().add(Aspect.MAGIC, 14).add(Aspect.AURA, 4).add(Aspect.TRAP, 6),
            TcIngredient.item("thaumcraft:amber_block"), List.of(filter, filter)));
        var rod = TcIngredient.item(amber);
        var charm = TcIngredient.item("thaumcraft:primal_charm");
        var empty = TcIngredient.EMPTY;
        TcRecipes.register(new ShapedArcaneRecipe(staff, "ROD_amber_staff", TcStack.of(staff),
            new AspectList().add(Aspect.ORDER, 18), 3, 3,
            List.of(empty, empty, charm, empty, rod, empty, rod, empty, empty)));
        add("ROD_amber", "amber_rod", ResearchPage.infusion(amber));
        add("ROD_amber_staff", "amber_staff_core", ResearchPage.arcane(staff));
        String crystal = PREFIX + "transmutation_rod";
        String crystalStaff = PREFIX + "transmutation_staff_core";
        TcRecipes.register(new InfusionRecipe(crystal, "ROD_transmutation", TcStack.of(crystal), 6,
            new AspectList().add(Aspect.MAGIC, 32).add(Aspect.EXCHANGE, 16).add(Aspect.AURA, 4), charm,
            List.of(TcIngredient.item("thaumcraft:air_shard"), TcIngredient.item("thaumcraft:fire_shard"),
                TcIngredient.item("thaumcraft:water_shard"), TcIngredient.item("thaumcraft:earth_shard"),
                TcIngredient.item("thaumcraft:order_shard"), TcIngredient.item("thaumcraft:entropy_shard"), filter)));
        var crystalRod = TcIngredient.item(crystal);
        TcRecipes.register(new ShapedArcaneRecipe(crystalStaff, "ROD_transmutation_staff", TcStack.of(crystalStaff),
            new AspectList().add(Aspect.ORDER, 18), 3, 3,
            List.of(empty, empty, charm, empty, crystalRod, empty, crystalRod, empty, empty)));
        add("ROD_transmutation", "transmutation_rod", ResearchPage.infusion(crystal));
        add("ROD_transmutation_staff", "transmutation_staff_core", ResearchPage.arcane(crystalStaff));
    }

    private static void registerGear() {
        String meteor = PREFIX + "meteor_boots";
        String comet = PREFIX + "comet_boots";
        var netherrack = TcIngredient.item("minecraft:netherrack");
        var snow = TcIngredient.item("minecraft:snow_block");
        var traveller = TcIngredient.item("thaumcraft:boots_traveller");
        TcRecipes.register(new InfusionRecipe(meteor, "TX.METEOR", TcStack.of(meteor), 4,
            new AspectList().add(Aspect.FIRE, 25).add(Aspect.ENERGY, 25).add(Aspect.TRAVEL, 25).add(Aspect.FLIGHT, 25),
            traveller, List.of(TcIngredient.item("thaumcraft:fire_crystal_cluster"), netherrack, netherrack,
                netherrack, TcIngredient.item("thaumcraft:focus_fire"))));
        TcRecipes.register(new InfusionRecipe(comet, "TX.COMET", TcStack.of(comet), 4,
            new AspectList().add(Aspect.WATER, 25).add(Aspect.COLD, 25).add(Aspect.TRAVEL, 25).add(Aspect.MOTION, 25),
            traveller, List.of(TcIngredient.item("thaumcraft:water_crystal_cluster"), snow, snow, snow,
                TcIngredient.item("thaumcraft:focus_frost"))));
        add("TX.METEOR", "meteor_boots", ResearchPage.infusion(meteor));
        add("TX.COMET", "comet_boots", ResearchPage.infusion(comet));

        var lapis = TcIngredient.item("minecraft:lapis_lazuli");
        var redstone = TcIngredient.item("minecraft:redstone");
        var empty = TcIngredient.EMPTY;
        cap(ExplorationCaps.SOJOURNER, Aspect.AIR, Aspect.EXCHANGE, 2,
            List.of(lapis, TcIngredient.item("minecraft:diamond"), lapis, lapis, empty, lapis));
        cap(ExplorationCaps.MECHANIST, Aspect.FIRE, Aspect.MECHANISM, 3,
            List.of(redstone, redstone, redstone, redstone, TcIngredient.item("minecraft:piston"), redstone,
                empty, TcIngredient.item("minecraft:repeater"), empty));
    }

    private static void cap(String cap, Aspect primal, Aspect specialty, int height, List<TcIngredient> grid) {
        String key = "CAP_" + cap;
        String charged = PREFIX + cap + "_wand_cap";
        String blank = PREFIX + "uncharged_" + cap + "_wand_cap";
        TcRecipes.register(new ShapedArcaneRecipe(blank, key, TcStack.of(blank),
            new AspectList().add(primal, 6).add(Aspect.ENTROPY, 6).add(Aspect.ORDER, 6), 3, height, grid));
        var filter = TcIngredient.item("thaumcraft:vis_filter");
        TcRecipes.register(new InfusionRecipe(charged, key, TcStack.of(charged), 5,
            new AspectList().add(specialty, 16).add(Aspect.MAGIC, 12).add(Aspect.AURA, 6).add(Aspect.ENERGY, 12),
            TcIngredient.item(blank), List.of(filter, filter)));
        add(key, cap + "_wand_cap", ResearchPage.arcane(blank), ResearchPage.infusion(charged));
    }

    private static void registerCandles() {
        String key = "TX.FLOATCANDLE";
        var pages = new java.util.ArrayList<ResearchPage>();
        pages.add(ResearchPage.text("tc.research_page." + key + ".1"));
        for (String color : thaumcraft.registry.ModBlocks.CANDLE_COLORS) {
            String id = PREFIX + color + "_floating_candle";
            var candle = TcIngredient.item("thaumcraft:" + color + "_tallow_candle");
            TcRecipes.register(new ShapelessArcaneRecipe(id, key, TcStack.of(id, 3),
                new AspectList().add(Aspect.AIR, 5),
                List.of(candle, candle, candle, TcIngredient.item("thaumcraft:air_shard"))));
            pages.add(ResearchPage.arcane(id));
        }
        ExplorationResearchLayout.add(key, PREFIX + "white_floating_candle", pages.toArray(ResearchPage[]::new));
    }

    private static void registerInfusions() {
        String trash = PREFIX + "trash_jar";
        var tile = TcIngredient.item("thaumcraft:obsidian_tile");
        var shard = TcIngredient.item("thaumcraft:entropy_shard");
        TcRecipes.register(new InfusionRecipe(trash, "TX.TRASHJAR", TcStack.of(trash), 7,
            new AspectList().add(Aspect.ENTROPY, 16).add(Aspect.VOID, 12).add(Aspect.HUNGER, 12)
                .add(Aspect.ELDRITCH, 12),
            TcIngredient.item("thaumcraft:void_jar"),
            List.of(TcIngredient.item("thaumcraft:hungry_chest"), tile, shard, tile, shard, tile, shard, tile)));
        add("TX.TRASHJAR", "trash_jar", ExplorationResearchLayout.page("TX.TRASHJAR", 2), ResearchPage.infusion(trash));

        String food = PREFIX + "food_talisman";
        TcRecipes.register(new InfusionRecipe(food, "TX.TALISMANFOOD", TcStack.of(food), 5,
            new AspectList().add(Aspect.HUNGER, 30).add(Aspect.FLESH, 25).add(Aspect.CROP, 25)
                .add(Aspect.EXCHANGE, 10),
            TcIngredient.item("thaumcraft:primal_charm"),
            List.of(TcIngredient.item("minecraft:obsidian"), TcIngredient.item("minecraft:cooked_beef"),
                TcIngredient.item("minecraft:cooked_chicken"), TcIngredient.item("minecraft:cooked_porkchop"),
                TcIngredient.item("minecraft:cooked_cod"), TcIngredient.item("minecraft:bread"))));
        add("TX.TALISMANFOOD", "food_talisman", ResearchPage.infusion(food));
    }

    private static void registerFleshCure() {
        String recipe = PREFIX + "flesh_cure";
        TcRecipes.register(new CrucibleRecipe(recipe, "TX.FLESHCURE", TcStack.of("minecraft:leather", 2),
            TcIngredient.item("minecraft:rotten_flesh"), new AspectList().add(Aspect.FLESH, 2).add(Aspect.CLOTH, 1)));
        ExplorationResearchLayout.add("TX.FLESHCURE", "minecraft:rotten_flesh",
            ExplorationResearchLayout.page("TX.FLESHCURE", 1), ResearchPage.crucible(recipe));
    }

    private static void seal(String research, String item, Aspect component, boolean chest) {
        String id = PREFIX + item;
        TcRecipes.register(new CrucibleRecipe(id, research, TcStack.of(id), TcIngredient.item(PREFIX + "blank_seal"),
            new AspectList().add(Aspect.ELDRITCH, 6).add(Aspect.EXCHANGE, 4).add(component, 4)
                .add(Aspect.TRAP, 4).add(Aspect.TRAVEL, 6)));
        if (chest) add(research, item, ResearchPage.arcane(PREFIX + "blank_seal"), ResearchPage.crucible(id));
        else add(research, item, ResearchPage.arcane(PREFIX + "blank_seal_jar"), ResearchPage.crucible(id));
    }

    private static void add(String key, String item, ResearchPage... recipePages) {
        var pages = new ArrayList<ResearchPage>();
        pages.add(ExplorationResearchLayout.page(key, 1));
        Collections.addAll(pages, recipePages);
        ExplorationResearchLayout.add(key, item == null ? null : PREFIX + item, pages.toArray(ResearchPage[]::new));
    }
}
