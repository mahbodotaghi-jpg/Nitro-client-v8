package dev.nitro.gui;

import dev.nitro.Config;
import dev.nitro.Theme;
import dev.nitro.module.Module;
import dev.nitro.module.ModuleManager;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import dev.nitro.setting.Setting;
import dev.nitro.setting.TextSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/** Dark bordered panels, accent-coloured headers and pill toggles. Left click toggles, right click expands. */
public class ClickGuiScreen extends Screen {
	private static final int W = 132, HEADER = 18, ROW = 15, SROW = 13;
	private static final int BG = 0xF0141619, WHITE = 0xFFF2F2F2, GRAY = 0xFF9AA0AD;
	private static final int HOVER = 0xFF1D2028, SUB = 0xFF0F1116, LINE = 0xFF262A33, OFF = 0xFF2B2F3A, DARK = 0xFF0B0E14;

	private static final int[] PX = {16, 156, 296};
	private static final int[] PY = {16, 16, 16};

	private record Region(int x, int y, int w, int h, Runnable left, Runnable right, Runnable mid, DoubleConsumer drag) {
		boolean in(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final List<Region> regions = new ArrayList<>();
	private int dragPanel = -1, dragX, dragY;
	private double curX, curY;
	private DoubleConsumer activeDrag;
	private Module binding;
	private TextSetting focused;

	public ClickGuiScreen() {
		super(Text.literal("Nitro"));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void removed() {
		Config.save();
	}

	// ------------------------------------------------------------ drawing

	@Override
	public void render(DrawContext g, int mx, int my, float delta) {
		regions.clear();
		g.fill(0, 0, width, height, 0x55000000);
		panel(g, 0, "COMBAT", Module.Category.COMBAT, mx, my);
		panel(g, 1, "RENDER", Module.Category.RENDER, mx, my);
		clientPanel(g, 2, mx, my);
	}

	private void header(DrawContext g, int idx, String title, int x, int y) {
		g.drawText(textRenderer, title, x + (W - textRenderer.getWidth(title)) / 2, y + 5, Theme.accent, false);
		regions.add(new Region(x, y, W, HEADER, () -> {
			dragPanel = idx;
			dragX = (int) curX - x;
			dragY = (int) curY - y;
		}, null, null, null));
	}

	private void pill(DrawContext g, int x, int y, boolean on) {
		g.fill(x, y, x + 20, y + 8, on ? Theme.accent : OFF);
		int kx = on ? x + 12 : x + 2;
		g.fill(kx, y + 2, kx + 6, y + 6, on ? DARK : WHITE);
	}

	private int sh(Setting s) {
		return s instanceof TextSetting ? SROW * 2 : SROW;
	}

	private void panel(DrawContext g, int idx, String title, Module.Category cat, int mx, int my) {
		List<Module> mods = ModuleManager.in(cat);
		int x = PX[idx], y = PY[idx];

		int h = HEADER;
		for (Module m : mods) {
			h += ROW;
			if (m.expanded) {
				h += SROW;
				for (Setting s : m.settings) h += sh(s);
			}
		}

		g.fill(x, y, x + W, y + h, BG);
		g.drawBorder(x, y, W, h, WHITE);
		header(g, idx, title, x, y);

		int ry = y + HEADER;
		for (Module m : mods) {
			boolean hov = mx >= x && mx < x + W && my >= ry && my < ry + ROW;
			g.fill(x + 1, ry, x + W - 1, ry + 1, LINE);
			if (hov) g.fill(x + 1, ry + 1, x + W - 1, ry + ROW, HOVER);
			if (m.enabled) g.fill(x + 1, ry, x + 3, ry + ROW, Theme.accent);
			g.drawText(textRenderer, m.name.toUpperCase(), x + 8, ry + 4, m.enabled ? Theme.accent : WHITE, false);
			pill(g, x + W - 26, ry + 4, m.enabled);
			final Module mm = m;
			regions.add(new Region(x, ry, W, ROW, mm::toggle, () -> mm.expanded = !mm.expanded, null, null));
			ry += ROW;

			if (m.expanded) {
				String bt = binding == m ? "PRESS A KEY" : "BIND: " + keyName(m.key);
				g.fill(x + 1, ry, x + W - 1, ry + SROW, SUB);
				g.drawText(textRenderer, bt, x + 8, ry + 3, GRAY, false);
				regions.add(new Region(x, ry, W, SROW, () -> binding = mm, () -> mm.key = -1, () -> binding = mm, null));
				ry += SROW;
				for (Setting s : m.settings) ry += setting(g, s, x, ry);
			}
		}
	}

	private int setting(DrawContext g, Setting s, int x, int y) {
		int h = sh(s);
		g.fill(x + 1, y, x + W - 1, y + h, SUB);
		String label = s.name.toUpperCase();

		if (s instanceof BoolSetting b) {
			g.drawText(textRenderer, label, x + 8, y + 3, b.value ? WHITE : GRAY, false);
			pill(g, x + W - 26, y + 2, b.value);
			regions.add(new Region(x, y, W, h, () -> b.value = !b.value, null, null, null));
		} else if (s instanceof NumSetting n) {
			double f = (n.value - n.min) / (n.max - n.min);
			g.fill(x + 4, y + 1, x + 4 + (int) ((W - 8) * f), y + h - 1, (Theme.accent & 0x00FFFFFF) | 0x55000000);
			String val = n.integer ? String.valueOf(n.i()) : String.valueOf(n.value);
			String text = textRenderer.trimToWidth(label, W - 12 - textRenderer.getWidth(val), false);
			g.drawText(textRenderer, text, x + 8, y + 3, WHITE, false);
			g.drawText(textRenderer, val, x + W - 6 - textRenderer.getWidth(val), y + 3, WHITE, false);
			regions.add(new Region(x, y, W, h, null, null, null,
				mouseX -> n.set(n.min + Math.max(0.0, Math.min(1.0, (mouseX - (x + 4)) / (double) (W - 8))) * (n.max - n.min))));
		} else if (s instanceof TextSetting t) {
			boolean foc = focused == t;
			g.drawText(textRenderer, label, x + 8, y + 3, GRAY, false);
			String v = t.value + (foc && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
			v = textRenderer.trimToWidth(v, W - 16, true);
			g.fill(x + 4, y + SROW, x + W - 4, y + h - 1, 0xFF14161C);
			g.drawBorder(x + 4, y + SROW, W - 8, h - SROW - 1, foc ? Theme.accent : LINE);
			g.drawText(textRenderer, v, x + 7, y + SROW + 2, WHITE, false);
			regions.add(new Region(x, y, W, h, () -> focused = t, null, null, null));
		}
		return h;
	}

	private int toggleRow(DrawContext g, int x, int ry, int mx, int my, String label, boolean on, Runnable toggle) {
		boolean hov = mx >= x && mx < x + W && my >= ry && my < ry + ROW;
		g.fill(x + 1, ry, x + W - 1, ry + 1, LINE);
		if (hov) g.fill(x + 1, ry + 1, x + W - 1, ry + ROW, HOVER);
		if (on) g.fill(x + 1, ry, x + 3, ry + ROW, Theme.accent);
		g.drawText(textRenderer, label, x + 8, ry + 4, on ? Theme.accent : WHITE, false);
		pill(g, x + W - 26, ry + 4, on);
		regions.add(new Region(x, ry, W, ROW, toggle, null, null, null));
		return ry + ROW;
	}

	private void clientPanel(DrawContext g, int idx, int mx, int my) {
		int x = PX[idx], y = PY[idx];
		int h = HEADER + ROW * 3 + SROW * 2;
		g.fill(x, y, x + W, y + h, BG);
		g.drawBorder(x, y, W, h, WHITE);
		header(g, idx, "CLIENT", x, y);

		int ry = y + HEADER;
		ry = toggleRow(g, x, ry, mx, my, "ARRAYLIST", Theme.hud, () -> Theme.hud = !Theme.hud);
		ry = toggleRow(g, x, ry, mx, my, "WATERMARK", Theme.watermark, () -> Theme.watermark = !Theme.watermark);
		ry += setting(g, Theme.watermarkText, x, ry);

		g.fill(x + 1, ry, x + W - 1, ry + 1, LINE);
		g.drawText(textRenderer, "ACCENT", x + 8, ry + 4, GRAY, false);
		for (int i = 0; i < Theme.ACCENTS.length; i++) {
			int c = Theme.ACCENTS[i];
			int sx = x + 56 + i * 17;
			g.fill(sx, ry + 3, sx + 12, ry + 13, c);
			if (Theme.accent == c) g.drawBorder(sx - 1, ry + 2, 14, 12, WHITE);
			regions.add(new Region(sx, ry + 2, 13, 12, () -> Theme.accent = c, null, null, null));
		}
	}

	private static String keyName(int key) {
		if (key <= 0) return "NONE";
		return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase();
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		curX = mx;
		curY = my;
		binding = null;
		focused = null;
		for (int i = regions.size() - 1; i >= 0; i--) {
			Region r = regions.get(i);
			if (!r.in(mx, my)) continue;
			if (button == 0 && r.drag() != null) {
				activeDrag = r.drag();
				activeDrag.accept(mx);
			}
			Runnable action = button == 0 ? r.left() : button == 1 ? r.right() : button == 2 ? r.mid() : null;
			if (action != null) action.run();
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (dragPanel >= 0) {
			PX[dragPanel] = (int) mx - dragX;
			PY[dragPanel] = (int) my - dragY;
			return true;
		}
		if (activeDrag != null) {
			activeDrag.accept(mx);
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		dragPanel = -1;
		activeDrag = null;
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (binding != null) {
			binding.key = (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) ? -1 : keyCode;
			binding = null;
			return true;
		}
		if (focused != null) {
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (!focused.value.isEmpty()) focused.value = focused.value.substring(0, focused.value.length() - 1);
			} else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
				focused = null;
			} else if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
				String clip = MinecraftClient.getInstance().keyboard.getClipboard();
				if (clip != null) focused.value = (focused.value + clip.trim()).substring(0, Math.min(64, (focused.value + clip.trim()).length()));
			}
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (focused != null && chr >= 32 && chr != 127 && focused.value.length() < 64) {
			focused.value += chr;
			return true;
		}
		return super.charTyped(chr, modifiers);
	}
}
