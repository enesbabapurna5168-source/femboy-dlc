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
 * Ana menu: sol tarafta "Menemen DLC" basligi, surum/tarih ve yuvarlak koseli 4 buton.
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

    private static void draw(Screen s, DrawContext ctx, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int accent = Theme.cur().accent;
        int w = s.width, h = s.height;
        layout(w, h);

        // koyu arka plan (oyunun logosunu ve alt yazilarini da kapatir)
        ctx.fillGradient(0, 0, w, h, 0xFF05080C, 0xFF0A1418);
        ctx.fillGradient(0, h / 2, w, h, 0x00000000, Theme.alpha(accent, 0x38));

        if (shown.isEmpty()) return;
        int x = shown.get(0).getX();
        int top = shown.get(0).getY() - 46;

        // baslik: simge + isim + surum + tarih
        rbox(ctx, x, top + 2, 14, 14, accent);
        ctx.drawTextWithShadow(tr, "Menemen DLC", x + 20, top + 5, 0xFFFFFFFF);
        String ver = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
        ctx.drawTextWithShadow(tr, "Version " + ver, x + 2, top + 22, 0xFF7C8088);
        ctx.drawTextWithShadow(tr, LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                x + 2, top + 33, 0xFF7C8088);

        for (int i = 0; i < shown.size(); i++) {
            ClickableWidget b = shown.get(i);
            int bx = b.getX(), by = b.getY(), bw = b.getWidth(), bh = b.getHeight();
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;

            rbox(ctx, bx, by, bw, bh, 0x55252C34);
            if (hover) rbox(ctx, bx, by, bw, bh, Theme.alpha(accent, 0x40));
            rbox(ctx, bx + 8, by + bh / 2 - 4, 8, 8, hover ? accent : Theme.alpha(accent, 0xB0));
            ctx.drawTextWithShadow(tr, labels.get(i), bx + 24, by + (bh - 8) / 2, 0xFFFFFFFF);
        }
    }

    private static void rbox(DrawContext ctx, int x, int y, int w, int h, int color) {
        Draw.rbox(ctx, x, y, w, h, color, 6);
    }
}
