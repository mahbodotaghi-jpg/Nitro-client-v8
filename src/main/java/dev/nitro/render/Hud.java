package dev.nitro.render;

import dev.nitro.Theme;
import dev.nitro.module.Module;
import dev.nitro.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Hud {
	private Hud() {}

	public static void render(DrawContext g, RenderTickCounter tick) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options.hudHidden || mc.player == null) return;
		TextRenderer tr = mc.textRenderer;

		String mark = Theme.watermarkText.value;
		if (Theme.watermark && mark != null && !mark.isEmpty()) {
			g.drawText(tr, mark, 4, 4, Theme.accent, true);
		}

		if (!Theme.hud) return;
		List<Module> on = new ArrayList<>();
		for (Module m : ModuleManager.all()) if (m.enabled) on.add(m);
		on.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

		int w = mc.getWindow().getScaledWidth();
		int y = 4;
		for (Module m : on) {
			int tw = tr.getWidth(m.name);
			g.fill(w - tw - 8, y - 1, w - 2, y + 10, 0x90101216);
			g.fill(w - 3, y - 1, w - 1, y + 10, Theme.accent);
			g.drawText(tr, m.name, w - tw - 5, y + 1, Theme.accent, false);
			y += 11;
		}
	}
}
