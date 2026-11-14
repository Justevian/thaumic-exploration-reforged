package tx;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import thaumcraft.entity.monster.taint.TaintacleTiny;

public final class TentacleMinion extends TaintacleTiny {
    public TentacleMinion(EntityType<? extends TaintacleTiny> type, Level level) {
        super(type, level);
    }

    @Override protected void registerGoals() {}
}
