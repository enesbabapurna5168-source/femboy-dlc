package com.example.spcombat;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Ayarlari .minecraft/config/spcombat.properties dosyasina kaydeder/yukler:
 * modul acik/kapali durumu, atanan tuslar, tum ayarlar ve secili tema.
 */
public final class Config {
    private Config() {}

    /** Modullerin calismasina izin verilen sunucu adresleri (kucuk harf). */
    public static final Set<String> servers = new LinkedHashSet<>();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spcombat.properties");
    }

    private static String clean(String s) {
        return s.replaceAll("[^A-Za-z0-9]+", "_");
    }

    private static String key(Module m, String suffix) {
        return clean(m.name) + "." + clean(suffix);
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;

        Properties pr = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            pr.load(in);
        } catch (IOException e) {
            return;
        }

        for (Module m : Modules.ALL) {
            String en = pr.getProperty(key(m, "enabled"));
            if (en != null) m.enabled = Boolean.parseBoolean(en);

            String b = pr.getProperty(key(m, "bind"));
            if (b != null) {
                try {
                    int k = Integer.parseInt(b);
                    m.bind = (k >= 0 && Keys.valid(k)) ? k : -1;
                } catch (NumberFormatException ignored) {
                }
            }

            for (Setting s : m.settings) {
                String v = pr.getProperty(key(m, "s_" + s.name));
                if (v != null) s.deserialize(v);
            }
        }

        String sv = pr.getProperty("servers");
        if (sv != null) {
            for (String part : sv.split(",")) {
                String a = part.trim().toLowerCase();
                if (!a.isEmpty()) servers.add(a);
            }
        }

        String t = pr.getProperty("theme");
        if (t != null) {
            try {
                Theme.set(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static void save() {
        Properties pr = new Properties();
        for (Module m : Modules.ALL) {
            pr.setProperty(key(m, "enabled"), String.valueOf(m.enabled));
            pr.setProperty(key(m, "bind"), String.valueOf(m.bind));
            for (Setting s : m.settings) {
                pr.setProperty(key(m, "s_" + s.name), s.serialize());
            }
        }
        pr.setProperty("theme", String.valueOf(Theme.index()));
        pr.setProperty("servers", String.join(",", servers));

        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (OutputStream out = Files.newOutputStream(f)) {
                pr.store(out, "Menemen Client");
            }
        } catch (IOException ignored) {
        }
    }
}
