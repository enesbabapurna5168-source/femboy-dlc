package com.example.spcombat;

import com.example.spcombat.Module.Category;

import java.util.List;

public final class Modules {
    private Modules() {}

    // =========================== COMBAT ===========================
    // Menzil: sunucu etkilesim siniri yaklasik 4 blok, bu yuzden max 4.
    public static final Module AURA = new Module("AttackAura", Category.COMBAT,
            Setting.num("Range", 3.0, 1, 4.0, 0.5),
            Setting.num("Aim range", 4.0, 1, 8.0, 0.5),
            Setting.list("Targets", 0, "Monsters", "Mobs", "All"),
            Setting.bool("Players", false),
            Setting.list("Sorting", 0, "Closest", "Lowest HP", "FOV"),
            Setting.num("FOV", 360, 30, 360, 10),
            Setting.list("Rotate", 3, "Off", "Snap", "Smooth", "Silent"),
            Setting.num("Smooth speed", 25, 5, 60, 5),
            Setting.bool("Predict", true),
            Setting.num("Switch delay (tick)", 6, 0, 20, 1),
            Setting.bool("Through walls", false),
            Setting.bool("Break shield", true),
            Setting.num("Cooldown %", 100, 50, 100, 5),
            Setting.bool("Crit jump", true),
            Setting.bool("Crits only", false),
            Setting.bool("Space only", false),
            Setting.bool("Pause on use", true),
            Setting.bool("Stop in GUI", true));

    public static final Module TRIGGER = new Module("TriggerBot", Category.COMBAT,
            Setting.list("Targets", 0, "Monsters", "Mobs", "All"),
            Setting.num("Cooldown %", 100, 50, 100, 5),
            Setting.bool("Crits only", false));

    public static final Module TOTEM = new Module("AutoTotem", Category.COMBAT,
            Setting.list("Mode", 0, "Always", "Below HP"),
            Setting.num("Health (hp)", 10, 2, 20, 1),
            Setting.num("Delay (tick)", 1, 0, 20, 1));

    public static final Module POTION = new Module("AutoPotion", Category.COMBAT,
            Setting.num("Health (hp)", 8, 2, 18, 1),
            Setting.num("Delay (tick)", 10, 0, 40, 1));

    /** Can azalinca hotbar'daki altin elmayi yer, bitince eski slota doner. */
    public static final Module AUTOGAPPLE = new Module("AutoGApple", Category.COMBAT,
            Setting.num("Health (hp)", 10, 2, 19, 1));

    /** Bind tusuna basinca hotbar'daki ender incisini atar. */
    public static final Module CLICKPEARL = new Module("ClickPearl", Category.COMBAT,
            Setting.num("Cooldown (tick)", 20, 0, 100, 1)).on().action();

    // =========================== MOVEMENT ===========================
    public static final Module AUTOSPRINT = new Module("AutoSprint", Category.MOVEMENT);

    public static final Module SPEED = new Module("Speed", Category.MOVEMENT,
            Setting.num("Multiplier", 1.3, 1.0, 3.0, 0.1),
            Setting.bool("Only on ground", true));

    public static final Module ELYTRA = new Module("ElytraTarget", Category.MOVEMENT,
            Setting.num("Range", 50, 10, 100, 5),
            Setting.list("Targets", 0, "Monsters", "Mobs", "All"),
            Setting.bool("Players", false),
            Setting.bool("Lead target", true),
            Setting.num("Speed (deg/tick)", 18, 2, 40, 1),
            Setting.num("Min distance", 2, 0, 10, 1));

    /** Elinde kilic varken bind tusuna basinca fisek atar. */
    public static final Module ELYHELPER = new Module("ElytraHelper", Category.MOVEMENT,
            Setting.bool("Require sword", true),
            Setting.bool("Only while gliding", true),
            Setting.bool("Hold to repeat", false),
            Setting.num("Cooldown (tick)", 10, 0, 40, 1)).on().action();

    /** Hiz esigin altina dusunce kendisi fisek atar. */
    public static final Module ELYBOOSTER = new Module("ElytraBooster", Category.MOVEMENT,
            Setting.num("Min speed (b/s)", 20, 5, 60, 5),
            Setting.num("Cooldown (tick)", 20, 5, 60, 1));

    /** Freeze: oldugun yerde dondurur. Hover: sadece dusmeyi durdurur. */
    public static final Module AIRSTUCK = new Module("AirStuck", Category.MOVEMENT,
            Setting.list("Mode", 0, "Freeze", "Hover"),
            Setting.bool("Only in air", true));

    /** Knockback'i azaltir ya da tamamen iptal eder (0% = hic geri tepme). */
    public static final Module VELOCITY = new Module("Velocity", Category.MOVEMENT,
            Setting.num("Horizontal %", 0, 0, 100, 10),
            Setting.num("Vertical %", 0, 0, 100, 10));

    /** Envanter/sandik acikken yurumeye devam et (W A S D + Space). */
    public static final Module GUIMOVE = new Module("GuiMove", Category.MOVEMENT);

    // =========================== RENDER ===========================
    public static final Module FULLBRIGHT = new Module("FullBright", Category.RENDER);

    /** Gokyuzune yumusak renk tonu verir. */
    public static final Module AMBIENCE = new Module("Ambience", Category.RENDER,
            Setting.list("Style", 0, "Sunset", "Twilight", "Pastel"),
            Setting.num("Intensity %", 40, 10, 100, 10)).on().cosmetic();

    /** Vurdugunda guzel bir ses calar. */
    public static final Module CLIENTSOUND = new Module("ClientSound", Category.RENDER,
            Setting.list("Sound", 0, "Ding", "Orb", "Amethyst", "Level"),
            Setting.num("Volume %", 80, 10, 100, 10),
            Setting.num("Pitch %", 100, 50, 200, 10)).on().cosmetic();

    public static final Module HUD = new Module("HUD", Category.RENDER,
            Setting.bool("Watermark", true),
            Setting.bool("Module list", false),
            Setting.bool("Coords", true)).on().cosmetic();

    /** Ust ortada zirh dayaniklilik paneli. */
    public static final Module ARMORHUD = new Module("ArmorHUD", Category.RENDER).on().cosmetic();

    /** Saldirdigin hedefin can gostergesi. */
    public static final Module TARGETHUD = new Module("TargetHUD", Category.RENDER).on().cosmetic();

    /** Saldirdigin / aura'nin sectigi hedefi parlatir (oyunun kendi parlama efekti, FPS'e etkisi yok). */
    public static final Module TARGETESP = new Module("TargetESP", Category.RENDER,
            Setting.list("Mode", 0, "Ring", "Glow", "Both"),
            Setting.num("Ring size %", 100, 50, 200, 10)).on().cosmetic();

    /** Karaktere gercek elytra kanatlari cizer (sadece gorsel, envanter degismez). */
    public static final Module WINGS = new Module("Wings", Category.RENDER).cosmetic();

    /** Acik modullerin adini ve tuslarini ekranin kosesinde kucuk bir panelde gosterir. */
    public static final Module KEYBINDS = new Module("Keybinds", Category.RENDER,
            Setting.list("Side", 1, "Left", "Right"),
            Setting.bool("Show keys", true)).on().cosmetic();

    /** Acikken GUI'nin altinda resimler gorunur. */
    public static final Module PHOTO = new Module("Photo", Category.RENDER,
            Setting.num("Size", 90, 40, 160, 10)).cosmetic();

    // =========================== PLAYER ===========================
    /** Blok kirmayi hizlandirir: bloklar arasi beklemeyi kaldirir, kirma ilerlemesini hizlandirir. */
    public static final Module FASTBREAK = new Module("FastBreak", Category.PLAYER,
            Setting.num("Speed", 1.4, 1.0, 1.4, 0.1),
            Setting.bool("No delay", true));

    /** Blok kirarken o blok icin en hizli aleti hotbar'dan secer. */
    public static final Module AUTOTOOL = new Module("AutoTool", Category.PLAYER);

    /** Envanterdeki daha iyi zirhi otomatik giyer. */
    public static final Module AUTOARMOR = new Module("AutoArmor", Category.PLAYER,
            Setting.num("Delay (tick)", 2, 0, 20, 1));

    /** Sandik acilinca icindekileri otomatik alir. */
    public static final Module CHESTSTEALER = new Module("ChestStealer", Category.PLAYER,
            Setting.num("Delay (tick)", 1, 0, 10, 1),
            Setting.bool("Auto close", true));

    // =========================== MISC ===========================
    /** Ayagin altindaki bos yere hotbar'daki blogu koyar. */
    public static final Module SCAFFOLD = new Module("Scaffold", Category.MISC,
            Setting.bool("Switch back", true));

    public static final Module AUTORESPAWN = new Module("AutoRespawn", Category.MISC);

    public static final List<Module> ALL = List.of(
            AURA, TRIGGER, TOTEM, POTION, AUTOGAPPLE, CLICKPEARL,
            AUTOSPRINT, SPEED, ELYTRA, ELYHELPER, ELYBOOSTER, AIRSTUCK, VELOCITY, GUIMOVE,
            FULLBRIGHT, AMBIENCE, CLIENTSOUND, HUD, ARMORHUD, TARGETHUD, KEYBINDS, PHOTO, TARGETESP, WINGS,
            AUTOTOOL, FASTBREAK, AUTOARMOR, CHESTSTEALER,
            SCAFFOLD, AUTORESPAWN);

    public static List<Module> byCategory(Category c) {
        return ALL.stream().filter(m -> m.category == c).toList();
    }
}
