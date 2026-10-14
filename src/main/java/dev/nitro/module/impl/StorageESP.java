package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.render.Render3D;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public class StorageESP extends Module {
	private final NumSetting range = add(new NumSetting("Range", 128, 16, 256, true));
	private final BoolSetting chests = add(new BoolSetting("Chests", true));
	private final BoolSetting trapped = add(new BoolSetting("Trapped chests", true));
	private final BoolSetting ender = add(new BoolSetting("Ender chests", true));
	private final BoolSetting barrels = add(new BoolSetting("Barrels", true));
	private final BoolSetting shulkers = add(new BoolSetting("Shulkers", true));

	public StorageESP() {
		super("Storage ESP", "Boxes storage blocks through walls.", Category.RENDER);
	}

	@Override
	public void onRender(WorldRenderContext ctx) {
		if (mc.world == null || mc.player == null) return;
		Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
		Vec3d cam = ctx.camera().getPos();
		double r2 = range.value * range.value;
		int cr = (int) (range.value / 16) + 1;
		ChunkPos cp = mc.player.getChunkPos();

		for (int dx = -cr; dx <= cr; dx++) {
			for (int dz = -cr; dz <= cr; dz++) {
				WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cp.x + dx, cp.z + dz);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					int color;
					if (be instanceof TrappedChestBlockEntity) {
						if (!trapped.value) continue;
						color = 0xFFFF4040;
					} else if (be instanceof ChestBlockEntity) {
						if (!chests.value) continue;
						color = 0xFFFFAA00;
					} else if (be instanceof EnderChestBlockEntity) {
						if (!ender.value) continue;
						color = 0xFFB04DFF;
					} else if (be instanceof BarrelBlockEntity) {
						if (!barrels.value) continue;
						color = 0xFFC08040;
					} else if (be instanceof ShulkerBoxBlockEntity) {
						if (!shulkers.value) continue;
						color = 0xFFFF77CC;
					} else {
						continue;
					}
					if (be.getPos().toCenterPos().squaredDistanceTo(cam) > r2) continue;
					Render3D.box(m, cam, new Box(be.getPos()).expand(-0.06), color);
				}
			}
		}
		Render3D.end();
	}
}
