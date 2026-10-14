package dev.nitro.mixin;

import dev.nitro.module.ModuleManager;
import dev.nitro.module.impl.FreeCam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void nitro$freeze(CallbackInfo ci) {
		FreeCam fc = ModuleManager.get(FreeCam.class);
		if (fc != null && fc.active()) {
			this.playerInput = PlayerInput.DEFAULT;
			this.movementForward = 0.0f;
			this.movementSideways = 0.0f;
		}
	}
}
