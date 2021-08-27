package com.hikarishima.lightland.magic.gui.hex;

import com.hikarishima.lightland.magic.MagicElement;
import com.hikarishima.lightland.magic.MagicRegistry;
import com.hikarishima.lightland.magic.capabilities.MagicHandler;
import com.hikarishima.lightland.magic.capabilities.ToServerMsg;
import com.hikarishima.lightland.magic.products.MagicProduct;
import com.hikarishima.lightland.magic.products.info.ProductState;
import com.lcy0x1.base.WindowBox;
import com.lcy0x1.core.magic.HexCell;
import com.lcy0x1.core.magic.HexDirection;
import com.lcy0x1.core.math.Frac;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class MagicHexScreen extends Screen {

    private static final ITextComponent TITLE = new TranslationTextComponent("gui.advancements");

    public final Screen parent;
    public final MagicHandler handler;
    public final MagicProduct<?, ?> product;
    public final HexGraphGui graph;
    public final HexResultGui result;

    public HexStatus.Save save = HexStatus.Save.YES;
    public HexStatus.Compile compile = HexStatus.Compile.EDITING;

    private double accurate_mouse_x, accurate_mouse_y;
    private boolean isScrolling = false;

    public MagicHexScreen(MagicHandler handler, MagicProduct<?, ?> product) {
        super(TITLE);
        parent = Minecraft.getInstance().screen;
        this.handler = handler;
        this.product = product;
        this.graph = new HexGraphGui(this);
        this.result = new HexResultGui(this);
    }

    public void init() {
        int sw = this.width;
        int sh = this.height;
        int w = 300;
        int h = 200;
        int x0 = (sw - w) / 2;
        int y0 = (sh - h) / 2;
        graph.box.setSize(this, x0, y0, 200, 200, 8);
        result.box.setSize(this, x0 + 200, y0, 100, 200, 8);
        if (product.usable()) {
            graph.compile();
            updated();
        }
    }

    @Override
    public void render(MatrixStack matrix, int mx, int my, float partial) {
        int col_bg = 0xFFC0C0C0;
        int col_m0 = 0xFF808080;
        int col_m1 = 0xFFFFFFFF;
        super.renderBackground(matrix);
        super.render(matrix, 0, 0, partial);
        if (Math.abs(accurate_mouse_x - mx) > 1)
            accurate_mouse_x = mx;
        if (Math.abs(accurate_mouse_y - my) > 1)
            accurate_mouse_y = my;
        graph.box.render(matrix, 0, col_bg, WindowBox.RenderType.FILL);
        graph.box.startClip(matrix);
        graph.render(matrix, accurate_mouse_x, accurate_mouse_y, partial);
        graph.box.endClip(matrix);
        graph.box.render(matrix, 8, col_m1, WindowBox.RenderType.MARGIN);
        graph.box.render(matrix, 2, col_m0, WindowBox.RenderType.MARGIN);

        result.box.render(matrix, 0, col_bg, WindowBox.RenderType.FILL);
        result.render(matrix, accurate_mouse_x, accurate_mouse_y, partial);
        result.box.render(matrix, 8, col_m1, WindowBox.RenderType.MARGIN);
        result.box.render(matrix, 2, col_m0, WindowBox.RenderType.MARGIN);

        graph.renderHover(matrix, mx, my);
    }

    @Override
    public void tick() {
        super.tick();
        result.tick();
        graph.tick();
    }

    public void mouseMoved(double mx, double my) {
        if (isScrolling)
            return;
        this.accurate_mouse_x = mx;
        this.accurate_mouse_y = my;
    }

    public boolean mouseDragged(double x0, double y0, int button, double dx, double dy) {
        if (button != 0) {
            isScrolling = false;
            return false;
        } else {
            if (graph.box.isMouseIn(x0, y0, 0)) {
                isScrolling = true;
                graph.scroll(dx, dy);
                return true;
            } else if (result.box.isMouseIn(x0, y0, 0)) {
                return result.mouseDragged(x0, y0, button, dx, dy);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double x0, double y0, int button) {
        if (result.mouseReleased(x0, y0, button))
            return true;
        return super.mouseReleased(x0, y0, button);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (graph.box.isMouseIn(mx, my, 0) && graph.mouseClicked(mx, my, button))
            return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (graph.box.isMouseIn(mx, my, 0) && graph.mouseScrolled(mx, my, amount))
            return true;
        return super.mouseScrolled(mx, my, amount);
    }

    @Override
    public boolean charTyped(char ch, int type) {
        if (graph.charTyped(ch))
            return true;
        return super.charTyped(ch, type);
    }

    protected void updated() {
        save();
    }

    private void save() {
        save = HexStatus.Save.NO;
        boolean pass = test();
        getCost();
        if (product.getState() == ProductState.CRAFTED) {
            if (!pass)
                return;
            if (result.cost > product.getCost())
                return;
        }
        forceSave(pass);
    }

    void forceSave(boolean pass) {
        save = HexStatus.Save.YES;
        product.updateBestSolution(graph.graph, result.data, pass ? result.cost : -1);
        ToServerMsg.sendHexUpdate(product);
    }

    private boolean test() {
        compile = HexStatus.Compile.EDITING;
        if (graph.error != null)
            compile = HexStatus.Compile.ERROR;
        if (graph.flow != null) {
            compile = HexStatus.Compile.FAILED;
            boolean wrong = false;
            HexCell cell = new HexCell(graph.graph, 0, 0);
            for (int i = 0; i < 6; i++) {
                graph.wrong_flow[i] = false;
                cell.toCorner(HexDirection.values()[i]);
                if (cell.exists() == (result.getElem(i) == null)) {
                    graph.wrong_flow[i] = true;
                    wrong = true;
                }
            }
            if (wrong)
                return false;
            for (int i = 0; i < 6; i++) {
                Frac[] arr = graph.flow.matrix[i];
                if (arr == null)
                    continue;
                Frac sample = null;
                for (int j = 0; j < 6; j++) {
                    Frac f = arr[j];
                    if (f == null)
                        continue;
                    if (i == j) {
                        wrong |= graph.wrong_flow[i] = true;
                        break;
                    }
                    if (sample == null)
                        sample = f;
                    else if (!sample.equals(f)) {
                        wrong |= graph.wrong_flow[i] = true;
                        break;
                    }
                }
            }
            if (wrong)
                return false;
            boolean[][] map = product.recipe.getGraph();
            for (int i = 0; i < 6; i++) {
                Frac[] arr = graph.flow.matrix[i];
                int i0 = result.data.order[i];
                boolean[] bar = map[i0];
                int rec = 0;
                for (int j = 0; j < 6; j++)
                    if (bar[result.data.order[j]])
                        rec++;
                Frac b = rec == 0 ? null : new Frac(1, rec);
                for (int j = 0; j < 6; j++) {
                    Frac f = arr[j];
                    if (!bar[result.data.order[j]]) {
                        if (f != null) {
                            wrong |= graph.wrong_flow[i] = true;
                            break;
                        }
                        continue;
                    }
                    if ((f == null) != (b == null)) {
                        wrong |= graph.wrong_flow[i] = true;
                        break;
                    }
                    if (f == null)
                        continue;
                    if (!f.equals(b)) {
                        wrong |= graph.wrong_flow[i] = true;
                    }
                }
            }
            if (!wrong)
                compile = HexStatus.Compile.COMPLETE;
            return !wrong;
        }
        return false;
    }

    private void getCost() {
        result.cost = 0;
        HexCell cell = new HexCell(graph.graph, 0, 0);
        for (cell.row = 0; cell.row < graph.graph.getRowCount(); cell.row++)
            for (cell.cell = 0; cell.cell < graph.graph.getCellCount(cell.row); cell.cell++) {
                if (cell.exists())
                    result.cost++;
            }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifier) {
        List<MagicElement> list = result.data.list;
        if (key == 259 && list.size() > 1) {
            list.remove(list.size() - 1);
            updated();
            return true;
        }
        if (key == 'W' || key == 'A' || key == 'S' || key == 'D' || key == ' ') {
            if (list.size() < 4) {
                MagicElement elem;
                if (key == 'W') elem = MagicRegistry.ELEM_AIR;
                else if (key == 'A') elem = MagicRegistry.ELEM_WATER;
                else if (key == 'S') elem = MagicRegistry.ELEM_EARTH;
                else if (key == 'D') elem = MagicRegistry.ELEM_FIRE;
                else elem = MagicRegistry.ELEM_QUINT;
                list.add(elem);
                updated();
                return true;
            }
        }
        return super.keyPressed(key, scan, modifier);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.screen == this && this.parent != null)
            this.minecraft.setScreen(this.parent);
    }

}
