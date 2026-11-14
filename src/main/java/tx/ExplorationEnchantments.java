package tx;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Exploration.ID)
final class ExplorationEnchantments {
    static final ResourceKey<Enchantment> BINDING = key("binding");
    static final ResourceKey<Enchantment> DISARM = key("disarming");
    static final ResourceKey<Enchantment> VISION = key("night_vision");

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(Exploration.ID, path));
    }

    static final class Status extends MobEffect {
        private final boolean withdrawal;
        Status(boolean withdrawal) {
            super(MobEffectCategory.HARMFUL, withdrawal ? 0x78438A : 0x665744);
            this.withdrawal = withdrawal;
        }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return withdrawal && duration % 20 == 0;
        }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            if (thaumcraft.taint.TaintBiomes.isTainted(level, entity.blockPosition())) return false;
            entity.hurtServer(level, level.damageSources().source(thaumcraft.damage.ModDamageTypes.TAINT), 1);
            return true;
        }
    }

    private static int enchantment(LivingEntity entity, EquipmentSlot slot, ResourceKey<Enchantment> key) {
        var level = entity.level();
        var item = entity.getItemBySlot(slot);
        if (item.isEmpty()) return 0;
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
            .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, item)).orElse(0);
    }

    @SubscribeEvent
    static void hit(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)
            || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)) return;
        int binding = enchantment(attacker, EquipmentSlot.MAINHAND, BINDING);
        if (binding > 0) victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
        if (binding > 1) victim.addEffect(new MobEffectInstance(Exploration.BINDING_EFFECT, 50));
        int disarm = enchantment(attacker, EquipmentSlot.MAINHAND, DISARM);
        if (disarm <= 0 || victim instanceof Player || victim.getMainHandItem().isEmpty()
            || level.getRandom().nextInt(Math.max(1, 10 - 2 * disarm)) != 0) return;
        var item = victim.getMainHandItem().copy();
        victim.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        net.minecraft.world.Containers.dropItemStack(level, victim.getX(), victim.getY(), victim.getZ(), item);
    }

    @SubscribeEvent
    static void teleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && entity.hasEffect(Exploration.BINDING_EFFECT)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
        if (entity instanceof Creeper creeper && entity.hasEffect(Exploration.BINDING_EFFECT)) creeper.setSwellDir(-1);
        if (entity.tickCount % 10 == 0 && enchantment(entity, EquipmentSlot.HEAD, VISION) > 0) {
            var vision = entity.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || vision.getDuration() < 202)
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
        }
    }
}
