package tx;

import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;

@EventBusSubscriber(modid = Exploration.ID)
final class ExplorationResearchLayout {
    private record Link(String original, int column, int row) {
        String key() { return "TX" + original; }
    }

    private record Layout(int column, int row, int complexity, AspectList aspects,
        String parents, String hiddenParents, boolean hidden, boolean secondary) {}

    private static final List<Link> LINKS = List.of(
        new Link("BASICARTIFACE", 3, 1), new Link("TALLOW", -6, -11), new Link("INFUSION", 2, -7),
        new Link("HOVERHARNESS", 2, -12), new Link("DISTILESSENTIA", 7, -6), new Link("BOOTSTRAVELLER", 1, -4),
        new Link("CAP_thaumium", -7, -7), new Link("INFUSIONENCHANTMENT", -5, -6),
        new Link("ROD_greatwood", -3, -2), new Link("ROD_greatwood_staff", -3, 3), new Link("JARVOID", 7, -10)
    );

    static void registerLinks() {
        for (Link link : LINKS) {
            var original = ResearchCategories.getResearch(link.original());
            if (original == null) throw new IllegalStateException("Missing Thaumcraft research " + link.original());
            var research = original.iconItem() == null
                ? new ResearchItem(link.key(), Exploration.ID, new AspectList(), link.column(), link.row(), 1, original.iconTexture())
                : new ResearchItem(link.key(), Exploration.ID, new AspectList(), link.column(), link.row(), 1, original.iconItem());
            research.setStub().setHidden().setParentsHidden(link.original());
            if (link.original().equals("BOOTSTRAVELLER")) research.setParents("TXINFUSION");
            if (original.isSecondary()) research.setSecondary();
            research.setPages(original.pages().toArray(ResearchPage[]::new)).registerResearchItem();
        }
        new ResearchItem("TX.TAINTBASICS", Exploration.ID, new AspectList(), -9, 0, 1, icon("taint_research"))
            .setRound().setStub().setAutoUnlock()
            .setPages(page("TX.TAINTBASICS", 1), page("TX.TAINTBASICS", 2)).registerResearchItem();
    }

    @SubscribeEvent
    static void mirror(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!ResearchManager.isComplete(player, "TX.TAINTBASICS")) ResearchManager.complete(player, "TX.TAINTBASICS", false);
        for (Link link : LINKS) {
            if (ResearchManager.isComplete(player, link.original()) && !ResearchManager.isComplete(player, link.key())) {
                ResearchManager.complete(player, link.key(), false);
            }
        }
    }

    static ResearchPage page(String key, int page) {
        return ResearchPage.text("tc.research_page." + key + "." + page);
    }

    private static ResourceLocation icon(String file) {
        return ResourceLocation.fromNamespaceAndPath(Exploration.ID, "textures/tabs/" + file + ".png");
    }

    static void add(String key, String item, ResearchPage... pages) {
        Layout layout = layout(key);
        ResourceLocation custom = switch (key) {
            case "TX.CHEST" -> icon("chest_seals");
            case "TX.JAR" -> icon("jar_seals");
            case "TX.ENCHANT.binding" -> icon("binding");
            case "TX.ENCHANT.night_vision" -> icon("night_vision");
            case "TX.ENCHANT.disarming" -> icon("disarm");
            default -> null;
        };
        ResearchItem research = custom == null
            ? new ResearchItem(key, Exploration.ID, layout.aspects(), layout.column(), layout.row(), layout.complexity(), TcStack.of(item))
            : new ResearchItem(key, Exploration.ID, layout.aspects(), layout.column(), layout.row(), layout.complexity(), custom);
        var hiddenParents = new LinkedHashSet<>(split(layout.hiddenParents()));
        for (ResearchPage page : pages) {
            if (page instanceof ResearchPage.Infusion && !split(layout.parents()).contains("INFUSION")) hiddenParents.add("INFUSION");
        }
        research.setParents(split(layout.parents()).toArray(String[]::new))
            .setParentsHidden(hiddenParents.toArray(String[]::new)).setPages(pages);
        if (layout.hidden()) research.setHidden();
        else if (!key.equals("TX.SOULBRAZIER")) research.setConcealed();
        if (layout.secondary()) research.setSecondary();
        switch (key) {
            case "TX.FLESHCURE" -> research.setItemTriggers("minecraft:rotten_flesh");
            case "TX.DREAMCATCHER" -> research.setAspectTriggers(Aspect.TAINT);
            case "TX.TENTACLE" -> research.setEntityTriggers("thaumcraft:taintacle");
            default -> {}
        }
        research.registerResearchItem();
    }

    private static List<String> split(String text) {
        return text.isEmpty() ? List.of() : List.of(text.split(" "));
    }

    private static Layout layout(String key) {
        return switch (key) {
            case "TX.RINGS" -> new Layout(1, 3, 1, new AspectList().add(Aspect.MAGIC, 5).add(Aspect.CRYSTAL, 2),
                "BASICARTIFACE TXBASICARTIFACE", "", false, true);
            case "TX.FLESHCURE" -> new Layout(-4, -10, 1, new AspectList().add(Aspect.ARMOR, 1).add(Aspect.FLESH, 3).add(Aspect.EXCHANGE, 2),
                "TXTALLOW", "TALLOW", true, true);
            case "TX.SOULBRAZIER" -> new Layout(-6, -10, 1, new AspectList().add(Aspect.AURA, 3).add(Aspect.ELDRITCH, 3),
                "", "", false, false);
            case "TX.FLOATCANDLE" -> new Layout(-7, -9, 1, new AspectList().add(Aspect.MAGIC, 1).add(Aspect.FLESH, 2).add(Aspect.AIR, 3),
                "TXTALLOW TALLOW", "", false, true);
            case "TX.BRAINCURE" -> new Layout(-2, -8, 1, new AspectList().add(Aspect.MIND, 4).add(Aspect.ORDER, 5).add(Aspect.MAN, 3).add(Aspect.UNDEAD, 3),
                "JARBRAIN TX.FLESHCURE TXINFUSION", "INFUSION", false, true);
            case "TX.THINKTANK" -> new Layout(-1, -5, 3, new AspectList().add(Aspect.UNDEAD, 4).add(Aspect.MIND, 8).add(Aspect.SENSES, 6).add(Aspect.GREED, 2),
                "TX.BRAINCURE RESEARCHER2", "INFUSION", false, false);
            case "TX.URN" -> new Layout(3, -10, 1, new AspectList().add(Aspect.MAGIC, 4).add(Aspect.VOID, 2).add(Aspect.WATER, 5),
                "INFUSION TXINFUSION", "ARCANEEAR", false, false);
            case "TX.EVERBURN" -> new Layout(4, -11, 1, new AspectList().add(Aspect.MAGIC, 4).add(Aspect.VOID, 2).add(Aspect.FIRE, 5),
                "TX.URN", "ARCANEEAR", false, false);
            case "TX.BELT" -> new Layout(1, -10, 1, new AspectList().add(Aspect.ORDER, 4).add(Aspect.EARTH, 4).add(Aspect.TRAVEL, 2),
                "INFUSION TXINFUSION HOVERHARNESS TXHOVERHARNESS", "", false, false);
            case "TX.REPLICATOR" -> new Layout(4, -5, 3, new AspectList().add(Aspect.CRAFT, 10).add(Aspect.MECHANISM, 10).add(Aspect.ORDER, 6),
                "DISTILESSENTIA TXINFUSION TXDISTILESSENTIA", "INFUSION", false, false);
            case "TX.CRUCSOULS" -> new Layout(5, -7, 3, new AspectList().add(Aspect.DEATH, 7).add(Aspect.HUNGER, 7).add(Aspect.SOUL, 8),
                "DISTILESSENTIA TXINFUSION TXDISTILESSENTIA", "TX.BRAINCURE INFUSION", false, false);
            case "TX.TALISMANFOOD" -> new Layout(-1, -11, 2, new AspectList().add(Aspect.HUNGER, 5).add(Aspect.FLESH, 4).add(Aspect.CROP, 4),
                "TXINFUSION TX.FLESHCURE", "INFUSION", false, false);
            case "TX.METEOR" -> new Layout(2, -1, 2, new AspectList().add(Aspect.FIRE, 5).add(Aspect.ENERGY, 5).add(Aspect.TRAVEL, 10).add(Aspect.FLIGHT, 5),
                "TXBOOTSTRAVELLER BOOTSTRAVELLER FOCUSFIRE", "INFUSION", false, false);
            case "TX.COMET" -> new Layout(5, -3, 2, new AspectList().add(Aspect.WATER, 5).add(Aspect.COLD, 5).add(Aspect.TRAVEL, 10).add(Aspect.MOTION, 5),
                "TXBOOTSTRAVELLER BOOTSTRAVELLER FOCUSFROST", "INFUSION", false, false);
            case "CAP_sojourner" -> new Layout(-5, -8, 2, new AspectList().add(Aspect.EXCHANGE, 5).add(Aspect.ENERGY, 5).add(Aspect.AURA, 8).add(Aspect.GREED, 5).add(Aspect.TOOL, 3),
                "TXCAP_thaumium CAP_thaumium", "WANDPED", false, false);
            case "CAP_mechanist" -> new Layout(-9, -6, 2, new AspectList().add(Aspect.MECHANISM, 5).add(Aspect.ENERGY, 5).add(Aspect.AURA, 8).add(Aspect.GREED, 5).add(Aspect.TOOL, 3),
                "TXCAP_thaumium CAP_thaumium", "NODETAPPER2", false, false);
            case "TX.DREAMCATCHER" -> new Layout(-7, 2, 2, new AspectList().add(Aspect.MIND, 5).add(Aspect.TOOL, 5).add(Aspect.TAINT, 6),
                "TX.TAINTBASICS", "", true, false);
            case "TX.TENTACLE" -> new Layout(-11, 2, 2, new AspectList().add(Aspect.TAINT, 8).add(Aspect.WEAPON, 4).add(Aspect.ARMOR, 4),
                "TX.TAINTBASICS", "", true, false);
            case "TX.ENCHANT.binding" -> new Layout(-7, -4, 1, new AspectList().add(Aspect.TRAP, 6).add(Aspect.ENTROPY, 5).add(Aspect.TRAVEL, 3),
                "TXINFUSIONENCHANTMENT INFUSIONENCHANTMENT", "", false, true);
            case "TX.ENCHANT.night_vision" -> new Layout(-5, -4, 1, new AspectList().add(Aspect.SENSES, 6).add(Aspect.DARKNESS, 2).add(Aspect.LIGHT, 6),
                "TXINFUSIONENCHANTMENT INFUSIONENCHANTMENT", "", false, true);
            case "TX.ENCHANT.disarming" -> new Layout(-3, -4, 1, new AspectList().add(Aspect.SLIME, 6).add(Aspect.TRAP, 4).add(Aspect.WEAPON, 4),
                "TXINFUSIONENCHANTMENT INFUSIONENCHANTMENT", "", false, true);
            case "ROD_amber" -> new Layout(-5, -1, 2, new AspectList().add(Aspect.TOOL, 5).add(Aspect.AURA, 8).add(Aspect.TRAP, 5).add(Aspect.MAGIC, 8),
                "TXROD_greatwood ROD_obsidian ROD_reed ROD_blaze ROD_ice ROD_quartz ROD_bone", "", false, false);
            case "ROD_transmutation" -> new Layout(-1, -1, 2, new AspectList().add(Aspect.TOOL, 8).add(Aspect.MAGIC, 8).add(Aspect.EXCHANGE, 12),
                "TXROD_greatwood ROD_greatwood", "", false, false);
            case "ROD_amber_staff" -> new Layout(-6, 0, 2, new AspectList().add(Aspect.TOOL, 3).add(Aspect.AURA, 4).add(Aspect.TRAP, 3).add(Aspect.MAGIC, 4),
                "TXROD_greatwood_staff ROD_amber", "ROD_greatwood_staff", false, true);
            case "ROD_transmutation_staff" -> new Layout(0, 0, 2, new AspectList().add(Aspect.TOOL, 4).add(Aspect.MAGIC, 4).add(Aspect.EXCHANGE, 6),
                "TXROD_greatwood_staff ROD_transmutation", "ROD_greatwood_staff", false, true);
            case "ROD_necromancer_staff" -> new Layout(-3, 1, 3, new AspectList().add(Aspect.TOOL, 5).add(Aspect.ENTROPY, 5).add(Aspect.DEATH, 8).add(Aspect.AURA, 4).add(Aspect.SOUL, 5),
                "ROD_greatwood_staff TXROD_greatwood_staff", "", false, false);
            case "TX.CHEST" -> new Layout(-9, -3, 2, new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.TRAP, 8).add(Aspect.TRAVEL, 5).add(Aspect.VOID, 3),
                "", "MIRROR TALLOW", false, false);
            case "TX.JAR" -> new Layout(-7, -2, 1, new AspectList().add(Aspect.ELDRITCH, 5).add(Aspect.TRAP, 8).add(Aspect.MAGIC, 5).add(Aspect.TRAVEL, 5),
                "", "", false, true);
            case "TX.TRASHJAR" -> new Layout(5, -9, 2, new AspectList().add(Aspect.ENTROPY, 8).add(Aspect.VOID, 8).add(Aspect.HUNGER, 5).add(Aspect.ELDRITCH, 3),
                "TXJARVOID JARVOID INFUSION TXINFUSION", "", false, false);
            case "TX.SORTER" -> new Layout(-3, 5, 2, new AspectList().add(Aspect.ORDER, 5).add(Aspect.EXCHANGE, 5),
                "TX.CHEST", "", false, false);
            case "TX.TAINTCHARM" -> new Layout(8, 3, 2, new AspectList().add(Aspect.ORDER, 5).add(Aspect.EXCHANGE, 5),
                "TX.DREAMCATCHER", "", false, false);
            default -> throw new IllegalArgumentException("Unknown Thaumic Exploration research " + key);
        };
    }
}
