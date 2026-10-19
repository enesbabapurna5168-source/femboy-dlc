package com.example.spcombat;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Ana menu: sol tarafta "Dark DLC" basligi, surum/tarih ve yuvarlak koseli 4 buton.
 * Butonlar oyunun kendi butonlari (tiklama davranisi degismez); sadece yerleri degisir ve
 * ustlerine yeni gorunum cizilir. Diger her sey (logo, splash yazisi, alt yazilar) kapatilir.
 */
public final class TitleStyle {
    private TitleStyle() {}

    private static int activeId;
    private static final List<ClickableWidget> shown = new ArrayList<>();
    private static final List<String> labels = new ArrayList<>();

    private static final String[] KEYS = {"menu.singleplayer", "menu.multiplayer", "menu.options", "menu.quit"};
    private static final String[] NAMES = {"Singleplayer", "Multiplayer", "Settings", "Quit Game"};

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof TitleScreen)) return;

            shown.clear();
            labels.clear();
            List<ClickableWidget> all = new ArrayList<>(Screens.getButtons(screen));
            for (int i = 0; i < KEYS.length; i++) {
                for (ClickableWidget b : all) {
                    if (b.getMessage().getContent() instanceof TranslatableTextContent t
                            && t.getKey().equals(KEYS[i])) {
                        shown.add(b);
                        labels.add(NAMES[i]);
                    }
                }
            }
            for (ClickableWidget b : all) {
                if (!shown.contains(b)) b.visible = false; // Realms, dil, erisilebilirlik vs.
            }

            // AltManager butonu (Multiplayer'in altina)
            ButtonWidget alt = ButtonWidget.builder(Text.literal("AltManager"),
                    btn -> client.setScreen(new AltScreen(screen))).dimensions(0, 0, 120, 20).build();
            Screens.getButtons(screen).add(alt);
            int at = Math.min(2, shown.size());
            shown.add(at, alt);
            labels.add(at, "AltManager");

            layout(w, h);

            // Fabric, ekran her yeniden kurulunca (boyut degisimi, geri donus) ekran olaylarini sifirlar:
            // bu yuzden her seferinde yeniden kaydediyoruz. Eski kayit kalirsa cizmesin diye numara veriyoruz.
            final int id = ++activeId;
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                if (id == activeId) draw(s, ctx, mx, my);
            });
        });
    }

    /** Butonlari sol sutuna dizer (her karede de cagrilir, ilk acilista yanlis kalmasin). */
    private static void layout(int w, int h) {
        int bw = 120, bh = 20, gap = 10;
        int x = Math.max(16, (int) (w * 0.07));
        int blockH = 46 + shown.size() * (bh + gap);
        int top = Math.max(10, (h - blockH) / 2);
        int y = top + 46;
        for (ClickableWidget b : shown) {
            b.visible = true;
            b.setX(x);
            b.setY(y);
            b.setWidth(bw);
            y += bh + gap;
        }
    }

    private static final float[] hv = new float[8];

    private static void draw(Screen s, DrawContext ctx, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int accent = Theme.cur().accent;
        int w = s.width, h = s.height;
        layout(w, h);
        double tt = System.currentTimeMillis() / 1000.0;

        // koyu arka plan: siyaha yakin, hafif mor tonlu
        ctx.fillGradient(0, 0, w, h, 0xFF030305, 0xFF0A0613);
        // yavasca suzulen yumusak isik kureleri
        for (int i = 0; i < 4; i++) {
            double cx = w * (0.2 + 0.2 * i) + Math.sin(tt * 0.12 + i * 1.7) * w * 0.10;
            double cy = h * (0.35 + 0.12 * (i % 2)) + Math.cos(tt * 0.10 + i * 2.3) * h * 0.12;
            int rad = (int) (h * (0.30 + 0.05 * i));
            Draw.rbox(ctx, (int) cx - rad, (int) cy - rad, rad * 2, rad * 2, Theme.alpha(accent, 0x0C + 4 * (i % 2)), rad);
        }
        ctx.fillGradient(0, h / 2, w, h, 0x00000000, Theme.alpha(accent, 0x2A));
        // yukari suzulen kucuk isiklar
        for (int i = 0; i < 46; i++) {
            double sd = (i * 0.6180339) % 1.0;
            double px = ((sd * 997.0) % 1.0) * w + Math.sin(tt * 0.5 + i) * 6;
            double py = h - ((tt * (6 + 10 * sd) + sd * h * 3.1) % (h + 20));
            int a = 0x18 + (int) (0x30 * ((i * 37) % 10) / 10.0);
            int sz = (i % 4 == 0) ? 2 : 1;
            ctx.fill((int) px, (int) py, (int) px + sz, (int) py + sz, Theme.alpha(0xFFFFFFFF, a));
        }
        // kenarlarda koyulasma (vignette)
        ctx.fillGradient(0, 0, w, h / 5, 0xB0000000, 0x00000000);
        ctx.fillGradient(0, h - h / 5, w, h, 0x00000000, 0xA0000000);
        ctx.fillGradient(0, 0, w / 6, h, 0x90000000, 0x00000000);
        ctx.fillGradient(w - w / 6, 0, w, h, 0x00000000, 0x90000000);
        // en ustte ince parlayan cizgi
        ctx.fillGradient(0, 0, w / 2, 1, Theme.alpha(accent, 0x00), Theme.alpha(accent, 0xC0));
        ctx.fillGradient(w / 2, 0, w, 1, Theme.alpha(accent, 0xC0), Theme.alpha(accent, 0x00));

        int on = 0;
        for (Module mo : Modules.ALL) if (mo.enabled && !mo.cosmetic && !mo.actionKey) on++;

        // ---- sag ust kose: "Dark DLC" rozeti
        float bs = 15f;
        float btw = Fnt.widthTitle("Dark DLC", bs);
        int pw = (int) btw + 46, ph = 30;
        int pxx = w - pw - 12, pyy = 12;
        Draw.shadow(ctx, pxx, pyy, pw, ph, 10);
        Draw.rbox(ctx, pxx, pyy, pw, ph, Theme.alpha(accent, 0x90), 10);
        Draw.rbox(ctx, pxx + 1, pyy + 1, pw - 2, ph - 2, 0xF008060D, 9);
        ctx.fillGradient(pxx + 5, pyy + 3, pxx + pw - 5, pyy + ph / 2, 0x14FFFFFF, 0x00FFFFFF);
        // nabiz gibi atan nokta
        int pulse = 0x90 + (int) (0x6F * (0.5 + 0.5 * Math.sin(tt * 2.2)));
        Draw.rbox(ctx, pxx + 11, pyy + ph / 2 - 3, 6, 6, Theme.alpha(accent, pulse), 3);
        float tx2 = pxx + 24;
        Fnt.drawTitle(ctx, "Dark", tx2 + 1, pyy + 6, bs, 0x66000000);
        Fnt.drawTitle(ctx, "Dark", tx2, pyy + 5, bs, 0xFFFFFFFF);
        Fnt.drawTitle(ctx, "DLC", tx2 + Fnt.widthTitle("Dark ", bs), pyy + 5, bs, accent);

        if (shown.isEmpty()) return;
        int x = shown.get(0).getX();
        int top = shown.get(0).getY() - 46;
        int bw = shown.get(0).getWidth();
        int lastBottom = shown.get(shown.size() - 1).getY() + 20;

        // cam gibi koyu panel
        Draw.shadow(ctx, x - 16, top - 12, bw + 32, lastBottom - top + 28, 10);
        Draw.rbox(ctx, x - 16, top - 12, bw + 32, lastBottom - top + 28, 0x88050509, 10);
        ctx.fill(x - 16, top - 12 + 6, x - 14, lastBottom + 16 - 6, Theme.alpha(accent, 0xB0));

        // baslik
        Fnt.drawTitle(ctx, "Dark", x + 1, top - 1, 21, 0x66000000);
        Fnt.drawTitle(ctx, "Dark", x, top - 2, 21, 0xFFFFFFFF);
        Fnt.drawTitle(ctx, "DLC", x + Fnt.widthTitle("Dark ", 21), top - 2, 21, accent);
        String ver = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        Fnt.draw(ctx, "Version " + ver, x + 1, top + 24, 8, 0xFF8A8F99);
        Fnt.draw(ctx, LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                x + 1, top + 34, 8, 0xFF8A8F99);

        for (int i = 0; i < shown.size(); i++) {
            ClickableWidget b = shown.get(i);
            int bx = b.getX(), by = b.getY(), bh = b.getHeight();
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
            if (i < hv.length) hv[i] += ((hover ? 1f : 0f) - hv[i]) * 0.25f;
            float f = i < hv.length ? hv[i] : 0f;

            int off = (int) (f * 4);
            rbox(ctx, bx + off, by, bw - off, bh, 0x70181B22);
            if (f > 0.02f) rbox(ctx, bx + off, by, bw - off, bh, Theme.alpha(accent, (int) (0x58 * f)));
            if (f > 0.02f) ctx.fill(bx + off, by + 4, bx + off + 2, by + bh - 4, Theme.alpha(accent, (int) (0xFF * f)));
            rbox(ctx, bx + off + 8, by + bh / 2 - 4, 8, 8, Theme.alpha(accent, 0x90 + (int) (0x6F * f)));
            Fnt.draw(ctx, labels.get(i), bx + off + 24, by + (bh - 9) / 2f, 9, 0xFFFFFFFF, true);
        }

        Fnt.draw(ctx, "Right Shift: GUI", 6, h - 13, 8, 0xFF6A6F78);

        // ---- sag taraf: durum karti (oyuncu, aktif modul, surum)
        String user = mc.getSession().getUsername();
        int cw = Math.max(150, Math.min(210, (int) (w * 0.24f)));
        int chh = 78;
        int cx0 = w - cw - Math.max(24, (int) (w * 0.06f));
        int cy0 = (int) (h / 2f - chh / 2f + Math.sin(tt * 0.8) * 3.0);
        Draw.shadow(ctx, cx0, cy0, cw, chh, 10);
        Draw.rbox(ctx, cx0, cy0, cw, chh, Theme.alpha(accent, 0x50), 10);
        Draw.rbox(ctx, cx0 + 1, cy0 + 1, cw - 2, chh - 2, 0xE8070509, 9);
        ctx.fillGradient(cx0 + 5, cy0 + 3, cx0 + cw - 5, cy0 + 24, Theme.alpha(accent, 0x22), 0x00000000);
        Fnt.drawTitle(ctx, "Dark", cx0 + 12, cy0 + 8, 13, 0xFFFFFFFF);
        Fnt.drawTitle(ctx, "DLC", cx0 + 12 + Fnt.widthTitle("Dark ", 13), cy0 + 8, 13, accent);
        ctx.fillGradient(cx0 + 12, cy0 + 26, cx0 + cw - 12, cy0 + 27, Theme.alpha(accent, 0xFF), Theme.alpha(accent, 0x10));
        Fnt.draw(ctx, "Player", cx0 + 12, cy0 + 34, 8, 0xFF8A8F99);
        Fnt.drawRight(ctx, Fnt.trim(user, 9, cw - 80, true), cx0 + cw - 12, cy0 + 33, 9, 0xFFFFFFFF, true);
        Fnt.draw(ctx, "Modules", cx0 + 12, cy0 + 46, 8, 0xFF8A8F99);
        Fnt.drawRight(ctx, on + " active", cx0 + cw - 12, cy0 + 45, 9, accent, true);
        Fnt.draw(ctx, "Theme", cx0 + 12, cy0 + 58, 8, 0xFF8A8F99);
        Fnt.drawRight(ctx, Theme.cur().name, cx0 + cw - 12, cy0 + 57, 9, 0xFFFFFFFF, true);
    }

    private static void rbox(DrawContext ctx, int x, int y, int w, int h, int color) {
        Draw.rbox(ctx, x, y, w, h, color, 6);
    }
}
