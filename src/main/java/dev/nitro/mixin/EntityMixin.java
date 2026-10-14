package dev.nitro.mixin;

import dev.nitro.module.ModuleManager;
import dev.nitro.module.impl.FreeCam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
	private void nitro$look(double dx, double dy, CallbackInfo ci) {
		if ((Object) this != MinecraftClient.getInstance().player) return;
		FreeCam fc = ModuleManager.get(FreeCam.class);
		if (fc != null && fc.active()) {
			fc.look(dx, dy);
			ci.cancel();
		}
	}
}
