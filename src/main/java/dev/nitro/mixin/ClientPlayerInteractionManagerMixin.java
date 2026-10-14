package dev.nitro.mixin;

import dev.nitro.module.ModuleManager;
import dev.nitro.module.impl.CrystalOptimizer;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
	@Inject(method = "attackEntity", at = @At("TAIL"))
	private void nitro$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
		CrystalOptimizer co = ModuleManager.get(CrystalOptimizer.class);
		if (co != null && co.enabled) co.onAttack(target);
	}
}
