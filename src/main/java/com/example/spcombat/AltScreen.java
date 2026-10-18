package com.example.spcombat;

import com.example.spcombat.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AltManager: takma ad (offline) listesi. Bir isme tiklayinca oyun o isimle devam eder.
 * Sadece "cracked/offline" sunucularda ise yarar (kendi Aternos sunucunda "Cracked" acik olmali).
 * Resmi (online) sunucular ve skin/hesap dogrulamasi bundan etkilenmez, orada eski hesap gecerlidir.
 */
public class AltScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget field;
    private String message = "";

    public AltScreen(Screen parent) {
        super(Text.literal("AltManager"));
        this.parent = parent;
    }

    /** Oyun acilinca son secilen ismi geri yukle. */
    public static void applySaved(MinecraftClient mc) {
        if (Config.currentAlt != null && valid(Config.currentAlt)) apply(mc, Config.currentAlt);
    }

    private static boolean valid(String n) {
        return n != null && n.matches("[A-Za-z0-9_]{3,16}");
    }

    private static void apply(MinecraftClient mc, String name) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        Session s = new Session(name, id, "", Optional.empty(), Optional.empty());
        ((MinecraftClientAccessor) mc).spcSetSession(s);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        field = new TextFieldWidget(this.textRenderer, cx - 100, 44, 140, 20, Text.literal("Name"));
        field.setMaxLength(16);
        this.addDrawableChild(field);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> add())
                .dimensions(cx + 46, 44, 54, 20).build());

        List<String> list = new ArrayList<>(Config.alts);
        String cur = this.client.getSession().getUsername();
        int y = 74;
        for (int i = 0; i < list.size() && i < 10; i++) {
            final String name = list.get(i);
            String label = (name.equals(cur) ? "> " : "") + name;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> use(name))
                    .dimensions(cx - 100, y, 150, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Remove"), b -> remove(name))
                    .dimensions(cx + 54, y, 46, 20).build());
            y += 24;
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.client.setScreen(parent))
                .dimensions(cx - 50, this.height - 30, 100, 20).build());
    }

    private void add() {
        String n = field.getText().trim();
        if (!valid(n)) {
            message = "Isim 3-16 karakter: harf, rakam veya _";
            return;
        }
        Config.alts.add(n);
        Config.save();
        message = "";
        this.clearAndInit();
    }

    private void use(String name) {
        apply(this.client, name);
        Config.currentAlt = name;
        Config.save();
        message = "";
        this.clearAndInit();
    }

    private void remove(String name) {
        Config.alts.remove(name);
        if (name.equals(Config.currentAlt)) Config.currentAlt = "";
        Config.save();
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        int cx = this.width / 2;
        ctx.drawCenteredTextWithShadow(this.textRenderer, "AltManager", cx, 16, Theme.cur().accent);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "Su an: " + this.client.getSession().getUsername(), cx, 30, 0xFFFFFFFF);
        if (!message.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, message, cx, this.height - 44, 0xFFFF6666);
        }
    }
}
