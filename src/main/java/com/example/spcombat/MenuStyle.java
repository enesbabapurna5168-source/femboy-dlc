package com.example.spcombat;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.TranslatableTextContent;

/**
 * Oyun menusundeki "Kaydet ve basliga don" (sunucuda "Baglantiyi kes") butonunu
 * guzellestirir, bir miktar asagi kaydirir ve ustune "Menemen DLC" yazar.
 * Butonun kendisi (tiklama davranisi) degismez, ustune renkli bir katman cizilir.
 * Renkler secili temadan gelir.
 */
public final class MenuStyle {
    private MenuStyle() {}

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof GameMenuScreen)) return;

            ClickableWidget found = null;
            for (ClickableWidget b : Screens.getButtons(screen)) {
                if (b.getMessage().getContent() instanceof TranslatableTextContent t) {
                    String k = t.getKey();
                    if (k.equals("menu.returnToMenu") || k.equals("menu.disconnect")) found = b;
                }
            }
            if (found == null) return;

            final ClickableWidget btn = found;
            btn.setY(btn.getY() + 34); // yazi icin bosluk ac

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                int accent = Theme.cur().accent;
                int x = btn.getX(), y = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
                boolean hover = mx >= x && mx < x + bw && my >= y && my < y + bh;

                ctx.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, accent);
                ctx.fill(x, y, x + bw, y + bh, Theme.shade(accent, hover ? 0.75f : 0.45f));
                ctx.fill(x, y, x + bw, y + bh / 2, hover ? 0x33FFFFFF : 0x22FFFFFF); // ust parlama

                MinecraftClient mc = MinecraftClient.getInstance();
                Fnt.drawCentered(ctx, btn.getMessage().getString(), x + bw / 2f, y + (bh - 9) / 2f, 9, 0xFFFFFFFF, true);

                // ustte buyuk "Menemen DLC" basligi
                float cx = x + bw / 2f;
                float tw = Fnt.width("Menemen DLC", 16, true);
                Draw.rbox(ctx, (int) (cx - tw / 2 - 12), y - 30, (int) tw + 24, 24, 0x80080B10, 8);
                Draw.rbox(ctx, (int) (cx - tw / 2 - 12), y - 8, (int) tw + 24, 2, Theme.alpha(accent, 0xE0), 1);
                Fnt.draw(ctx, "Menemen", cx - tw / 2, y - 27, 16, 0xFFFFFFFF, true);
                Fnt.draw(ctx, "DLC", cx - tw / 2 + Fnt.width("Menemen ", 16, true), y - 27, 16, accent, true);
            });
        });
    }
}
