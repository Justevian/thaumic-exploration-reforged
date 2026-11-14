package tx.mixin;

import java.util.Comparator;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.entity.monster.taint.Taintacle;
import tx.TentacleMinion;

@Mixin(value = Taintacle.class, remap = false)
abstract class TentacleMinionTargetMixin {
    @Inject(method = "findTarget", at = @At("HEAD"), cancellable = true)
    private void tx$findHostile(CallbackInfoReturnable<LivingEntity> result) {
        if (!((Object) this instanceof TentacleMinion minion)) return;
        result.setReturnValue(minion.level().getEntitiesOfClass(Monster.class, minion.getBoundingBox().inflate(1.0),
            target -> target.isAlive() && !(target instanceof Taintacle))
            .stream().min(Comparator.comparingDouble(minion::distanceToSqr)).orElse(null));
    }
}
