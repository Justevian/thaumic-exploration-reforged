package tx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumcraft.platform.Bauble;
import thaumcraft.platform.BaubleType;
import thaumcraft.wand.VisDiscountGear;

final class ExplorationAccessories {
    static final String[] RINGS = {"air", "fire", "water", "earth", "order", "entropy"};
    static final Aspect[] ASPECTS = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};

    static final class Ring extends Item implements Bauble, VisDiscountGear {
        private final int index;

        Ring(Properties properties, int index) {
            super(properties.stacksTo(1));
            this.index = index;
        }

        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }
        @Override public int getVisDiscount(ItemStack stack, Player player, Aspect aspect) {
            return aspect == ASPECTS[index] ? 3 : 0;
        }
    }

    static final class TentacleRing extends Item implements Bauble {
        TentacleRing(Properties properties) { super(properties.stacksTo(1)); }
        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }
    }

    static final class Belt extends Item implements Bauble {
        private static final ResourceLocation RESISTANCE = ResourceLocation.fromNamespaceAndPath(Exploration.ID, "stabilizer_belt");

        Belt(Properties properties) { super(properties.stacksTo(1)); }
        @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.BELT; }

        @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
            var attribute = wearer.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (!wearer.level().isClientSide() && attribute != null && !attribute.hasModifier(RESISTANCE)) {
                attribute.addTransientModifier(new AttributeModifier(RESISTANCE, 1, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        @Override public void onUnequipped(ItemStack stack, LivingEntity wearer) {
            var attribute = wearer.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (!wearer.level().isClientSide() && attribute != null) attribute.removeModifier(RESISTANCE);
        }
    }
}
