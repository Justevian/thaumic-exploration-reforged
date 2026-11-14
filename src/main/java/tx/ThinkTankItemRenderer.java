package tx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import org.joml.Vector3f;
import org.joml.Vector3fc;

record ThinkTankItemRenderer() implements NoDataSpecialModelRenderer {
    @Override public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
        ThinkTankRenderer.renderModel(pose, collector, light, 0);
    }

    @Override public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y * 1.125F, z));
            }
        }
    }

    record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
        @Override public ThinkTankItemRenderer bake(BakingContext context) { return new ThinkTankItemRenderer(); }
        @Override public MapCodec<Unbaked> type() { return CODEC; }
    }
}
