package com.example.spcombat.mixin;

import com.example.spcombat.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class WingsMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void spcombat$wings(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!Modules.WINGS.enabled) return;
        if (entity != MinecraftClient.getInstance().player) return;
        if (state instanceof BipedEntityRenderState biped && biped.equippedChestStack.isEmpty()) {
            biped.equippedChestStack = new ItemStack(Items.ELYTRA);
        }
    }
}
