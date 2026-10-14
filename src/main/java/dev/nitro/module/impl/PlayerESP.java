package dev.nitro.module.impl;

import dev.nitro.Theme;
import dev.nitro.module.Module;
import dev.nitro.module.ModuleManager;
import dev.nitro.render.Render3D;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class PlayerESP extends Module {
	private final NumSetting range = add(new NumSetting("Range", 128, 16, 256, true));
	private final BoolSetting tracers = add(new BoolSetting("Tracers", false));

	public PlayerESP() {
		super("Player ESP", "Boxes players through walls.", Category.RENDER);
	}

	@Override
	public void onRender(WorldRenderContext ctx) {
		if (mc.world == null || mc.player == null) return;
		Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
		Vec3d cam = ctx.camera().getPos();
		float td = ctx.tickCounter().getTickDelta(false);
		double r2 = range.value * range.value;
		FreeCam fc = ModuleManager.get(FreeCam.class);
		boolean freecam = fc != null && fc.active();

		Vec3d look = Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.4);

		for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
			boolean self = p == mc.player;
			if (self && !freecam) continue;
			Vec3d pos = p.getLerpedPos(td);
			if (pos.squaredDistanceTo(cam) > r2) continue;
			Box box = p.getDimensions(p.getPose()).getBoxAt(pos);
			int color = self ? Theme.accent : 0xFFFF4D4D;
			Render3D.box(m, cam, box, color);
			if (tracers.value && !self) {
				Vec3d mid = box.getCenter();
				Render3D.line(m, (float) look.x, (float) look.y, (float) look.z,
					(float) (mid.x - cam.x), (float) (mid.y - cam.y), (float) (mid.z - cam.z), color);
			}
		}
		Render3D.end();
	}
}
