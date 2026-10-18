package com.example.spcombat;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Yumusak yazi tipi (Inter). Harfler hazir bir resimden (font_m.png / font_b.png) cizilir,
 * oyunun kendi pikselli yazisindan cok daha temiz gorunur. Turkce harfler desteklenir.
 * size = yazi yuksekligi (GUI birimi), normal yazi icin 8-10 arasi idealdir.
 */
public final class Fnt {
    private Fnt() {}

    private static final Identifier MED = Identifier.of("spcombat", "textures/gui/font_m.png");
    private static final Identifier BOLD = Identifier.of("spcombat", "textures/gui/font_b.png");
    private static final int[] MAP = new int[0x2700];

    static {
        java.util.Arrays.fill(MAP, -1);
        for (int i = 0; i < FontData.CHARS.length(); i++) {
            char c = FontData.CHARS.charAt(i);
            if (c < MAP.length) MAP[c] = i;
        }
    }

    private static int idx(char c) {
        int i = c < MAP.length ? MAP[c] : -1;
        if (i < 0) i = MAP['?'];
        return i;
    }

    public static float width(String s, float size) { return width(s, size, false); }

    public static float width(String s, float size, boolean bold) {
        float[] adv = bold ? FontData.ADV_B : FontData.ADV_M;
        float sc = size / FontData.EM;
        float w = 0;
        for (int i = 0; i < s.length(); i++) w += adv[idx(s.charAt(i))] * sc;
        return w;
    }

    public static int w(String s, float size) { return Math.round(width(s, size, false)); }

    /** Verilen genislige sigacak sekilde kisaltir. */
    public static String trim(String s, float size, float maxW) { return trim(s, size, maxW, false); }

    public static String trim(String s, float size, float maxW, boolean bold) {
        if (width(s, size, bold) <= maxW) return s;
        String e = "...";
        int n = s.length();
        while (n > 0 && width(s.substring(0, n), size, bold) + width(e, size, bold) > maxW) n--;
        return s.substring(0, n) + e;
    }

    public static void draw(DrawContext ctx, String s, float x, float y, float size, int color) {
        draw(ctx, s, x, y, size, color, false);
    }

    public static void draw(DrawContext ctx, String s, float x, float y, float size, int color, boolean bold) {
        if (s == null || s.isEmpty()) return;
        float[] adv = bold ? FontData.ADV_B : FontData.ADV_M;
        Identifier tex = bold ? BOLD : MED;
        float sc = size / FontData.EM;
        int dw = Math.round(FontData.CW * sc), dh = Math.round(FontData.CH * sc);
        float cur = x;
        float top = y + size * 0.82f - FontData.BASE * sc;
        int ty = Math.round(top);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int k = idx(c);
            if (c != ' ') {
                int u = (k % FontData.COLS) * FontData.CW;
                int v = (k / FontData.COLS) * FontData.CH;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, Math.round(cur - FontData.M * sc), ty,
                        (float) u, (float) v, dw, dh, FontData.CW, FontData.CH, FontData.TEX, FontData.TEX, color);
            }
            cur += adv[k] * sc;
        }
    }

    public static void drawShadow(DrawContext ctx, String s, float x, float y, float size, int color, boolean bold) {
        draw(ctx, s, x + 1, y + 1, size, ((color >>> 24) * 0x70 / 255) << 24, bold);
        draw(ctx, s, x, y, size, color, bold);
    }

    public static void drawCentered(DrawContext ctx, String s, float cx, float y, float size, int color, boolean bold) {
        draw(ctx, s, cx - width(s, size, bold) / 2f, y, size, color, bold);
    }

    public static void drawRight(DrawContext ctx, String s, float rx, float y, float size, int color, boolean bold) {
        draw(ctx, s, rx - width(s, size, bold), y, size, color, bold);
    }
}
