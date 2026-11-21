package tx;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.client.research.ResearchBrowserScreen;
import thaumcraft.research.ResearchItem;

final class ResearchZoomSmoke {
    private static Field field(String name) throws ReflectiveOperationException {
        Field field = ResearchBrowserScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static ResearchItem hovered(Minecraft mc, ResearchBrowserScreen browser, int x, int y) throws Exception {
        var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        browser.render(graphics, x, y, 1);
        graphics.flush();
        return (ResearchItem) field("currentHighlight").get(browser);
    }

    static void verify(Minecraft mc) throws Exception {
        ResearchBrowserScreen browser = new ResearchBrowserScreen();
        browser.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        int left = (browser.width - 256) / 2 + 16;
        int top = (browser.height - 230) / 2 + 17;
        Field zoom = field("tx$zoom");
        if (Math.abs(zoom.getFloat(null) - 0.8F) > 0.001F) throw new AssertionError("Default scale");
        ResearchItem hit = null;
        int x = left, y = top;
        for (int yy = top + 9; hit == null && yy < top + 180; yy += 12) {
            for (int xx = left + 9; hit == null && xx < left + 208; xx += 12) {
                hit = hovered(mc, browser, xx, yy);
                if (hit != null) { x = xx; y = yy; }
            }
        }
        if (hit == null) throw new AssertionError("No research under cursor");
        if (!browser.mouseScrolled(x, y, 0, 1) || zoom.getFloat(null) <= 0.8F) throw new AssertionError("Zoom in");
        if (hovered(mc, browser, x, y) != hit) throw new AssertionError("Cursor should stay on the same research");
        if (hovered(mc, browser, left - 1, y) != null) throw new AssertionError("Hover outside clipped map");
        browser.mouseScrolled(x, y, 0, -100);
        if (Math.abs(zoom.getFloat(null) - 0.4F) > 0.001F) throw new AssertionError("Min zoom");
        hovered(mc, browser, x, y);
        browser.mouseScrolled(x, y, 0, 100);
        if (Math.abs(zoom.getFloat(null) - 1.6F) > 0.001F) throw new AssertionError("Max zoom");
        hovered(mc, browser, left - 1, y);
        browser.mouseClicked(left + 50, top + 50, 0);
        double before = field("mapX").getDouble(browser);
        browser.mouseDragged(left + 50, top + 50, 0, 16, 0);
        if (Math.abs(before - field("mapX").getDouble(browser) - 10) > 0.01) throw new AssertionError("Scaled drag");
        browser.mouseReleased(left + 50, top + 50, 0);
    }

}
