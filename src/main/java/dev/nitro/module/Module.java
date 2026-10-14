package dev.nitro.module;

import dev.nitro.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public enum Category { COMBAT, RENDER }

	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting> settings = new ArrayList<>();
	public boolean enabled;
	public boolean expanded;
	public boolean keyWas;
	public int key = -1;

	protected final MinecraftClient mc = MinecraftClient.getInstance();

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <T extends Setting> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean on) {
		if (enabled == on) return;
		enabled = on;
		if (on) onEnable(); else onDisable();
	}

	protected void msg(String text) {
		if (mc.player != null) mc.player.sendMessage(Text.literal("\u00a7b[Nitro] \u00a7f" + text), false);
	}

	public void onEnable() {}
	public void onDisable() {}
	public void onTick() {}
	public void onRender(WorldRenderContext ctx) {}
}
