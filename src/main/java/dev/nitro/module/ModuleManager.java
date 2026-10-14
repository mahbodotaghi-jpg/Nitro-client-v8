package dev.nitro.module;

import dev.nitro.module.impl.*;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private ModuleManager() {}

	public static void init() {
		MODULES.add(new AnchorMacro());
		MODULES.add(new CrystalOptimizer());
		MODULES.add(new AutoTotem());
		MODULES.add(new HoverTotem());
		MODULES.add(new AutoInvTotem());
		MODULES.add(new StorageESP());
		MODULES.add(new PlayerESP());
		MODULES.add(new FreeCam());
		MODULES.add(new OreSim());
		MODULES.add(new ChunkFinder());
	}

	public static List<Module> all() {
		return MODULES;
	}

	public static List<Module> in(Module.Category category) {
		List<Module> out = new ArrayList<>();
		for (Module m : MODULES) if (m.category == category) out.add(m);
		return out;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T get(Class<T> type) {
		for (Module m : MODULES) if (type.isInstance(m)) return (T) m;
		return null;
	}
}
