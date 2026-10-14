package dev.nitro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nitro.module.Module;
import dev.nitro.module.ModuleManager;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import dev.nitro.setting.Setting;
import dev.nitro.setting.TextSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nitro.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Config() {}

	public static void save() {
		try {
			JsonObject root = new JsonObject();
			root.addProperty("accent", Theme.accent);
			root.addProperty("hud", Theme.hud);
			root.addProperty("watermark", Theme.watermark);
			root.addProperty("watermarkText", Theme.watermarkText.value);
			JsonObject mods = new JsonObject();
			for (Module m : ModuleManager.all()) {
				JsonObject o = new JsonObject();
				o.addProperty("key", m.key);
				JsonObject s = new JsonObject();
				for (Setting st : m.settings) {
					if (st instanceof BoolSetting b) s.addProperty(st.name, b.value);
					else if (st instanceof NumSetting n) s.addProperty(st.name, n.value);
					else if (st instanceof TextSetting t) s.addProperty(st.name, t.value);
				}
				o.add("settings", s);
				mods.add(m.name, o);
			}
			root.add("modules", mods);
			Files.writeString(FILE, GSON.toJson(root));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void load() {
		if (!Files.exists(FILE)) return;
		try {
			JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
			if (root.has("accent")) Theme.accent = root.get("accent").getAsInt();
			if (root.has("hud")) Theme.hud = root.get("hud").getAsBoolean();
			if (root.has("watermark")) Theme.watermark = root.get("watermark").getAsBoolean();
			if (root.has("watermarkText")) Theme.watermarkText.value = root.get("watermarkText").getAsString();
			if (!root.has("modules")) return;
			JsonObject mods = root.getAsJsonObject("modules");
			for (Module m : ModuleManager.all()) {
				if (!mods.has(m.name)) continue;
				JsonObject o = mods.getAsJsonObject(m.name);
				if (o.has("key")) m.key = o.get("key").getAsInt();
				if (!o.has("settings")) continue;
				JsonObject s = o.getAsJsonObject("settings");
				for (Setting st : m.settings) {
					if (!s.has(st.name)) continue;
					if (st instanceof BoolSetting b) b.value = s.get(st.name).getAsBoolean();
					else if (st instanceof NumSetting n) n.set(s.get(st.name).getAsDouble());
					else if (st instanceof TextSetting t) t.value = s.get(st.name).getAsString();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
