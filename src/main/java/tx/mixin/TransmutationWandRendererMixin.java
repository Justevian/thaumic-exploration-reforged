package tx.mixin;

import java.util.stream.IntStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.client.render.WandSpecialRenderer;
import thaumcraft.wand.WandRod;

@Mixin(value = WandSpecialRenderer.class, remap = false)
abstract class TransmutationWandRendererMixin {
    @Unique private static final int[] TX_FRAMES = {
        0,0,0,0,0,0,1,2,3,4,5,5,5,5,5,5,6,7,8,9,
        10,10,10,10,10,10,11,12,13,14,15,15,15,15,15,15,16,17,18,19,
        20,20,20,20,20,20,21,22,23,24,25,25,25,25,25,25,26,27,28,29
    };
    @Unique private static final ResourceLocation[] TX_TEXTURES = IntStream.range(0, 30)
        .mapToObj(frame -> ResourceLocation.fromNamespaceAndPath("thaumicexploration",
            "textures/model/transmutation/" + frame + ".png"))
        .toArray(ResourceLocation[]::new);

    @Redirect(method = "render",
        at = @At(value = "INVOKE", target = "Lthaumcraft/wand/WandRod;texture()Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation tx$transmutationTexture(WandRod rod) {
        if (!rod.tag().equals("transmutation") && !rod.tag().equals("transmutation_staff")) return rod.texture();
        return TX_TEXTURES[TX_FRAMES[(int) (Util.getMillis() / 100L % TX_FRAMES.length)]];
    }
}
