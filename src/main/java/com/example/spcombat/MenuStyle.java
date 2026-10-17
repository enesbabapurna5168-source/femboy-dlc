package com.example.spcombat;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.TranslatableTextContent;

/**
 * Oyun menusundeki "Kaydet ve basliga don" (sunucuda "Baglantiyi kes") butonunu
 * guzellestirir, bir miktar asagi kaydirir ve ustune "femboy DLC" yazar.
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
            btn.setY(btn.getY() + 18); // yazi icin bosluk ac

            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                int accent = Theme.cur().accent;
                int x = btn.getX(), y = btn.getY(), bw = btn.getWidth(), bh = btn.getHeight();
                boolean hover = mx >= x && mx < x + bw && my >= y && my < y + bh;

                ctx.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, accent);
                ctx.fill(x, y, x + bw, y + bh, Theme.shade(accent, hover ? 0.75f : 0.45f));
                ctx.fill(x, y, x + bw, y + bh / 2, hover ? 0x33FFFFFF : 0x22FFFFFF); // ust parlama

                MinecraftClient mc = MinecraftClient.getInstance();
                ctx.drawCenteredTextWithShadow(mc.textRenderer, btn.getMessage(),
                        x + bw / 2, y + (bh - 8) / 2, 0xFFFFFFFF);

                PixelText.drawCentered(ctx, "femboy DLC", x + bw / 2, y - 18, 2, accent);
            });
        });
    }
}
