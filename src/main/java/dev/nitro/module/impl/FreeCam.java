package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.setting.NumSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FreeCam extends Module {
	private final NumSetting speed = add(new NumSetting("Speed", 6, 1, 20, true));

	public Vec3d pos, prev;
	public float yaw, pitch;

	public FreeCam() {
		super("Free cam", "Detach the camera from your player.", Category.RENDER);
	}

	public boolean active() {
		return enabled && pos != null;
	}

	public Vec3d cameraPos(float tickDelta) {
		return prev.lerp(pos, tickDelta);
	}

	public void look(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90.0f, 90.0f);
	}

	@Override
	public void onEnable() {
		if (mc.player == null) { enabled = false; return; }
		pos = prev = mc.gameRenderer.getCamera().getPos();
		yaw = mc.player.getYaw();
		pitch = mc.player.getPitch();
	}

	@Override
	public void onDisable() {
		pos = prev = null;
	}

	@Override
	public void onTick() {
		if (mc.player == null || pos == null) return;
		prev = pos;
		if (mc.currentScreen != null) return;

		double rad = Math.toRadians(yaw);
		Vec3d fwd = new Vec3d(-Math.sin(rad), 0, Math.cos(rad));
		Vec3d right = new Vec3d(-Math.cos(rad), 0, -Math.sin(rad));

		Vec3d move = Vec3d.ZERO;
		if (mc.options.forwardKey.isPressed()) move = move.add(fwd);
		if (mc.options.backKey.isPressed()) move = move.subtract(fwd);
		if (mc.options.rightKey.isPressed()) move = move.add(right);
		if (mc.options.leftKey.isPressed()) move = move.subtract(right);
		if (mc.options.jumpKey.isPressed()) move = move.add(0, 1, 0);
		if (mc.options.sneakKey.isPressed()) move = move.subtract(0, 1, 0);

		if (move.lengthSquared() > 0) pos = pos.add(move.normalize().multiply(speed.value * 0.1));
	}
}
