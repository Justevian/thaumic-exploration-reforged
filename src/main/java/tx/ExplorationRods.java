package tx;

import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumcraft.wand.RodUpdate;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

final class ExplorationRods {
    private ExplorationRods() {}

    static void register() {
        WandRegistry.registerRod("necromancer", 200,
            ResourceLocation.fromNamespaceAndPath(Exploration.ID, "necromancer_staff_core"), 15,
            ExplorationRods::updateNecromancer, false, true, false,
            ResourceLocation.fromNamespaceAndPath(Exploration.ID, "textures/model/necromancer_staff_core.png"));
        RodUpdate recharge = (stack, player) -> {
            if (player.tickCount % 100 == 0) {
                for (Aspect aspect : Aspect.getPrimalAspects()) Wands.addVis(stack, aspect, 1, true);
            }
        };
        var texture = ResourceLocation.fromNamespaceAndPath(Exploration.ID, "textures/model/amber_rod.png");
        WandRegistry.registerRod("amber", 10, ResourceLocation.fromNamespaceAndPath(Exploration.ID, "amber_rod"),
            8, recharge, false, false, false, texture);
        WandRegistry.registerRod("amber", 25, ResourceLocation.fromNamespaceAndPath(Exploration.ID, "amber_staff_core"),
            18, recharge, false, true, false, texture);
        var crystal = ResourceLocation.fromNamespaceAndPath(Exploration.ID, "textures/model/transmutation_rod.png");
        WandRegistry.registerRod("transmutation", 75, ResourceLocation.fromNamespaceAndPath(Exploration.ID, "transmutation_rod"),
            6, (stack, player) -> redistribute(stack), false, false, false, crystal);
        WandRegistry.registerRod("transmutation", 175, ResourceLocation.fromNamespaceAndPath(Exploration.ID, "transmutation_staff_core"),
            14, (stack, player) -> redistribute(stack), false, true, false, crystal);
    }

    private static void updateNecromancer(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var key = net.minecraft.core.component.DataComponents.CUSTOM_DATA;
        var data = stack.getOrDefault(key, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        boolean initialized = PortNbt.getBooleanOr(data, "necromancer_initialized", false);
        boolean changed = !initialized;
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int current = Wands.getVis(stack, aspect);
            int previous = PortNbt.getIntOr(data, aspect.getTag(), 0);
            if (initialized && current > previous) {
                current = previous + (current - previous) * 3 / 4;
                Wands.storeVis(stack, aspect, current);
            }
            if (current != previous) {
                data.putInt(aspect.getTag(), current);
                changed = true;
            }
        }
        if (player.takeXpDelay == 0) {
            for (var orb : level.getEntitiesOfClass(thaumcraft.entity.AspectOrb.class, player.getBoundingBox().inflate(3))) {
                Aspect aspect = orb.getAspect();
                if (!orb.isAlive() || !aspect.isPrimal() || Wands.getVis(stack, aspect) >= Wands.getMaxVis(stack)) continue;
                Wands.addVis(stack, aspect, orb.getAspectValue() * 4, true);
                data.putInt(aspect.getTag(), Wands.getVis(stack, aspect));
                player.takeXpDelay = 2;
                orb.discard();
                changed = true;
                break;
            }
        }
        if (changed) {
            data.putBoolean("necromancer_initialized", true);
            stack.set(key, net.minecraft.world.item.component.CustomData.of(data));
        }
    }

    static void redistribute(ItemStack stack) {
        int cutoff = Wands.getMaxVis(stack) * 9 / 10;
        var low = new ArrayList<Aspect>();
        int excess = 0;
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int amount = Wands.getVis(stack, aspect);
            if (amount <= cutoff) low.add(aspect);
            else excess += amount - cutoff;
        }
        if (low.isEmpty() || excess < 10) return;
        int share = excess / (low.size() * 4);
        if (share == 0) return;
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            if (Wands.getVis(stack, aspect) > cutoff) Wands.storeVis(stack, aspect, cutoff);
        }
        for (Aspect aspect : low) Wands.addRealVis(stack, aspect, share, true);
    }
}
