package tx;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.registry.ModBlocks;

final class FoodTalisman extends Item {
    private static final int CAPACITY = 100;

    FoodTalisman(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity owner, int slot, boolean selected) {
        if (level.isClientSide() || !(owner instanceof Player player) || player.isCreative() || player.isSpectator()
            || owner.tickCount % 20 != 0) return;

        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int storedFood = PortNbt.getIntOr(data, "food", 0);
        float storedSaturation = PortNbt.getFloatOr(data, "saturation", 0);
        var inventory = player.getInventory();
        boolean ate = false;
        for (int index = 0; index < 10; index++) {
            ItemStack candidate = inventory.getItem(index);
            var food = candidate.get(DataComponents.FOOD);
            if (food == null || !food.effects().isEmpty()
                || food.usingConvertsTo().isPresent() || candidate.is(ModBlocks.MANA_BEAN)
                || food.nutrition() <= 0 || storedFood + food.nutrition() > CAPACITY) continue;
            storedFood += food.nutrition();
            storedSaturation = Math.min(CAPACITY, storedSaturation + food.saturation());
            candidate.shrink(1);
            ate = true;
        }
        if (ate) {
            inventory.setChanged();
            player.playSound(SoundEvents.GENERIC_EAT, 0.6F, 1);
        }

        var hunger = player.getFoodData();
        int foodUsed = Math.min(20 - hunger.getFoodLevel(), storedFood);
        hunger.setFoodLevel(hunger.getFoodLevel() + foodUsed);
        float saturationUsed = Math.min(hunger.getFoodLevel() - hunger.getSaturationLevel(), storedSaturation);
        hunger.setSaturation(hunger.getSaturationLevel() + saturationUsed);
        int foodLeft = storedFood - foodUsed;
        float saturationLeft = storedSaturation - saturationUsed;
        if (ate || foodUsed != 0 || saturationUsed != 0) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putInt("food", foodLeft);
                tag.putFloat("saturation", saturationLeft);
            });
        }
    }
}
