package tx;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.damage.ModDamageTypes;
import thaumcraft.registry.ModEffects;
import thaumcraft.registry.ModItems;
import thaumcraft.taint.TaintBiomes;

@EventBusSubscriber(modid = Exploration.ID)
final class TaintCharms {
    private static final String CURSE = "thaumicexploration.taint_curse";
    private static final String GRACE = "thaumicexploration.taint_grace";

    private static boolean carries(Player player, Item item) {
        for (int slot = 0; slot < 10; slot++) {
            if (player.getInventory().getItem(slot).is(item)) return true;
        }
        return false;
    }

    @SubscribeEvent
    static void protect(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;
        var source = event.getSource();
        if (player.onGround() && thaumcraft.platform.Baubles.isEquipped(player, Exploration.TENTACLE_RING.get())) {
            spawnTentacle(level, player);
        }
        if ((source.is(ModDamageTypes.TAINT) || source.is(ModDamageTypes.TENTACLE) || source.is(ModDamageTypes.SWARM))
            && carries(player, Exploration.DREAMCATCHER.get()) && level.getRandom().nextInt(4) != 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator() || !player.isAlive()) return;
        var data = player.getPersistentData();
        if (!carries(player, Exploration.TAINT_CHARM.get())) {
            if (PortNbt.getBooleanOr(data, CURSE, false)) {
                data.remove(CURSE);
                data.remove(GRACE);
                if (!player.isCreative()) player.hurt(player.damageSources().magic(), 999);
            }
            return;
        }
        data.putBoolean(CURSE, true);
        player.removeEffect(ModEffects.TAINT_POISON);
        if (TaintBiomes.isTainted(level, player.blockPosition())) {
            data.remove(GRACE);
            if (player.tickCount % 80 == 0 && player.getFoodData().getFoodLevel() < 4) player.getFoodData().eat(1, 0);
            return;
        }
        int ticks = PortNbt.getIntOr(data, GRACE, 0) + 1;
        if (ticks > 100) {
            for (int slot = 0; slot < 10; slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.is(ModItems.TAINTED_GOO) || stack.is(ModItems.TAINT_TENDRIL)) {
                    stack.shrink(1);
                    ticks = 0;
                    break;
                }
            }
            if (ticks > 100) {
                if (!player.hasEffect(Exploration.WITHDRAWAL)) {
                    player.addEffect(new MobEffectInstance(Exploration.WITHDRAWAL, 100, 1));
                }
                ticks = 81;
            }
        }
        data.putInt(GRACE, ticks);
    }

    static boolean spawnTentacle(ServerLevel level, Player player) {
        var spots = new java.util.ArrayList<net.minecraft.core.BlockPos>();
        var origin = player.blockPosition();
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, 1, 3))) {
            if (!pos.equals(origin) && level.hasChunkAt(pos) && level.isEmptyBlock(pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                spots.add(pos.immutable());
            }
        }
        if (spots.isEmpty()) return false;
        var minion = Exploration.MINION.get().create(level);
        if (minion == null) return false;
        var pos = spots.get(level.getRandom().nextInt(spots.size()));
        minion.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (!level.addFreshEntity(minion)) return false;
        minion.playSound(thaumcraft.registry.ModSounds.TENTACLE.get(), 1, 1);
        return true;
    }
}
