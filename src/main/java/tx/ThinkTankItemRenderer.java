package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.client.platform.ImmediateGeometry;

final class ThinkTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    ThinkTankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
        MultiBufferSource buffers, int light, int overlay) {
        ThinkTankRenderer.renderModel(poses, new ImmediateGeometry(buffers), light, 0);
    }
}
