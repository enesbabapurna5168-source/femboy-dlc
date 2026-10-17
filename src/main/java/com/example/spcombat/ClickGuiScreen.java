package com.example.spcombat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kategori sutunlu ayar ekrani (Combat / Movement / Render / Player / Misc / Themes).
 *
 *  - Modul satirina SOL TIK: ac/kapat
 *  - Modul satirina SAG TIK: ayarlari ac/kapat (+/-)
 *  - Ayar satiri: sayilar slider (basili tutup surukle), digerleri sol/sag tik
 *  - "Bind" satiri: sol tik -> tusa bas, sag tik veya Backspace -> temizle
 *  - Fare tekerlegi: sutunu kaydirir
 *  - Sag Shift / ESC: kapatir
 *
 * Fare ve tus girisi GLFW ile okunuyor (surumler arasi Screen imza degisikliklerinden
 * etkilenmesin diye).
 */
public class ClickGuiScreen extends Screen {
    private static final int HEAD_H = 18;
    private static final int ROW_H = 16;
    private static final int SET_H = 14;
    private static final int GAP = 6;
    private static final int CATS = 5;
    private static final int COLS = CATS + 1; // + Themes

    private static final Set<Module> OPEN = new HashSet<>();
    private static final int[] scroll = new int[COLS];

    private final int[] colX = new int[COLS];
    private final int[] contentH = new int[COLS];
    private int colW, headTop, bodyTop, viewH;

    private Module binding;
    private Setting dragging;
    private int dragX, dragW;
    private boolean lastL, lastR, lastShift = true, shiftReleased;

    public ClickGuiScreen() {
        super(Text.literal("SP Combat Utils"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ESC tus atama modunda iptal icin kullanilir, ekrani kapatmasin
    public boolean shouldCloseOnEsc() {
        return binding == null;
    }

    // Ekran kapanirken ayarlari kaydet
    public void removed() {
        Config.save();
    }

    // Fare tekerlegi: imza 1.20.2 - 1.21.x arasinda ayni
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int c = 0; c < COLS; c++) {
            if (mouseX >= colX[c] && mouseX < colX[c] + colW
                    && mouseY >= bodyTop && mouseY < bodyTop + viewH) {
                scroll[c] -= (int) (vertical * ROW_H * 1.5);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        ctx.fill(0, 0, this.width, this.height, 0x44000000);

        long win = this.client.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean clickL = l && !lastL;
        boolean clickR = r && !lastR;
        lastL = l;
        lastR = r;

        // Sag Shift tekrar basilinca kapat (acarken basili tutulan tusu saymaz)
        boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shift) shiftReleased = true;
        if (shift && shiftReleased && !lastShift && binding == null) {
            this.close();
            return;
        }
        lastShift = shift;

        if (binding != null) captureBind(win);

        layout();

        // slider surukleme
        if (dragging != null) {
            if (l) dragging.setFraction((mx - dragX) / (double) dragW);
            else dragging = null;
        }

        int accent = Theme.cur().accent;
        Module.Category[] cats = Module.Category.values();
        for (int c = 0; c < CATS; c++) {
            drawCategory(ctx, c, cats[c], mx, my, clickL, clickR, accent);
        }
        drawThemes(ctx, CATS, mx, my, clickL, accent);

        ctx.drawText(this.textRenderer,
                "Sol tik: ac/kapat   Sag tik: ayarlar   Slider: surukle   Bind: ayar satirindan   Sag Shift / ESC: kapat",
                Math.max(6, (this.width - 6 - this.textRenderer.getWidth(
                        "Sol tik: ac/kapat   Sag tik: ayarlar   Slider: surukle   Bind: ayar satirindan   Sag Shift / ESC: kapat")) / 2),
                this.height - 14, 0xFFBFC3CC, true);
    }

    private void layout() {
        colW = MathHelper.clamp((this.width - 20 - GAP * (COLS - 1)) / COLS, 86, 124);
        int totalW = colW * COLS + GAP * (COLS - 1);
        int x0 = (this.width - totalW) / 2;
        for (int c = 0; c < COLS; c++) colX[c] = x0 + c * (colW + GAP);

        headTop = Math.max(18, (int) (this.height * 0.2f));
        bodyTop = headTop + HEAD_H;
        viewH = Math.max(60, this.height - bodyTop - 24);
    }

    // ---------------- Tus atama ----------------
    private void captureBind(long win) {
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            binding = null;
            return;
        }
        if (GLFW.glfwGetKey(win, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS) {
            binding.bind = -1;
            binding = null;
            return;
        }
        for (int[] range : Keys.RANGES) {
            for (int k = range[0]; k <= range[1]; k++) {
                if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_BACKSPACE
                        || k == GLFW.GLFW_KEY_DELETE || k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;
                if (GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS) {
                    binding.bind = k;
                    binding.bindDown = true; // hemen tetiklenmesin
                    binding = null;
                    return;
                }
            }
        }
    }

    // ---------------- Kategori sutunu ----------------
    private void drawCategory(DrawContext ctx, int c, Module.Category cat, int mx, int my,
                              boolean clickL, boolean clickR, int accent) {
        List<Module> mods = Modules.byCategory(cat);
        int x = colX[c];

        int on = 0;
        for (Module m : mods) if (m.enabled) on++;
        drawHeader(ctx, x, cat.title, on + "/" + mods.size(), accent);

        ctx.fill(x, bodyTop, x + colW, bodyTop + viewH, 0xB00B0C10);
        ctx.enableScissor(x, bodyTop, x + colW, bodyTop + viewH);

        boolean hoverCol = mx >= x && mx < x + colW && my >= bodyTop && my < bodyTop + viewH;
        int y = bodyTop - scroll[c];

        for (Module m : mods) {
            boolean open = OPEN.contains(m);
            boolean over = hoverCol && my >= y && my < y + ROW_H;

            if (m.enabled) {
                ctx.fill(x, y, x + colW, y + ROW_H, Theme.alpha(accent, 0x30));
                ctx.fill(x, y, x + 2, y + ROW_H, accent);
            }
            if (over) ctx.fill(x, y, x + colW, y + ROW_H, 0x18FFFFFF);

            String marker = open ? "-" : "+";
            int markerX = x + colW - 6 - this.textRenderer.getWidth(marker);
            ctx.drawText(this.textRenderer, marker, markerX, y + 4, 0xFF7C8088, false);

            int labelMax = colW - 8 - 14;
            if (m.bind >= 0) {
                String b = "[" + Keys.name(m.bind) + "]";
                int bw = this.textRenderer.getWidth(b);
                ctx.drawText(this.textRenderer, b, markerX - 4 - bw, y + 4, 0xFF8A8F99, false);
                labelMax -= bw + 4;
            }
            ctx.drawText(this.textRenderer, this.textRenderer.trimToWidth(m.name, Math.max(20, labelMax)),
                    x + 8, y + 4, m.enabled ? 0xFFFFFFFF : 0xFFB4B8C0, false);

            if (over && clickL) m.enabled = !m.enabled;
            if (over && clickR) {
                if (!OPEN.remove(m)) OPEN.add(m);
            }
            y += ROW_H;

            if (!open) continue;

            int blockH = (m.settings.size() + 1) * SET_H;
            ctx.fill(x, y, x + colW, y + blockH, 0x60000000);

            // Bind satiri
            boolean bo = hoverCol && my >= y && my < y + SET_H;
            if (bo) ctx.fill(x + 4, y + 1, x + colW - 4, y + SET_H - 1, 0x18FFFFFF);
            ctx.drawText(this.textRenderer, "Bind", x + 10, y + 3, 0xFFC8CCD4, false);
            String bv = binding == m ? "..." : (m.bind < 0 ? "None" : Keys.name(m.bind));
            ctx.drawText(this.textRenderer, bv, x + colW - 10 - this.textRenderer.getWidth(bv), y + 3,
                    binding == m ? 0xFFFFD255 : accent, false);
            if (bo && clickL) binding = (binding == m) ? null : m;
            if (bo && clickR) { m.bind = -1; if (binding == m) binding = null; }
            y += SET_H;

            // Ayar satirlari
            for (Setting s : m.settings) {
                boolean so = hoverCol && my >= y && my < y + SET_H;
                int sx = x + 6, sw = colW - 12;

                if (s.type == Setting.Type.NUM) {
                    ctx.fill(sx, y + 1, sx + sw, y + SET_H - 1, 0x28FFFFFF);
                    ctx.fill(sx, y + 1, sx + (int) (sw * s.fraction()), y + SET_H - 1, Theme.alpha(accent, 0x70));
                    if (so && clickL) {
                        dragging = s;
                        dragX = sx;
                        dragW = sw;
                        s.setFraction((mx - sx) / (double) sw);
                    }
                } else {
                    if (so) ctx.fill(sx, y + 1, sx + sw, y + SET_H - 1, 0x18FFFFFF);
                    if (so && clickL) s.click(true);
                    if (so && clickR) s.click(false);
                }

                String v = s.display();
                int vw = this.textRenderer.getWidth(v);
                int vColor = s.type == Setting.Type.BOOL ? (s.bool ? accent : 0xFF8A8F99) : 0xFFFFFFFF;
                ctx.drawText(this.textRenderer,
                        this.textRenderer.trimToWidth(s.name, Math.max(16, sw - vw - 10)),
                        sx + 4, y + 3, 0xFFC8CCD4, false);
                ctx.drawText(this.textRenderer, v, sx + sw - 4 - vw, y + 3, vColor, false);
                y += SET_H;
            }
        }

        ctx.disableScissor();
        finishColumn(ctx, c, x, y);
    }

    // ---------------- Tema sutunu ----------------
    private void drawThemes(DrawContext ctx, int c, int mx, int my, boolean clickL, int accent) {
        int x = colX[c];
        drawHeader(ctx, x, "Themes", "", accent);

        ctx.fill(x, bodyTop, x + colW, bodyTop + viewH, 0xB00B0C10);
        ctx.enableScissor(x, bodyTop, x + colW, bodyTop + viewH);

        boolean hoverCol = mx >= x && mx < x + colW && my >= bodyTop && my < bodyTop + viewH;
        int y = bodyTop - scroll[c];

        for (int i = 0; i < Theme.ALL.length; i++) {
            Theme t = Theme.ALL[i];
            boolean over = hoverCol && my >= y && my < y + ROW_H;
            boolean sel = i == Theme.index();

            if (sel) ctx.fill(x + 3, y + 1, x + colW - 3, y + ROW_H - 1, Theme.alpha(t.accent, 0x28));
            if (over) ctx.fill(x + 3, y + 1, x + colW - 3, y + ROW_H - 1, 0x18FFFFFF);
            if (sel) outline(ctx, x + 3, y + 1, colW - 6, ROW_H - 2, t.accent);

            ctx.fill(x + 9, y + 5, x + 15, y + 11, t.accent);
            ctx.drawText(this.textRenderer, t.name, x + 21, y + 4, sel ? 0xFFFFFFFF : 0xFFB4B8C0, false);

            if (over && clickL) Theme.set(i);
            y += ROW_H;
        }

        ctx.disableScissor();
        finishColumn(ctx, c, x, y);
    }

    // ---------------- ortak parcalar ----------------
    private void drawHeader(DrawContext ctx, int x, String title, String count, int accent) {
        ctx.fill(x, headTop, x + colW, headTop + HEAD_H, 0xE0101216);
        ctx.fill(x, headTop + HEAD_H - 1, x + colW, headTop + HEAD_H, Theme.alpha(accent, 0xB0));
        ctx.drawText(this.textRenderer, title, x + 8, headTop + 5, 0xFFFFFFFF, true);
        if (!count.isEmpty()) {
            ctx.drawText(this.textRenderer, count,
                    x + colW - 7 - this.textRenderer.getWidth(count), headTop + 5, 0xFF8A8F99, false);
        }
    }

    private void finishColumn(DrawContext ctx, int c, int x, int yEnd) {
        contentH[c] = (yEnd + scroll[c]) - bodyTop;
        int max = Math.max(0, contentH[c] - viewH);
        scroll[c] = MathHelper.clamp(scroll[c], 0, max);

        if (max > 0) {
            int barH = Math.max(14, (int) ((long) viewH * viewH / contentH[c]));
            int barY = bodyTop + (int) ((long) (viewH - barH) * scroll[c] / max);
            ctx.fill(x + colW - 2, barY, x + colW, barY + barH, Theme.alpha(Theme.cur().accent, 0xA0));
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
