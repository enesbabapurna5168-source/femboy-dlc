package com.example.spcombat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

/**
 * Savas, hareket ve gorsel yardimcilar. Tek oyunculu dunyalarda her zaman calisir;
 * sunucuda sadece GUI'den "Bu sunucuda izin: ACIK" yaptigin adreslerde calisir
 * (bkz. Access). Izinsiz sunucuda gorsel/ses moduller (HUD, Ambience, ClientSound,
 * ArmorHUD, TargetHUD) disindakiler kapanir ve calismaz.
 *
 * Sag Shift: GUI (Kontroller menusunden degistirilebilir).
 * Modul tuslari GUI'deki "Bind" satirindan atanir; hicbir modulde varsayilan tus yok.
 */
public class SpCombatClient implements ClientModInitializer {

    /** Sunucunun saldiri icin kabul ettigi en fazla mesafe. */
    private static final double REACH_LIMIT = 4.0;

    private static KeyBinding keyGui;

    /** HUD paneli icin: son hedef ve ne zaman vuruldugu. */
    public static int ticks;
    public static LivingEntity hudTarget;
    public static int hudTargetTick;

    private static LivingEntity auraLock;      // AttackAura: kilitli hedef
    private static int auraLockTick;
    private static LivingEntity espGlowing;    // TargetESP: parlattigimiz entity
    private static int totemCd, potionCd, helperCd, boosterCd, pearlCd, armorCd, stealCd;
    private static Vec3d frozen;               // AirStuck (Freeze)
    private static boolean nightVisionApplied; // FullBright
    private static int toolPrev = -1;          // AutoTool: onceki slot
    private static boolean eating;             // AutoGApple durumu
    private static int eatTicks, eatPrevSlot = -1;
    private static boolean stealStarted;       // ChestStealer: bu sandikta bir sey alindi mi

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("spcombat", "main"));
        keyGui = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.spcombat.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));

        Hud.init();
        Ambience.init();
        HitSound.init();
        MenuStyle.init();
        ClientTickEvents.END_CLIENT_TICK.register(SpCombatClient::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Config.save());
    }

    // ==================================================================
    //  Ana dongu
    // ==================================================================
    private static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            frozen = null;
            nightVisionApplied = false;
            eating = false;
            toolPrev = -1;
            stealStarted = false;
            espGlowing = null;
            auraLock = null;
            return;
        }
        ticks++;
        targetEsp();

        // FullBright kapandiysa efekti kaldir (her durumda)
        if (nightVisionApplied && !Modules.FULLBRIGHT.enabled) {
            p.removeStatusEffect(StatusEffects.NIGHT_VISION);
            nightVisionApplied = false;
        }
        if (frozen != null && !Modules.AIRSTUCK.enabled) frozen = null;

        // GUI her yerde acilabilir
        while (keyGui.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        }

        // Tek oyunculu her zaman; sunucuda sadece GUI'den izin verilen adreslerde
        boolean sp = Access.allowed(mc);
        handleBinds(mc, p, sp);

        // Izinsiz sunucuda: gorsel/ses/eylem disindaki her sey kapanir, modul kodu calismaz
        if (!sp) {
            for (Module m : Modules.ALL) {
                if (!m.cosmetic && !m.actionKey) m.enabled = false;
            }
            if (eating) { mc.options.useKey.setPressed(false); eating = false; }
            toolPrev = -1;
            return;
        }

        if (Modules.AUTORESPAWN.enabled && mc.currentScreen instanceof DeathScreen) {
            p.requestRespawn();
        }

        // Oyuncu oluyse ya da bir ekran (envanter, menu) aciksa savas/hareket modulleri durur
        boolean idle = !p.isAlive() || mc.currentScreen != null;

        // Ekran acikken de calisanlar
        if (Modules.GUIMOVE.enabled) guiMove(mc);
        if (Modules.AUTOARMOR.enabled) autoArmor(mc, p);
        if (Modules.TOTEM.enabled) autoTotem(mc, p);
        if (Modules.CHESTSTEALER.enabled) chestStealer(mc, p);

        // Eylem tuslari (bind tetikler)
        elytraHelper(mc, p);
        clickPearl(mc, p);

        if (eating && (idle || !Modules.AUTOGAPPLE.enabled)) stopEating(mc, p);

        if (!idle) {
            if (Modules.AURA.enabled) attackAura(mc, p);
            if (Modules.TRIGGER.enabled) triggerBot(mc, p);
            if (Modules.POTION.enabled) autoPotion(mc, p);
            if (Modules.AUTOGAPPLE.enabled) autoGApple(mc, p);
            if (Modules.ELYTRA.enabled) elytraTarget(mc, p);
            if (Modules.ELYBOOSTER.enabled) elytraBooster(mc, p);
            if (Modules.AUTOSPRINT.enabled) autoSprint(mc, p);
            if (Modules.SPEED.enabled) speed(mc, p);
            if (Modules.AUTOTOOL.enabled) autoTool(mc, p);
            else if (toolPrev >= 0) toolPrev = -1;
            if (Modules.SCAFFOLD.enabled) scaffold(mc, p);
        }
        if (Modules.AIRSTUCK.enabled) airStuck(p);
        if (Modules.FULLBRIGHT.enabled) fullbright(p);
        // Velocity: mixin/VelocityMixin.java icinde
    }

    // TargetESP: son hedefi oyunun parlama (glow) efektiyle isaretler
    private static void targetEsp() {
        LivingEntity want = null;
        if (Modules.TARGETESP.enabled) {
            LivingEntity t = hudTarget;
            if (t != null && t.isAlive() && ticks - hudTargetTick <= 60) want = t;
        }
        if (espGlowing != null && espGlowing != want) {
            espGlowing.setGlowing(false);
            espGlowing = null;
        }
        if (want != null) {
            want.setGlowing(true);
            espGlowing = want;
        }
    }

    // ==================================================================
    //  Tus atamalari (GUI'deki Bind satiri)
    //  Aksiyon modulleri bind'a basilinca tetiklenir, digerleri ac/kapat olur.
    // ==================================================================
    private static void handleBinds(MinecraftClient mc, ClientPlayerEntity p, boolean sp) {
        long win = mc.getWindow().getHandle();
        for (Module m : Modules.ALL) {
            if (m.bind < 0) { m.bindDown = false; continue; }

            boolean down = mc.currentScreen == null && GLFW.glfwGetKey(win, m.bind) == GLFW.GLFW_PRESS;
            boolean edge = down && !m.bindDown;
            m.bindDown = down;
            if (!edge) continue;

            if (m.actionKey) {
                if (sp) m.trigger();
                else p.sendMessage(Text.literal("Bu sunucuda izin yok (GUI'den izin ver)."), true);
                continue;
            }
            if (!sp && !m.cosmetic) {
                p.sendMessage(Text.literal("Bu sunucuda izin yok (GUI'den izin ver)."), true);
                continue;
            }
            m.enabled = !m.enabled;
            p.sendMessage(Text.literal(m.name + ": " + (m.enabled ? "ACIK" : "KAPALI")), true);
        }
    }

    // ==================================================================
    //  Ortak yardimcilar
    // ==================================================================

    /** Hedef turu: 0 = Monsters, 1 = Mobs, 2 = All. Oyuncular, pet'ler, koylu/tuccar ve golemler haric. */
    private static boolean valid(LivingEntity e, ClientPlayerEntity p, int mode) {
        if (e == p || !e.isAlive()) return false;
        if (e instanceof PlayerEntity || e instanceof ArmorStandEntity || e instanceof MerchantEntity) return false;
        if (e instanceof TameableEntity t && t.isTamed()) return false;
        if (e instanceof IronGolemEntity) return false;
        switch (mode) {
            case 0:  return e instanceof Monster;
            case 1:  return e instanceof MobEntity;
            default: return true;
        }
    }

    /** Gozden hedefin vurulabilir kutusuna en yakin noktaya mesafenin karesi (oyunun kendi olcusu). */
    private static double reachSq(ClientPlayerEntity p, LivingEntity e) {
        Vec3d eye = p.getEyePos();
        Box b = e.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.squaredDistanceTo(x, y, z);
    }

    /** Arada blok yok mu? (duvar arkasindaki hedefe vurmamak icin) */
    private static boolean visible(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t) {
        Vec3d from = p.getEyePos();
        Vec3d[] points = { t.getBoundingBox().getCenter(), t.getEyePos() };
        for (Vec3d to : points) {
            BlockHitResult r = mc.world.raycast(new RaycastContext(
                    from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            if (r.getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    /**
     * Menzildeki en uygun hedef.
     * @param reach     true: gozden kutuya mesafe (saldiri icin), false: merkezden merkeze (uzak hedef icin)
     * @param lowHp     true: en dusuk cani olan, false: en yakin
     * @param needSight true: duvar arkasindakileri atla
     */
    private static LivingEntity pick(MinecraftClient mc, ClientPlayerEntity p, double range, int mode,
                                     boolean reach, boolean lowHp, boolean needSight) {
        Box box = p.getBoundingBox().expand(range + 2.0);
        double limit = range * range;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        float bestHp = Float.MAX_VALUE;

        for (LivingEntity t : mc.world.getEntitiesByClass(LivingEntity.class, box, e -> valid(e, p, mode))) {
            double d = reach ? reachSq(p, t) : p.squaredDistanceTo(t);
            if (d > limit) continue;

            boolean better;
            if (lowHp) {
                float hp = t.getHealth();
                better = hp < bestHp || (hp == bestHp && d < bestDist);
            } else {
                better = d < bestDist;
            }
            if (!better) continue;
            if (needSight && !visible(mc, p, t)) continue;

            best = t;
            bestDist = d;
            bestHp = t.getHealth();
        }
        return best;
    }

    /** {yaw, pitch} - oyuncunun gozunden hedef noktaya bakis acisi. */
    private static float[] angles(ClientPlayerEntity p, Vec3d to) {
        Vec3d from = p.getEyePos();
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{yaw, MathHelper.clamp(pitch, -90f, 90f)};
    }

    /** Havadaysa ve dusuyorsa vurus kritik olur. */
    private static boolean critReady(ClientPlayerEntity p) {
        return !p.isOnGround() && p.getVelocity().y < 0 && !p.isClimbing()
                && !p.isTouchingWater() && !p.hasVehicle();
    }

    private static boolean cooldownReady(ClientPlayerEntity p, Module m) {
        return p.getAttackCooldownProgress(0.5f) >= (float) (m.num("Cooldown %") / 100.0);
    }

    private static void hit(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t) {
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        hudTarget = t;
        hudTargetTick = ticks;
    }

    /** Hotbar'dan (yoksa sol elden) esyayi kullanir, sonra eski slota doner. */
    private static boolean useFromHotbar(MinecraftClient mc, ClientPlayerEntity p, Item item) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (!inv.getStack(i).isOf(item)) continue;
            int prev = inv.getSelectedSlot();
            inv.setSelectedSlot(i);
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            p.swingHand(Hand.MAIN_HAND);
            inv.setSelectedSlot(prev);
            return true;
        }
        if (p.getOffHandStack().isOf(item)) {
            mc.interactionManager.interactItem(p, Hand.OFF_HAND);
            p.swingHand(Hand.OFF_HAND);
            return true;
        }
        return false;
    }

    private static int findHotbar(PlayerInventory inv, Item a, Item b) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(a) || s.isOf(b)) return i;
        }
        return -1;
    }

    // ==================================================================
    //  COMBAT
    // ==================================================================

    // ---- AttackAura: hedefi bul -> (istenirse) ona don -> cooldown/crit hazirsa vur.
    //      Aim range, vurus menzilinden (Range) genis: hedefe once doner, menzile girince vurur.
    private static void attackAura(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.AURA;
        if (m.bool("Pause on use") && p.isUsingItem()) return;
        if (m.bool("Stop in GUI") && mc.currentScreen != null) return;

        double range = Math.min(m.num("Range"), REACH_LIMIT);
        double aim = Math.max(range, m.num("Aim range"));
        int hold = (int) m.num("Switch delay (tick)");
        LivingEntity t;
        if (hold > 0 && auraLock != null && auraLock.isAlive() && ticks - auraLockTick < hold
                && reachSq(p, auraLock) <= aim * aim) {
            t = auraLock;                                   // gecikme dolana kadar ayni hedefte kal
        } else {
            t = auraPick(mc, p, aim, m);
            if (t != auraLock) { auraLock = t; auraLockTick = ticks; }
        }
        if (t == null) return;
        hudTarget = t;                                      // TargetHUD / TargetESP
        hudTargetTick = ticks;

        int rot = m.index("Rotate");
        if (rot != 0) {
            Vec3d aimAt = t.getBoundingBox().getCenter();
            if (m.bool("Predict")) {                          // hedefin hareket yonune 2 tick onden bak
                aimAt = aimAt.add((t.getX() - t.prevX) * 2.0, 0, (t.getZ() - t.prevZ) * 2.0);
            }
            float[] a = angles(p, aimAt);
            float dYaw = MathHelper.wrapDegrees(a[0] - p.getYaw());
            float dPitch = a[1] - p.getPitch();

            if (rot == 1) {                                   // Snap
                p.setYaw(a[0]);
                p.setPitch(a[1]);
            } else {                                          // Smooth
                float sp = (float) m.num("Smooth speed");
                p.setYaw(p.getYaw() + MathHelper.clamp(dYaw, -sp, sp));
                p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch, -sp, sp), -90f, 90f));
                if (Math.abs(dYaw) > 30f || Math.abs(dPitch) > 30f) return; // henuz bakmiyor
            }
        }

        if (reachSq(p, t) > range * range) return;           // sadece hedefe donuyor, menzile girince vurur
        if (!cooldownReady(p, m)) return;
        if (m.bool("Crits only") && !critReady(p)) return;
        if (m.bool("Space only") && !mc.options.jumpKey.isPressed()) return;

        // Hedef kalkan tutuyorsa hotbar'daki baltayla kirar, sonra eski slota doner
        if (m.bool("Break shield") && t instanceof PlayerEntity pl && pl.isBlocking()) {
            PlayerInventory inv = p.getInventory();
            for (int i = 0; i < 9; i++) {
                if (inv.getStack(i).isIn(ItemTags.AXES)) {
                    int prev = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    hit(mc, p, t);
                    inv.setSelectedSlot(prev);
                    return;
                }
            }
        }
        hit(mc, p, t);
    }

    /** AttackAura hedef secimi: Targets + Players, Sorting, FOV ve duvar arkasi kontrolu. */
    private static LivingEntity auraPick(MinecraftClient mc, ClientPlayerEntity p, double range, Module m) {
        boolean players = m.bool("Players");
        int mode = m.index("Targets");
        int sort = m.index("Sorting");
        double fov = m.num("FOV");
        boolean needSight = !m.bool("Through walls");

        Box box = p.getBoundingBox().expand(range + 2.0);
        double limit = range * range;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity t : mc.world.getEntitiesByClass(LivingEntity.class, box, e -> {
            if (e == p || !e.isAlive()) return false;
            if (e instanceof PlayerEntity) return players && !e.isSpectator();
            return valid(e, p, mode);
        })) {
            double d = reachSq(p, t);
            if (d > limit) continue;

            float[] a = angles(p, t.getBoundingBox().getCenter());
            float off = Math.abs(MathHelper.wrapDegrees(a[0] - p.getYaw()));
            if (fov < 360 && off > fov / 2.0) continue;

            double score;
            if (sort == 1) score = t.getHealth() * 1000.0 + d;
            else if (sort == 2) score = off * 1000.0 + d;
            else score = d;
            if (score >= bestScore) continue;
            if (needSight && !visible(mc, p, t)) continue;

            best = t;
            bestScore = score;
        }
        return best;
    }

    // ---- TriggerBot: nisangah bir hedefin ustundeyken vurur.
    private static void triggerBot(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.TRIGGER;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !valid(le, p, m.index("Targets"))) return;
        if (reachSq(p, le) > REACH_LIMIT * REACH_LIMIT) return;
        if (!cooldownReady(p, m)) return;
        if (m.bool("Crits only") && !critReady(p)) return;

        hit(mc, p, le);
    }

    // ---- AutoTotem: sol elde totem yoksa envanterden koyar. Envanter acikken de calisir.
    private static void autoTotem(MinecraftClient mc, ClientPlayerEntity p) {
        if (totemCd > 0) { totemCd--; return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;

        Module m = Modules.TOTEM;
        if (m.index("Mode") == 1 && p.getHealth() > (float) m.num("Health (hp)")) return;

        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int screenSlot = i < 9 ? i + 36 : i;   // hotbar 0-8 => 36-44
                // SWAP + buton 40 = sol el ile degistir
                mc.interactionManager.clickSlot(
                        p.playerScreenHandler.syncId, screenSlot, 40, SlotActionType.SWAP, p);
                totemCd = (int) m.num("Delay (tick)");
                return;
            }
        }
    }

    // ---- AutoPotion: can esiginin altinda healing splash atar (once sol el, sonra hotbar).
    private static void autoPotion(MinecraftClient mc, ClientPlayerEntity p) {
        if (potionCd > 0) { potionCd--; return; }
        if (p.getHealth() > (float) Modules.POTION.num("Health (hp)")) return;

        if (isHealingSplash(p.getOffHandStack())) {
            throwDown(mc, p, Hand.OFF_HAND, -1);
            potionCd = (int) Modules.POTION.num("Delay (tick)");
            return;
        }

        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (isHealingSplash(inv.getStack(i))) {
                throwDown(mc, p, Hand.MAIN_HAND, i);
                potionCd = (int) Modules.POTION.num("Delay (tick)");
                return;
            }
        }
    }

    /** hotbarSlot < 0 ise slot degistirmez (sol el). */
    private static void throwDown(MinecraftClient mc, ClientPlayerEntity p, Hand hand, int hotbarSlot) {
        PlayerInventory inv = p.getInventory();
        int prevSlot = inv.getSelectedSlot();
        float prevPitch = p.getPitch();

        if (hotbarSlot >= 0) inv.setSelectedSlot(hotbarSlot);
        p.setPitch(90f);                                  // asagi bak: potion ayaklarina dussun
        mc.interactionManager.interactItem(p, hand);
        p.swingHand(hand);
        if (hotbarSlot >= 0) inv.setSelectedSlot(prevSlot);
        p.setPitch(prevPitch);
    }

    private static boolean isHealingSplash(ItemStack s) {
        if (!s.isOf(Items.SPLASH_POTION)) return false;
        PotionContentsComponent c = s.get(DataComponentTypes.POTION_CONTENTS);
        if (c == null) return false;
        for (StatusEffectInstance e : c.getEffects()) {
            if (e.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) return true;
        }
        return false;
    }

    // ---- AutoGApple: can esigin altinda hotbar'daki altin elmayi yer, bitince eski slota doner.
    private static void autoGApple(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.AUTOGAPPLE;
        boolean low = p.getHealth() <= (float) m.num("Health (hp)");
        PlayerInventory inv = p.getInventory();

        if (!eating) {
            if (!low) return;
            int slot = findHotbar(inv, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
            if (slot < 0) return;
            eatPrevSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(slot);
            mc.options.useKey.setPressed(true);
            eating = true;
            eatTicks = 0;
            return;
        }

        eatTicks++;
        boolean finished = eatTicks > 3 && !p.isUsingItem(); // yeme bitti
        if (finished || eatTicks > 80) stopEating(mc, p);
    }

    private static void stopEating(MinecraftClient mc, ClientPlayerEntity p) {
        mc.options.useKey.setPressed(false);
        if (eatPrevSlot >= 0) p.getInventory().setSelectedSlot(eatPrevSlot);
        eatPrevSlot = -1;
        eating = false;
    }

    // ---- ClickPearl: bind tusuna basinca ender incisi atar.
    private static void clickPearl(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.CLICKPEARL;
        if (pearlCd > 0) pearlCd--;
        boolean pressed = m.consume();
        if (!pressed || !m.enabled || pearlCd > 0 || mc.currentScreen != null) return;

        if (useFromHotbar(mc, p, Items.ENDER_PEARL)) {
            pearlCd = (int) m.num("Cooldown (tick)");
        } else {
            p.sendMessage(Text.literal("Ender incisi bulunamadi (hotbar veya sol el)."), true);
        }
    }

    // ==================================================================
    //  MOVEMENT
    // ==================================================================

    // ---- AutoSprint
    private static void autoSprint(MinecraftClient mc, ClientPlayerEntity p) {
        if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                && !p.isUsingItem()
                && (p.getHungerManager().getFoodLevel() > 6 || p.getAbilities().allowFlying)) {
            p.setSprinting(true);
        }
    }

    // ---- Speed: yerdeki hizi kademeli artirir, sprint hizinin Multiplier kati ile sinirlar.
    private static void speed(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.SPEED;
        if (p.isGliding() || p.hasVehicle() || p.isClimbing() || p.isTouchingWater() || p.isInLava()) return;
        if (m.bool("Only on ground") && !p.isOnGround()) return;

        boolean moving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (!moving) return;

        Vec3d v = p.getVelocity();
        double h = Math.hypot(v.x, v.z);
        if (h < 0.01) return;

        double cap = 0.2873 * m.num("Multiplier");
        double nh = Math.min(h * 1.15, cap);
        if (nh <= h) return;

        double f = nh / h;
        p.setVelocity(v.x * f, v.y, v.z * f);
    }

    // ---- ElytraTarget: ucarken en yakin hedefe oransal (yumusak) sekilde doner.
    //      Min distance'in icine girince birakir ki AttackAura isini yapsin.
    private static void elytraTarget(MinecraftClient mc, ClientPlayerEntity p) {
        if (!p.isGliding()) return;
        Module m = Modules.ELYTRA;

        LivingEntity t = pick(mc, p, m.num("Range"), m.index("Targets"), false, false, false);
        if (t == null) return;
        if (Math.sqrt(p.squaredDistanceTo(t)) < m.num("Min distance")) return;

        float[] a = angles(p, t.getBoundingBox().getCenter());
        float speed = (float) m.num("Speed (deg/tick)");
        float dYaw = MathHelper.wrapDegrees(a[0] - p.getYaw());
        float dPitch = a[1] - p.getPitch();

        // Fark buyukken hizli, kuculdukce yavas: titremez
        float stepYaw = MathHelper.clamp(dYaw * 0.4f, -speed, speed);
        float stepPitch = MathHelper.clamp(dPitch * 0.4f, -speed, speed);
        p.setYaw(p.getYaw() + stepYaw);
        p.setPitch(MathHelper.clamp(p.getPitch() + stepPitch, -90f, 90f));
    }

    // ---- ElytraHelper: bind tusuna basinca (kilic elindeyken) fisek atar.
    //      Hold to repeat: tus basili kaldikca tekrarlar.
    private static void elytraHelper(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.ELYHELPER;
        if (helperCd > 0) helperCd--;

        boolean pressed = m.consume();
        if (m.bool("Hold to repeat") && m.bindDown) pressed = true;
        if (!pressed || !m.enabled || helperCd > 0 || mc.currentScreen != null) return;

        if (m.bool("Only while gliding") && !p.isGliding()) return;
        if (m.bool("Require sword") && !p.getMainHandStack().isIn(ItemTags.SWORDS)) return;

        if (useFromHotbar(mc, p, Items.FIREWORK_ROCKET)) {
            helperCd = (int) m.num("Cooldown (tick)");
        } else {
            p.sendMessage(Text.literal("Fisek bulunamadi (hotbar veya sol el)."), true);
        }
    }

    // ---- ElytraBooster: hiz esigin altina dusunce kendisi fisek atar.
    private static void elytraBooster(MinecraftClient mc, ClientPlayerEntity p) {
        Module m = Modules.ELYBOOSTER;
        if (boosterCd > 0) { boosterCd--; return; }
        if (!p.isGliding()) return;
        if (p.getVelocity().length() * 20.0 >= m.num("Min speed (b/s)")) return;

        if (useFromHotbar(mc, p, Items.FIREWORK_ROCKET)) {
            boosterCd = (int) m.num("Cooldown (tick)");
        } else {
            boosterCd = 40; // fisek yok: surekli deneme
        }
    }

    // ---- AirStuck
    //      Freeze: acildigi konumda dondurur. Hover: dusmeyi durdurur, yatay hareket serbest.
    private static void airStuck(ClientPlayerEntity p) {
        Module m = Modules.AIRSTUCK;
        boolean onlyAir = m.bool("Only in air");

        if (m.index("Mode") == 1) {                       // Hover
            frozen = null;
            if (onlyAir && p.isOnGround()) return;
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.0, v.z);
            return;
        }

        if (frozen == null) {                             // Freeze
            if (onlyAir && p.isOnGround()) return;
            frozen = new Vec3d(p.getX(), p.getY(), p.getZ());
        }
        p.setPosition(frozen.x, frozen.y, frozen.z);
        p.setVelocity(Vec3d.ZERO);
    }

    // ---- GuiMove: envanter/sandik acikken W A S D + Space ile yurumeye devam et.
    //      (Varsayilan tuslar okunur; yazi kutulu ekranlarda calismaz.)
    private static void guiMove(MinecraftClient mc) {
        Screen s = mc.currentScreen;
        if (s == null) return;
        if (!(s instanceof HandledScreen) || s instanceof AnvilScreen || s instanceof CreativeInventoryScreen) return;

        long w = mc.getWindow().getHandle();
        mc.options.forwardKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS);
        mc.options.backKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS);
        mc.options.leftKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS);
        mc.options.rightKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);
        mc.options.jumpKey.setPressed(GLFW.glfwGetKey(w, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
    }

    // ---- FullBright: istemci tarafinda gece gorusu efekti.
    private static void fullbright(ClientPlayerEntity p) {
        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || cur.getDuration() < 400) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        }
        nightVisionApplied = true;
    }

    // ==================================================================
    //  PLAYER
    // ==================================================================

    // ---- AutoTool: blok kirarken o blok icin en hizli aleti secer, birakinca eski slota doner.
    private static void autoTool(MinecraftClient mc, ClientPlayerEntity p) {
        PlayerInventory inv = p.getInventory();

        if (mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof BlockHitResult b
                && b.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.world.getBlockState(b.getBlockPos());
            int cur = inv.getSelectedSlot();
            int best = cur;
            float bestSpeed = inv.getStack(cur).getMiningSpeedMultiplier(st);
            for (int i = 0; i < 9; i++) {
                float sp = inv.getStack(i).getMiningSpeedMultiplier(st);
                if (sp > bestSpeed + 0.01f) {
                    bestSpeed = sp;
                    best = i;
                }
            }
            if (best != cur) {
                if (toolPrev < 0) toolPrev = cur;
                inv.setSelectedSlot(best);
            }
        } else if (toolPrev >= 0) {
            inv.setSelectedSlot(toolPrev);
            toolPrev = -1;
        }
    }

    // ---- AutoArmor: envanterdeki daha iyi zirhi giyer (tier'e gore: netherite > elmas > demir ...).
    private static final String[] ARMOR_SUFFIX = {"_helmet", "_chestplate", "_leggings", "_boots"};
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static int armorRank(ItemStack s, int part) {
        if (s.isEmpty()) return 0;
        String path = Registries.ITEM.getId(s.getItem()).getPath();
        if (!path.endsWith(ARMOR_SUFFIX[part])) return -1;
        if (path.startsWith("netherite_")) return 6;
        if (path.startsWith("diamond_")) return 5;
        if (path.startsWith("iron_")) return 4;
        if (path.startsWith("chainmail_")) return 3;
        if (path.startsWith("golden_") || path.startsWith("turtle_")) return 2;
        if (path.startsWith("leather_")) return 1;
        return 1;
    }

    private static void autoArmor(MinecraftClient mc, ClientPlayerEntity p) {
        if (armorCd > 0) { armorCd--; return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        PlayerInventory inv = p.getInventory();
        for (int part = 0; part < 4; part++) {
            ItemStack worn = p.getEquippedStack(ARMOR_SLOTS[part]);
            int curRank = armorRank(worn, part);
            if (curRank < 0) continue; // zirh slotunda baska bir sey var (elytra, balkabagi): dokunma

            int bestInv = -1, bestRank = curRank;
            for (int i = 0; i < 36; i++) {
                int r = armorRank(inv.getStack(i), part);
                if (r > bestRank) {
                    bestRank = r;
                    bestInv = i;
                }
            }
            if (bestInv < 0) continue;

            int sync = p.playerScreenHandler.syncId;
            int slot = bestInv < 9 ? bestInv + 36 : bestInv;
            int armorSlot = 5 + part; // 5 basliik, 6 govde, 7 pantolon, 8 bot
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);      // yeni zirhi al
            mc.interactionManager.clickSlot(sync, armorSlot, 0, SlotActionType.PICKUP, p); // giy (eskisi imlece gelir)
            mc.interactionManager.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);      // eskisini yerine koy
            armorCd = (int) Modules.AUTOARMOR.num("Delay (tick)");
            return;
        }
    }

    // ---- ChestStealer: sandik acilinca icindekileri tek tek alir.
    private static void chestStealer(MinecraftClient mc, ClientPlayerEntity p) {
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            stealStarted = false;
            return;
        }
        if (stealCd > 0) { stealCd--; return; }

        Module m = Modules.CHESTSTEALER;
        GenericContainerScreenHandler h = gcs.getScreenHandler();
        int n = h.getRows() * 9;
        for (int i = 0; i < n; i++) {
            if (!h.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.QUICK_MOVE, p);
                stealStarted = true;
                stealCd = (int) m.num("Delay (tick)");
                return;
            }
        }
        // sandik bosaldi
        if (stealStarted && m.bool("Auto close")) {
            p.closeHandledScreen();
            stealStarted = false;
        }
    }

    // ==================================================================
    //  MISC
    // ==================================================================

    // ---- Scaffold: ayagin altindaki bos yere hotbar'daki bir blok koyar.
    private static void scaffold(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isGliding() || p.hasVehicle() || p.getAbilities().flying) return;

        BlockPos target = BlockPos.ofFloored(p.getX(), p.getY() - 1.0, p.getZ());
        if (!mc.world.getBlockState(target).isAir()) return;

        // Yerlestirmek icin yaninda/altinda dolu bir komsu blok bul
        Direction[] dirs = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        BlockPos support = null;
        Direction face = null;
        for (Direction d : dirs) {
            BlockPos n = target.offset(d);
            BlockState st = mc.world.getBlockState(n);
            if (!st.isAir() && st.getFluidState().isEmpty()) {
                support = n;
                face = d.getOpposite();
                break;
            }
        }
        if (support == null) return;

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() instanceof BlockItem bi
                    && bi.getBlock().getDefaultState().isSolidBlock(mc.world, target)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;

        int prev = inv.getSelectedSlot();
        inv.setSelectedSlot(slot);
        Vec3d hitPos = Vec3d.ofCenter(support).add(
                face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, support, false));
        p.swingHand(Hand.MAIN_HAND);
        if (Modules.SCAFFOLD.bool("Switch back")) inv.setSelectedSlot(prev);
    }
}
