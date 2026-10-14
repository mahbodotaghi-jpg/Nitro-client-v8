package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.render.Render3D;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import dev.nitro.setting.TextSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Re-runs vanilla overworld ore generation (1.18+ style) from a world seed and boxes the predicted
 * veins. Per chunk: population seed -> per-feature decorator seed -> count / square / height ->
 * OreFeature vein shape, exactly as the game does it.
 */
public class OreSim extends Module {
	public enum Ore {
		COAL(0xFF6B6B6B, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE),
		IRON(0xFFD8AF93, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE),
		GOLD(0xFFFFD700, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE),
		REDSTONE(0xFFFF2222, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE),
		DIAMOND(0xFF4DEBFF, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE),
		LAPIS(0xFF2F5BFF, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE),
		COPPER(0xFFE07B39, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE),
		EMERALD(0xFF17DD62, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);

		final int color;
		final Block[] blocks;

		Ore(int color, Block... blocks) {
			this.color = color;
			this.blocks = blocks;
		}

		String label() {
			String n = name();
			return n.charAt(0) + n.substring(1).toLowerCase();
		}
	}

	/** One placed-feature: count/rarity, square spread, height range, vein size. */
	private record Feat(Ore ore, int index, int size, float discard, int cMin, int cMax, int rarity,
						int hMin, int hMax, boolean trapezoid, Predicate<RegistryEntry<Biome>> biome) {}

	private record Hit(long pos, Ore ore) {}

	private static final int STEP = 6; // GenerationStep.Feature.UNDERGROUND_ORES

	private static final Predicate<RegistryEntry<Biome>> ANY = b -> true;
	private static final Predicate<RegistryEntry<Biome>> BADLANDS = b ->
		b.matchesKey(BiomeKeys.BADLANDS) || b.matchesKey(BiomeKeys.ERODED_BADLANDS) || b.matchesKey(BiomeKeys.WOODED_BADLANDS);
	private static final Predicate<RegistryEntry<Biome>> MOUNTAINS = b ->
		b.matchesKey(BiomeKeys.WINDSWEPT_HILLS) || b.matchesKey(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS)
			|| b.matchesKey(BiomeKeys.WINDSWEPT_FOREST) || b.matchesKey(BiomeKeys.MEADOW)
			|| b.matchesKey(BiomeKeys.CHERRY_GROVE) || b.matchesKey(BiomeKeys.GROVE)
			|| b.matchesKey(BiomeKeys.SNOWY_SLOPES) || b.matchesKey(BiomeKeys.FROZEN_PEAKS)
			|| b.matchesKey(BiomeKeys.JAGGED_PEAKS) || b.matchesKey(BiomeKeys.STONY_PEAKS);

	// Indices are the feature's position in the merged overworld UNDERGROUND_ORES list (kept in
	// ascending order). Use the "Index offset" setting / "Calibrate" if they are off in your version.
	private static final List<Feat> FEATS = List.of(
		new Feat(Ore.COAL, 9, 17, 0.0f, 30, 30, 0, 136, 320, false, ANY),
		new Feat(Ore.COAL, 10, 17, 0.5f, 20, 20, 0, 0, 192, true, ANY),
		new Feat(Ore.IRON, 11, 9, 0.0f, 90, 90, 0, 80, 384, true, ANY),
		new Feat(Ore.IRON, 12, 9, 0.0f, 10, 10, 0, -24, 56, true, ANY),
		new Feat(Ore.IRON, 13, 4, 0.0f, 10, 10, 0, -64, 72, false, ANY),
		new Feat(Ore.GOLD, 14, 9, 0.5f, 4, 4, 0, -64, 32, true, ANY),
		new Feat(Ore.GOLD, 15, 9, 0.5f, 0, 1, 0, -64, -48, false, ANY),
		new Feat(Ore.REDSTONE, 16, 8, 0.0f, 4, 4, 0, -64, 15, false, ANY),
		new Feat(Ore.REDSTONE, 17, 8, 0.0f, 8, 8, 0, -96, -32, true, ANY),
		new Feat(Ore.DIAMOND, 18, 4, 0.5f, 7, 7, 0, -144, 16, true, ANY),
		new Feat(Ore.DIAMOND, 19, 12, 0.7f, 1, 1, 9, -144, 16, true, ANY),
		new Feat(Ore.DIAMOND, 20, 8, 1.0f, 4, 4, 0, -144, 16, true, ANY),
		new Feat(Ore.LAPIS, 21, 7, 0.0f, 2, 2, 0, -32, 32, true, ANY),
		new Feat(Ore.LAPIS, 22, 7, 1.0f, 4, 4, 0, -64, 64, false, ANY),
		new Feat(Ore.COPPER, 23, 10, 0.0f, 16, 16, 0, -16, 112, true, ANY),
		new Feat(Ore.GOLD, 24, 9, 0.5f, 50, 50, 0, 32, 256, false, BADLANDS),
		new Feat(Ore.EMERALD, 25, 3, 0.0f, 100, 100, 0, -80, 480, true, MOUNTAINS)
	);

	private static final Set<Block> ALL_ORE_BLOCKS = new HashSet<>();
	static {
		for (Ore o : Ore.values()) for (Block b : o.blocks) ALL_ORE_BLOCKS.add(b);
	}

	private final TextSetting seed = add(new TextSetting("Seed", ""));
	private final NumSetting range = add(new NumSetting("Range (chunks)", 4, 1, 12, true));
	private final NumSetting offset = add(new NumSetting("Index offset", 0, -8, 8, true));
	private final NumSetting maxBoxes = add(new NumSetting("Max boxes", 2000, 100, 6000, true));
	private final BoolSetting confirmed = add(new BoolSetting("Confirmed only", false));
	private final BoolSetting calibrate = add(new BoolSetting("Calibrate now", false));
	private final EnumMap<Ore, BoolSetting> oreToggles = new EnumMap<>(Ore.class);

	private final Map<Long, List<Hit>> cache = new HashMap<>();
	private String signature = "";

	public OreSim() {
		super("Oresim", "Predicts ore positions from the world seed.", Category.RENDER);
		for (Ore o : Ore.values()) oreToggles.put(o, add(new BoolSetting(o.label(), o == Ore.DIAMOND)));
	}

	@Override
	public void onEnable() {
		cache.clear();
		if (parseSeed() == null) msg("Oresim: enter the world seed in the module settings.");
	}

	@Override
	public void onDisable() {
		cache.clear();
	}

	// ---------------------------------------------------------------- seed

	private Long parseSeed() {
		String s = seed.value == null ? "" : seed.value.trim();
		if (s.isEmpty()) return null;
		if (s.matches("-?\\d{1,19}")) {
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException ignored) {
				// falls through to hashing, same as the game does for non-numeric seeds
			}
		}
		return (long) s.hashCode();
	}

	// ---------------------------------------------------------------- tick

	@Override
	public void onTick() {
		if (mc.world == null || mc.player == null) return;
		if (mc.world.getRegistryKey() != World.OVERWORLD) return;
		Long sd = parseSeed();
		if (sd == null) return;

		String sig = sd + ":" + offset.i();
		if (!sig.equals(signature)) {
			cache.clear();
			signature = sig;
		}

		if (calibrate.value) {
			calibrate.value = false;
			runCalibration(sd);
			return;
		}

		ChunkPos cp = mc.player.getChunkPos();
		int r = range.i();
		long start = System.nanoTime();
		for (int d = 0; d <= r; d++) {
			for (int dx = -d; dx <= d; dx++) {
				for (int dz = -d; dz <= d; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != d) continue;
					int x = cp.x + dx, z = cp.z + dz;
					long key = ChunkPos.toLong(x, z);
					if (cache.containsKey(key) || !ready(x, z)) continue;
					cache.put(key, simulate(mc.world, x, z, sd, offset.i(), null));
					if (System.nanoTime() - start > 4_000_000L) return;
				}
			}
		}
	}

	/** A chunk can be simulated once it and its 8 neighbours are loaded (veins cross chunk borders). */
	private boolean ready(int cx, int cz) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				if (mc.world.getChunkManager().getWorldChunk(cx + dx, cz + dz) == null) return false;
		return true;
	}

	// ---------------------------------------------------------------- render

	@Override
	public void onRender(WorldRenderContext ctx) {
		if (mc.world == null || mc.player == null || cache.isEmpty()) return;
		if (mc.world.getRegistryKey() != World.OVERWORLD) return;

		Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
		Vec3d cam = ctx.camera().getPos();
		ChunkPos cp = mc.player.getChunkPos();
		int r = range.i() + 1;
		int budget = maxBoxes.i();
		BlockPos.Mutable mut = new BlockPos.Mutable();

		for (Map.Entry<Long, List<Hit>> e : cache.entrySet()) {
			ChunkPos c = new ChunkPos(e.getKey());
			if (Math.abs(c.x - cp.x) > r || Math.abs(c.z - cp.z) > r) continue;
			for (Hit h : e.getValue()) {
				if (!oreToggles.get(h.ore).value) continue;
				mut.set(h.pos);
				BlockState st = mc.world.getBlockState(mut);
				if (confirmed.value ? !isOreBlock(st) : st.isAir()) continue;
				Render3D.box(m, cam, new Box(mut), h.ore.color);
				if (--budget <= 0) {
					Render3D.end();
					return;
				}
			}
		}
		Render3D.end();
	}

	// ---------------------------------------------------------------- calibration

	/**
	 * Tries index offsets -4..4 on coal/iron/copper (common ores) in the loaded chunks around you and
	 * keeps the offset whose predicted veins line up with real ore blocks the best.
	 */
	private void runCalibration(long sd) {
		EnumSet<Ore> only = EnumSet.of(Ore.COAL, Ore.IRON, Ore.COPPER);
		ChunkPos cp = mc.player.getChunkPos();
		int bestOff = 0, best = -1, second = -1, tested = 0;

		for (int off = -4; off <= 4; off++) {
			int score = 0;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					int x = cp.x + dx, z = cp.z + dz;
					if (!ready(x, z)) continue;
					if (off == -4) tested++;
					for (Hit h : simulate(mc.world, x, z, sd, off, only)) {
						if (isOreBlock(mc.world.getBlockState(BlockPos.fromLong(h.pos)))) score++;
					}
				}
			}
			if (score > best) {
				second = best;
				best = score;
				bestOff = off;
			} else if (score > second) {
				second = score;
			}
		}

		if (tested == 0) {
			msg("Calibration needs loaded chunks around you. Stand still for a moment and try again.");
		} else if (best < 8 || best < second * 1.5) {
			msg("Calibration inconclusive (best score " + best + "). Check the seed and Minecraft version, and try again underground.");
		} else {
			offset.set(bestOff);
			cache.clear();
			msg("Calibrated: index offset " + bestOff + " (score " + best + ").");
		}
	}

	// ---------------------------------------------------------------- simulation

	private static boolean isOreBlock(BlockState s) {
		return ALL_ORE_BLOCKS.contains(s.getBlock());
	}

	private static boolean isTarget(BlockState s) {
		return s.isIn(BlockTags.STONE_ORE_REPLACEABLES) || s.isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || isOreBlock(s);
	}

	private static boolean exposed(ClientWorld w, BlockPos pos, BlockPos.Mutable tmp) {
		for (Direction d : Direction.values()) {
			if (w.getBlockState(tmp.set(pos, d)).isAir()) return true;
		}
		return false;
	}

	private static boolean keep(Random r, float discard) {
		if (discard <= 0.0f) return true;
		if (discard >= 1.0f) return false;
		return r.nextFloat() >= discard;
	}

	private static int height(Random r, Feat f) {
		if (!f.trapezoid) return f.hMin + r.nextInt(f.hMax - f.hMin + 1);
		int k = f.hMax - f.hMin;
		int l = k / 2;
		int m = k - l;
		return f.hMin + r.nextInt(m + 1) + r.nextInt(l + 1);
	}

	private List<Hit> simulate(ClientWorld w, int cx, int cz, long worldSeed, int off, Set<Ore> only) {
		List<Hit> out = new ArrayList<>();
		Set<Long> placed = new HashSet<>();
		Random r = new Random();

		// ChunkRandom#setPopulationSeed
		r.setSeed(worldSeed);
		long a = r.nextLong() | 1L;
		long b = r.nextLong() | 1L;
		long population = ((long) (cx * 16)) * a + ((long) (cz * 16)) * b ^ worldSeed;

		for (Feat f : FEATS) {
			if (only != null && !only.contains(f.ore)) continue;

			// ChunkRandom#setDecoratorSeed
			r.setSeed(population + (long) (f.index + off) + 10000L * STEP);

			if (f.rarity > 0 && !(r.nextFloat() < 1.0f / f.rarity)) continue;
			int count = f.cMin == f.cMax ? f.cMin : f.cMin + r.nextInt(f.cMax - f.cMin + 1);

			for (int i = 0; i < count; i++) {
				int x = r.nextInt(16) + cx * 16;
				int z = r.nextInt(16) + cz * 16;
				int y = height(r, f);
				if (f.biome != ANY && !f.biome.test(w.getBiome(new BlockPos(x, y, z)))) continue;
				vein(w, r, f, x, y, z, placed, out);
			}
		}
		return out;
	}

	/** Port of OreFeature#generate + #generateVeinPart, recording positions instead of placing blocks. */
	private void vein(ClientWorld w, Random r, Feat f, int ox, int oy, int oz, Set<Long> placed, List<Hit> out) {
		float angle = r.nextFloat() * (float) Math.PI;
		float g = (float) f.size / 8.0F;
		int i = MathHelper.ceil(((float) f.size / 16.0F * 2.0F + 1.0F) / 2.0F);
		double startX = (double) ox + Math.sin((double) angle) * (double) g;
		double endX = (double) ox - Math.sin((double) angle) * (double) g;
		double startZ = (double) oz + Math.cos((double) angle) * (double) g;
		double endZ = (double) oz - Math.cos((double) angle) * (double) g;
		double startY = (double) (oy + r.nextInt(3) - 2);
		double endY = (double) (oy + r.nextInt(3) - 2);
		int x0 = ox - MathHelper.ceil(g) - i;
		int y0 = oy - 2 - i;
		int z0 = oz - MathHelper.ceil(g) - i;
		int hSize = 2 * (MathHelper.ceil(g) + i);
		int vSize = 2 * (2 + i);

		boolean reachable = false;
		search:
		for (int s = x0; s <= x0 + hSize; s++) {
			for (int t = z0; t <= z0 + hSize; t++) {
				if (y0 <= w.getTopY(Heightmap.Type.WORLD_SURFACE, s, t)) {
					reachable = true;
					break search;
				}
			}
		}
		if (!reachable) return;

		BitSet visited = new BitSet(hSize * vSize * hSize);
		BlockPos.Mutable mut = new BlockPos.Mutable();
		BlockPos.Mutable tmp = new BlockPos.Mutable();
		int n = f.size;
		double[] ds = new double[n * 4];

		for (int k = 0; k < n; k++) {
			float t = (float) k / (float) n;
			double px = MathHelper.lerp((double) t, startX, endX);
			double py = MathHelper.lerp((double) t, startY, endY);
			double pz = MathHelper.lerp((double) t, startZ, endZ);
			double h = r.nextDouble() * (double) n / 16.0;
			double rad = ((double) (MathHelper.sin((float) Math.PI * t) + 1.0F) * h + 1.0) / 2.0;
			ds[k * 4] = px;
			ds[k * 4 + 1] = py;
			ds[k * 4 + 2] = pz;
			ds[k * 4 + 3] = rad;
		}

		for (int k = 0; k < n - 1; k++) {
			if (ds[k * 4 + 3] <= 0.0) continue;
			for (int m = k + 1; m < n; m++) {
				if (ds[m * 4 + 3] <= 0.0) continue;
				double dr = ds[k * 4 + 3] - ds[m * 4 + 3];
				double dx = ds[k * 4] - ds[m * 4];
				double dy = ds[k * 4 + 1] - ds[m * 4 + 1];
				double dz = ds[k * 4 + 2] - ds[m * 4 + 2];
				if (dr * dr > dx * dx + dy * dy + dz * dz) {
					if (dr > 0.0) ds[m * 4 + 3] = -1.0;
					else ds[k * 4 + 3] = -1.0;
				}
			}
		}

		for (int k = 0; k < n; k++) {
			double rad = ds[k * 4 + 3];
			if (rad < 0.0) continue;
			double cx = ds[k * 4], cy = ds[k * 4 + 1], cz = ds[k * 4 + 2];
			int minX = Math.max(MathHelper.floor(cx - rad), x0);
			int minY = Math.max(MathHelper.floor(cy - rad), y0);
			int minZ = Math.max(MathHelper.floor(cz - rad), z0);
			int maxX = Math.max(MathHelper.floor(cx + rad), minX);
			int maxY = Math.max(MathHelper.floor(cy + rad), minY);
			int maxZ = Math.max(MathHelper.floor(cz + rad), minZ);

			for (int s = minX; s <= maxX; s++) {
				double u = ((double) s + 0.5 - cx) / rad;
				if (u * u >= 1.0) continue;
				for (int t = minY; t <= maxY; t++) {
					double v = ((double) t + 0.5 - cy) / rad;
					if (u * u + v * v >= 1.0) continue;
					for (int z = minZ; z <= maxZ; z++) {
						double q = ((double) z + 0.5 - cz) / rad;
						if (u * u + v * v + q * q >= 1.0 || w.isOutOfHeightLimit(t)) continue;

						int bit = s - x0 + (t - y0) * hSize + (z - z0) * hSize * vSize;
						if (visited.get(bit)) continue;
						visited.set(bit);

						mut.set(s, t, z);
						long key = mut.asLong();
						if (placed.contains(key)) continue;
						BlockState st = w.getBlockState(mut);
						if (!isTarget(st)) continue;
						if (keep(r, f.discard) || !exposed(w, mut, tmp)) {
							placed.add(key);
							out.add(new Hit(key, f.ore));
						}
					}
				}
			}
		}
	}
}
