package tx.mixin;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.crafting.ArcaneWandRecipe;

@Mixin(value = ArcaneWandRecipe.class, remap = false)
abstract class CreativeWandRecipeMixin {
    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void tx$allowCreativeAssembly(List<ItemStack> grid, Player player, CallbackInfoReturnable<Boolean> result) {
        if (player.isCreative() && !result.getReturnValueZ()
            && (tx$isExplorationPart(grid.get(4)) || tx$isExplorationPart(grid.get(2))
                || grid.get(4).is(thaumcraft.registry.ModItems.ICY_ROD.get()))
            && !((ArcaneWandRecipe) (Object) this).result(grid).isEmpty()) {
            result.setReturnValue(true);
        }
    }

    @Unique
    private static boolean tx$isExplorationPart(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
            .getNamespace().equals("thaumicexploration");
    }
}
