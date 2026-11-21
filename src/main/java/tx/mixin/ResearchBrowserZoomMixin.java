package tx.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import thaumcraft.client.research.ResearchBrowserScreen;
import thaumcraft.research.ResearchItem;

@Mixin(value = ResearchBrowserScreen.class, remap = false)
abstract class ResearchBrowserZoomMixin extends Screen {
    @Unique private static final String TX_BACKGROUND_BLIT =
        "Lthaumcraft/client/platform/GuiBlit;blit(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIIIIIIIII)V";
    @Unique private static final String TX_SCISSOR_END = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V";
    @Unique private static final int TX_VIEW_WIDTH = 224;
    @Unique private static final int TX_VIEW_HEIGHT = 196;
    @Unique private static float tx$zoom = 0.8F;

    @Shadow private double mapX;
    @Shadow private double mapY;
    @Shadow private double prevMapX;
    @Shadow private double prevMapY;
    @Shadow private double targetX;
    @Shadow private double targetY;
    @Shadow private int guiMapTop;
    @Shadow private int guiMapLeft;
    @Shadow private int guiMapBottom;
    @Shadow private int guiMapRight;
    @Shadow private ResearchItem currentHighlight;

    @Shadow private int left() { throw new AssertionError(); }
    @Shadow private int top() { throw new AssertionError(); }

    protected ResearchBrowserZoomMixin(Component title) { super(title); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int x = left() + 16;
        int y = top() + 17;
        if (vertical == 0 || mouseX < x || mouseX >= x + TX_VIEW_WIDTH || mouseY < y || mouseY >= y + TX_VIEW_HEIGHT) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        float next = Mth.clamp((float) (tx$zoom * Math.pow(1.18, vertical)), 0.4F, 1.6F);
        if (next != tx$zoom) {
            double shift = 1.0 / tx$zoom - 1.0 / next;
            double viewX = Mth.clamp(mapX, guiMapTop, Math.max(guiMapTop, guiMapBottom - 1));
            double viewY = Mth.clamp(mapY, guiMapLeft, Math.max(guiMapLeft, guiMapRight - 1));
            prevMapX = mapX = targetX = Mth.clamp(viewX + (mouseX - x) * shift, guiMapTop, Math.max(guiMapTop, guiMapBottom - 1));
            prevMapY = mapY = targetY = Mth.clamp(viewY + (mouseY - y) * shift, guiMapLeft, Math.max(guiMapLeft, guiMapRight - 1));
            tx$zoom = next;
            currentHighlight = null;
        }
        return true;
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target =
        "Lthaumcraft/client/research/ResearchBrowserScreen;drawMap(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void tx$mapMouseCoordinates(Args args) {
        int x = left() + 16;
        int y = top() + 17;
        int mouseX = args.get(1);
        int mouseY = args.get(2);
        if (mouseX < x || mouseX >= x + TX_VIEW_WIDTH || mouseY < y || mouseY >= y + TX_VIEW_HEIGHT) {
            args.set(1, x - 1);
            args.set(2, y - 1);
            return;
        }
        args.set(1, x + Mth.floor((mouseX - x) / tx$zoom));
        args.set(2, y + Mth.floor((mouseY - y) / tx$zoom));
    }

    // Keep the background, scroll controls, tabs and tooltip in screen coordinates.
    @Inject(method = "drawMap", at = @At(value = "INVOKE", target = TX_BACKGROUND_BLIT, ordinal = 0, shift = At.Shift.AFTER))
    private void tx$beginMapZoom(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int x = left() + 16;
        int y = top() + 17;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(tx$zoom, tx$zoom, 1);
        graphics.pose().translate(-x, -y, 0);
    }

    @Inject(method = "drawMap", at = @At(value = "INVOKE", target = TX_SCISSOR_END))
    private void tx$endMapZoom(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        graphics.pose().popPose();
    }

    @ModifyConstant(method = "drawMap", constant = @Constant(intValue = TX_VIEW_WIDTH), require = 2,
        slice = @Slice(from = @At(value = "INVOKE", target = TX_BACKGROUND_BLIT, ordinal = 0),
            to = @At(value = "INVOKE", target = TX_SCISSOR_END)))
    private int tx$logicalWidth(int width) { return (int) Math.ceil(width / tx$zoom); }

    @ModifyConstant(method = "drawMap", constant = @Constant(intValue = TX_VIEW_HEIGHT), require = 2,
        slice = @Slice(from = @At(value = "INVOKE", target = TX_BACKGROUND_BLIT, ordinal = 0),
            to = @At(value = "INVOKE", target = TX_SCISSOR_END)))
    private int tx$logicalHeight(int height) { return (int) Math.ceil(height / tx$zoom); }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double tx$dragX(double delta) { return delta / tx$zoom; }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private double tx$dragY(double delta) { return delta / tx$zoom; }

    @ModifyConstant(method = {"mouseClicked", "mouseDragged"}, constant = @Constant(intValue = 8, ordinal = 0))
    private int tx$alignDragArea(int value) { return 16; }
}
