package tx;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.WandRegistry;
import thaumcraft.wand.Wands;

@EventBusSubscriber(modid = Exploration.ID)
final class ExplorationCaps {
    static final String SOJOURNER = "sojourner";
    static final String MECHANIST = "mechanist";

    static void register() {
        for (String cap : List.of(SOJOURNER, MECHANIST)) {
            WandRegistry.registerCap(cap, 0.95F, List.of(), 0,
                Identifier.fromNamespaceAndPath(Exploration.ID, cap + "_wand_cap"), 6, null,
                Identifier.fromNamespaceAndPath(Exploration.ID, "textures/model/" + cap + "_wand_cap.png"));
        }
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        ItemStack wand = player.getMainHandItem();
        if (!Wands.isWand(wand)) return;
        String cap = Wands.getCap(wand).tag();
        if (cap.equals(MECHANIST) && player.isUsingItem() && player.getUseItem() == wand) {
            int remaining = player.getUseItemRemainingTicks();
            BlockPos pos = WandKeys.TARGET.get(wand);
            if (remaining % 5 == 2 && pos != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof NodeBlockEntity node) {
                node.onUsingWandTick(wand, player, remaining - 2);
            }
        }
        if (!cap.equals(SOJOURNER) || player.tickCount % 20 != 0) return;
        var room = Wands.getAspectsWithRoom(wand).getAspects();
        if (room.isEmpty()) return;
        BlockPos origin = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -8, -8), origin.offset(8, 8, 8))) {
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof NodeBlockEntity node)) continue;
            for (Aspect aspect : room) {
                if (node.getAspects().getAmount(aspect) > 1 && node.takeFromContainer(aspect, 1)) {
                    Wands.addVis(wand, aspect, 1, true);
                    node.setChanged();
                    level.sendBlockUpdated(pos, node.getBlockState(), node.getBlockState(), 2);
                    return;
                }
            }
        }
    }
}
