package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.render.Render3D;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans loaded chunks for blocks that do not appear naturally (or only appear after a chunk has been
 * loaded for a long time) and flags chunks with enough of them as a possible base:
 * - deepslate placed sideways (natural deepslate always faces up/down),
 * - amethyst clusters that are not attached to geode blocks,
 * - long vine columns that are not hanging from leaves or logs,
 * - fully grown kelp (age 24+, natural kelp stops at 23).
 */
public class ChunkFinder extends Module {
	private static final int C_DEEP = 0xFFB04DFF, C_CLUSTER = 0xFFFF77CC, C_VINE = 0xFF55FF55, C_KELP = 0xFFFFFF55;
	private static final int C_CHUNK = 0xFFFFAA00;
	private static final int RESCAN_TICKS = 1200;

	private final NumSetting range = add(new NumSetting("Range (chunks)", 8, 1, 16, true));
	private final BoolSetting deepslate = add(new BoolSetting("Rotated deepslate", true));
	private final BoolSetting clusters = add(new BoolSetting("Amethyst clusters", true));
	private final BoolSetting vines = add(new BoolSetting("Grown vines", true));
	private final BoolSetting kelp = add(new BoolSetting("Grown kelp", true));
	private final NumSetting minVine = add(new NumSetting("Min vine length", 10, 3, 30, true));
	private final NumSetting minScore = add(new NumSetting("Min score", 4, 1, 30, true));
	private final BoolSetting marks = add(new BoolSetting("Mark blocks", true));
	private final BoolSetting tracers = add(new BoolSetting("Tracers", false));
	private final BoolSetting alerts = add(new BoolSetting("Chat alerts", true));

	private record Mark(long pos, int color) {}

	private static final class Result {
		int deep, clusters, vines, kelp, tick;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		boolean flagged;
		final List<Mark> marks = new ArrayList<>();
	}

	private final Map<Long, Result> results = new HashMap<>();
	private final Set<Long> alerted = new HashSet<>();
	private int tick;

	public ChunkFinder() {
		super("Chunk finder", "Flags chunks with signs of a player base.", Category.RENDER);
	}

	@Override
	public void onEnable() {
		results.clear();
		alerted.clear();
	}

	@Override
	public void onDisable() {
		results.clear();
		alerted.clear();
	}

	// ------------------------------------------------------------ scanning

	@Override
	public void onTick() {
		if (mc.world == null || mc.player == null) return;
		tick++;
		ChunkPos cp = mc.player.getChunkPos();
		int r = range.i();
		long start = System.nanoTime();

		for (int d = 0; d <= r; d++) {
			for (int dx = -d; dx <= d; dx++) {
				for (int dz = -d; dz <= d; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
					int x = cp.x + dx, z = cp.z + dz;
					long key = ChunkPos.toLong(x, z);
					Result old = results.get(key);
					if (old != null && tick - old.tick < RESCAN_TICKS) continue;
					WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(x, z);
					if (chunk == null) continue;

					Result res = scan(chunk);
					results.put(key, res);
					if (res.flagged && alerted.add(key) && alerts.value) {
						msg("Possible base at chunk " + x + ", " + z + " (blocks ~" + (x * 16 + 8) + ", " + (z * 16 + 8) + "): "
							+ res.deep + " rotated deepslate, " + res.clusters + " clusters, "
							+ res.vines + " vines, " + res.kelp + " kelp");
					}
					if (System.nanoTime() - start > 3_000_000L) return;
				}
			}
		}

		if (tick % 200 == 0) {
			Iterator<Map.Entry<Long, Result>> it = results.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Long, Result> e = it.next();
				ChunkPos c = new ChunkPos(e.getKey());
				if (Math.abs(c.x - cp.x) > r + 3 || Math.abs(c.z - cp.z) > r + 3) {
					alerted.remove(e.getKey());
					it.remove();
				}
			}
		}
	}

	private static boolean interesting(BlockState s, boolean d, boolean c, boolean v, boolean k) {
		return (d && s.isOf(Blocks.DEEPSLATE) && s.get(PillarBlock.AXIS) != Direction.Axis.Y)
			|| (c && s.isOf(Blocks.AMETHYST_CLUSTER))
			|| (v && s.isOf(Blocks.VINE))
			|| (k && s.isOf(Blocks.KELP) && s.get(Properties.AGE_25) >= 24);
	}

	private Result scan(WorldChunk chunk) {
		Result r = new Result();
		r.tick = tick;
		boolean wd = deepslate.value, wc = clusters.value, wv = vines.value, wk = kelp.value;

		ChunkSection[] sections = chunk.getSectionArray();
		int bottom = chunk.getBottomSectionCoord();
		int baseX = chunk.getPos().getStartX();
		int baseZ = chunk.getPos().getStartZ();
		BlockPos.Mutable p = new BlockPos.Mutable();

		for (int i = 0; i < sections.length; i++) {
			ChunkSection sec = sections[i];
			if (sec == null || sec.isEmpty()) continue;
			if (!sec.hasAny(s -> interesting(s, wd, wc, wv, wk))) continue; // cheap palette check

			int baseY = (bottom + i) * 16;
			for (int ly = 0; ly < 16; ly++) {
				for (int lz = 0; lz < 16; lz++) {
					for (int lx = 0; lx < 16; lx++) {
						BlockState s = sec.getBlockState(lx, ly, lz);
						if (!interesting(s, wd, wc, wv, wk)) continue;
						p.set(baseX + lx, baseY + ly, baseZ + lz);

						if (s.isOf(Blocks.DEEPSLATE)) {
							r.deep++;
							mark(r, p, C_DEEP);
						} else if (s.isOf(Blocks.AMETHYST_CLUSTER)) {
							Direction face = s.get(Properties.FACING);
							BlockState support = mc.world.getBlockState(p.offset(face.getOpposite()));
							if (!support.isOf(Blocks.AMETHYST_BLOCK) && !support.isOf(Blocks.BUDDING_AMETHYST)) {
								r.clusters++;
								mark(r, p, C_CLUSTER);
							}
						} else if (s.isOf(Blocks.VINE)) {
							if (grownVine(p, s)) {
								r.vines++;
								mark(r, p, C_VINE);
							}
						} else if (s.isOf(Blocks.KELP)) {
							r.kelp++;
							mark(r, p, C_KELP);
						}
					}
				}
			}
		}

		int score = Math.min(r.deep, 10) + Math.min(r.clusters, 10) + Math.min(r.vines, 10) + Math.min(r.kelp, 10);
		r.flagged = score >= minScore.i();
		return r;
	}

	private void mark(Result r, BlockPos p, int color) {
		r.minY = Math.min(r.minY, p.getY());
		r.maxY = Math.max(r.maxY, p.getY());
		if (r.marks.size() < 150) r.marks.add(new Mark(p.asLong(), color));
	}

	/** Top of a long vine column that is not attached to leaves or logs. */
	private boolean grownVine(BlockPos.Mutable p, BlockState s) {
		if (mc.world.getBlockState(p.up()).isOf(Blocks.VINE)) return false; // not the top of the column

		for (Direction d : Direction.Type.HORIZONTAL) {
			if (s.get(VineBlock.getFacingProperty(d))) {
				BlockState n = mc.world.getBlockState(p.offset(d));
				if (n.isIn(BlockTags.LEAVES) || n.isIn(BlockTags.LOGS)) return false;
			}
		}
		if (s.get(VineBlock.UP)) {
			BlockState n = mc.world.getBlockState(p.up());
			if (n.isIn(BlockTags.LEAVES) || n.isIn(BlockTags.LOGS)) return false;
		}

		int len = 1;
		BlockPos.Mutable q = p.mutableCopy();
		while (len < 64) {
			q.move(Direction.DOWN);
			if (!mc.world.getBlockState(q).isOf(Blocks.VINE)) break;
			len++;
		}
		return len >= minVine.i();
	}

	// ------------------------------------------------------------ render

	@Override
	public void onRender(WorldRenderContext ctx) {
		if (mc.world == null || mc.player == null || results.isEmpty()) return;
		Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
		Vec3d cam = ctx.camera().getPos();
		ChunkPos cp = mc.player.getChunkPos();
		int r = range.i() + 1;
		int budget = 800;
		Vec3d look = Vec3d.fromPolar(ctx.camera().getPitch(), ctx.camera().getYaw()).multiply(0.4);

		for (Map.Entry<Long, Result> e : results.entrySet()) {
			Result res = e.getValue();
			if (!res.flagged) continue;
			ChunkPos c = new ChunkPos(e.getKey());
			if (Math.abs(c.x - cp.x) > r || Math.abs(c.z - cp.z) > r) continue;

			double sx = c.getStartX(), sz = c.getStartZ();
			double y1 = res.minY - 1, y2 = res.maxY + 2;
			Render3D.box(m, cam, new Box(sx, y1, sz, sx + 16, y2, sz + 16), C_CHUNK);

			if (tracers.value) {
				Vec3d mid = new Vec3d(sx + 8, (y1 + y2) / 2, sz + 8);
				Render3D.line(m, (float) look.x, (float) look.y, (float) look.z,
					(float) (mid.x - cam.x), (float) (mid.y - cam.y), (float) (mid.z - cam.z), C_CHUNK);
			}
			if (marks.value) {
				for (Mark mk : res.marks) {
					if (budget-- <= 0) break;
					Render3D.box(m, cam, new Box(BlockPos.fromLong(mk.pos)), mk.color);
				}
			}
		}
		Render3D.end();
	}
}
