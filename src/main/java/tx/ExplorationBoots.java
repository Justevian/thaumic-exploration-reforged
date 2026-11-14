package tx;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.item.gear.TcMaterials;
import thaumcraft.platform.ArmorSlot;
import thaumcraft.platform.Registration;
import thaumcraft.platform.TcArmorMaterial;

@EventBusSubscriber(modid = Exploration.ID)
final class ExplorationBoots {
    private static final Identifier SPEED = Identifier.fromNamespaceAndPath(Exploration.ID, "boots_speed");
    private static final Identifier STEP = Identifier.fromNamespaceAndPath(Exploration.ID, "boots_step");
    private static final Map<Player, Motion> MOTION = new WeakHashMap<>();

    private static final class Motion {
        int running;
        int airborne;
        int smashing;
        boolean sneaking;
        double x;
        double z;
    }

    static Item.Properties properties(Item.Properties properties, String id) {
        var material = new TcArmorMaterial(25, 1, 3, 2, 1, 25,
            SoundEvents.ARMOR_EQUIP_GENERIC, 0, 0, TcMaterials.REPAIRS_NONE,
            Identifier.fromNamespaceAndPath(Exploration.ID, id));
        return Registration.armor(properties, material, ArmorSlot.BOOTS).durability(350);
    }

    @SubscribeEvent
    static void jump(LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.getAbilities().flying) return;
        var feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (feet.is(Exploration.METEOR.get()) && player.isShiftKeyDown()) {
            var look = player.getLookAngle();
            double lift = Math.max(look.y, Math.abs(look.x + look.z)) / 1.5;
            player.setDeltaMovement(player.getDeltaMovement().add(look.x * 4, lift, look.z * 4));
        } else if (feet.is(Exploration.METEOR.get()) || feet.is(Exploration.COMET.get())) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.275, 0));
        }
    }

    @SubscribeEvent
    static void fall(LivingFallEvent event) {
        if (event.getEntity().getItemBySlot(EquipmentSlot.FEET).is(Exploration.METEOR.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        var feet = player.getItemBySlot(EquipmentSlot.FEET);
        boolean meteor = feet.is(Exploration.METEOR.get());
        boolean comet = feet.is(Exploration.COMET.get());
        if ((!meteor && !comet) || player.getAbilities().flying || player.isSpectator()) {
            MOTION.remove(player);
            updateAttributes(player, 0, false);
            return;
        }
        Motion motion = MOTION.computeIfAbsent(player, ignored -> {
            Motion initial = new Motion();
            initial.x = player.getX();
            initial.z = player.getZ();
            initial.sneaking = player.isShiftKeyDown();
            return initial;
        });
        boolean moving = Math.abs(player.getX() - motion.x) + Math.abs(player.getZ() - motion.z) > 0.02;
        motion.x = player.getX();
        motion.z = player.getZ();
        motion.running = moving || !player.onGround() ? Math.min(100, motion.running + 1) : 0;
        double speed = meteor ? 0.055 : 0.110 + (motion.running / 5) * 0.003;
        updateAttributes(player, player.isInWater() ? speed / 4 : speed, true);
        if (comet) {
            if (!player.isInWater() && player.tickCount % 4 == 0) freezeWater(level, player);
            player.fallDistance = Math.max(0, player.fallDistance - 0.25);
            motion.smashing = 0;
            motion.airborne = 0;
        } else {
            meteorTick(level, player, motion);
        }
        if (moving && !player.isInWater() && player.tickCount % 3 == 0) {
            level.sendParticles(meteor ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                player.getX(), player.getY() + 0.25, player.getZ(), 2, 0.25, 0.1, 0.25, 0.01);
        }
        motion.sneaking = player.isShiftKeyDown();
    }

    private static void updateAttributes(Player player, double speed, boolean equipped) {
        var movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (movement == null || step == null) return;
        if (equipped) {
            movement.addOrUpdateTransientModifier(new AttributeModifier(SPEED, speed, AttributeModifier.Operation.ADD_VALUE));
            step.addOrUpdateTransientModifier(new AttributeModifier(STEP, 0.4, AttributeModifier.Operation.ADD_VALUE));
        } else {
            movement.removeModifier(SPEED);
            step.removeModifier(STEP);
        }
    }

    private static void meteorTick(ServerLevel level, Player player, Motion motion) {
        if (player.onGround() || player.isInWater()) {
            if (player.onGround() && motion.smashing > 5) {
                int power = motion.smashing > 15 ? 3 : motion.smashing > 10 ? 2 : 1;
                level.explode(player, player.getX(), player.getY(), player.getZ(), power, Level.ExplosionInteraction.NONE);
            }
            motion.airborne = 0;
            motion.smashing = 0;
            return;
        }
        if (++motion.airborne > 5 && player.isShiftKeyDown() && !motion.sneaking && motion.smashing == 0) {
            motion.smashing = 1;
        }
        if (motion.smashing > 0) {
            motion.smashing++;
            player.setDeltaMovement(player.getDeltaMovement().add(0, -0.1, 0));
            player.syncVelocity = true;
        }
    }

    static void freezeWater(ServerLevel level, Player player) {
        BlockPos center = player.blockPosition().below();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, 0, -5), center.offset(5, 0, 5))) {
            if (Math.abs(pos.getX() - center.getX()) + Math.abs(pos.getZ() - center.getZ()) >= 8
                || !level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0 && level.isEmptyBlock(pos.above())) {
                level.setBlockAndUpdate(pos, Exploration.COMET_ICE.get().defaultBlockState());
                level.scheduleTick(pos, Exploration.COMET_ICE.get(), 60 + level.getRandom().nextInt(41));
            }
        }
    }
}
