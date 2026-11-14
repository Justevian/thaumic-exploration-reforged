package tx.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.wand.WandItem;
import thaumcraft.wand.Wands;

@Mixin(value = WandItem.class, remap = false)
abstract class WandNameMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void tx$name(ItemStack stack, CallbackInfoReturnable<Component> result) {
        var cap = Wands.getCap(stack);
        var rod = Wands.getRod(stack);
        if (!cap.item().getNamespace().equals("thaumicexploration")
            && !rod.item().getNamespace().equals("thaumicexploration")) return;
        String form = rod.staff() ? "staff" : Wands.isSceptre(stack) ? "sceptre" : "wand";
        Component rodName = Component.translatable("item.Wand." + rod.textureTag() + ".rod");
        Component objectName = Component.translatable("item.Wand." + form + ".obj");
        Component capName = Component.translatable("item.Wand." + cap.tag() + ".cap");
        String format = cap.item().getNamespace().equals("thaumicexploration") ? "addon_cap" : "base_cap";
        result.setReturnValue(Component.translatable("item.thaumicexploration.wand_name." + format,
            capName, rodName, objectName));
    }
}
