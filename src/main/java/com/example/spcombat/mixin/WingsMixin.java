package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wings: sadece bizim oyuncumuzun cizimine elytra kanatlari ekler (gorsel). Envanter ve zirh
 * degismez: cizim aninda gogus zirhi bossa oyun elytra varmis gibi cizer.
 * Bu mixin ayri bir ayar dosyasinda ve zorunlu degil: uyusmazlik olursa sadece Wings calismaz.
 */
@Mixin(LivingEntityRenderer.class)
public class WingsMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void spcombat$wings(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!Modules.WINGS.enabled) return;
        if (entity != MinecraftClient.getInstance().player) return;
        if (state.equippedChestStack.isEmpty()) {
            state.equippedChestStack = new ItemStack(Items.ELYTRA);
        }
    }
}
