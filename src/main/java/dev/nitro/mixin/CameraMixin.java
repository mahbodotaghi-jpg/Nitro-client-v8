package dev.nitro.mixin;

import dev.nitro.module.ModuleManager;
import dev.nitro.module.impl.FreeCam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow protected abstract void setPos(Vec3d pos);
	@Shadow protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "update", at = @At("TAIL"))
	private void nitro$update(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		FreeCam fc = ModuleManager.get(FreeCam.class);
		if (fc != null && fc.active()) {
			setRotation(fc.yaw, fc.pitch);
			setPos(fc.cameraPos(tickDelta));
		}
	}

	@Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
	private void nitro$third(CallbackInfoReturnable<Boolean> cir) {
		FreeCam fc = ModuleManager.get(FreeCam.class);
		if (fc != null && fc.active()) cir.setReturnValue(true);
	}
}
