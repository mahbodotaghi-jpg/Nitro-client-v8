package dev.nitro;

import dev.nitro.gui.ClickGuiScreen;
import dev.nitro.module.Module;
import dev.nitro.module.ModuleManager;
import dev.nitro.render.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NitroClient implements ClientModInitializer {
	public static final String NAME = "Nitro Client";
	public static final String VERSION = "1.0.0";

	public static KeyBinding guiKey;

	@Override
	public void onInitializeClient() {
		guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.nitro.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nitro"));

		ModuleManager.init();
		Config.load();

		ClientTickEvents.END_CLIENT_TICK.register(this::tick);

		WorldRenderEvents.LAST.register(ctx -> {
			if (ctx.matrixStack() == null) return;
			for (Module m : ModuleManager.all()) {
				if (!m.enabled) continue;
				try {
					m.onRender(ctx);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});

		HudRenderCallback.EVENT.register(Hud::render);
	}

	private void tick(MinecraftClient mc) {
		while (guiKey.wasPressed()) mc.setScreen(new ClickGuiScreen());
		if (mc.getWindow() == null) return;

		long handle = mc.getWindow().getHandle();
		for (Module m : ModuleManager.all()) {
			if (m.key > 0) {
				boolean down = InputUtil.isKeyPressed(handle, m.key);
				if (down && !m.keyWas && mc.currentScreen == null && mc.world != null) m.toggle();
				m.keyWas = down;
			}
			if (m.enabled) {
				try {
					m.onTick();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}
}
