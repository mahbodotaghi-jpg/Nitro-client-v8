package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Look at a respawn anchor and right-click it (or hold G). The macro charges it with glowstone from
 * your hotbar, then switches to the "Explode slot" and detonates it. Anchors only explode outside
 * the Nether, so use it in the Overworld or End.
 */
public class AnchorMacro extends Module {
	private final KeyBinding macroKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
		"key.nitro.anchor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.nitro"));

	private final BoolSetting rightClick = add(new BoolSetting("Right click trigger", true));
	private final NumSetting chargeDelay = add(new NumSetting("Charge delay (ticks)", 1, 0, 10, true));
	private final NumSetting explodeDelay = add(new NumSetting("Switch delay (ticks)", 1, 0, 10, true));
	private final NumSetting explodeSlot = add(new NumSetting("Explode slot", 9, 1, 9, true));
	private final BoolSetting notify = add(new BoolSetting("Action bar hints", true));

	private int cooldown;

	public AnchorMacro() {
		super("Anchor macro", "Right-click or hold G on an anchor: charge, switch, explode.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) {
			cooldown = 0;
			return;
		}
		boolean trigger = macroKey.isPressed() || (rightClick.value && mc.options.useKey.isPressed());
		if (!trigger) { cooldown = 0; return; }
		if (cooldown > 0) { cooldown--; return; }

		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		BlockState state = mc.world.getBlockState(hit.getBlockPos());
		if (!state.isOf(Blocks.RESPAWN_ANCHOR)) return;

		if (mc.world.getDimension().respawnAnchorWorks()) {
			hint("Anchors don't explode in this dimension.");
			cooldown = 30;
			return;
		}

		int charges = state.get(RespawnAnchorBlock.CHARGES);
		if (charges == 0) {
			int slot = findGlowstone();
			if (slot == -1) {
				hint("No glowstone in your hotbar.");
				cooldown = 30;
				return;
			}
			mc.player.getInventory().selectedSlot = slot;
			use(hit);
			cooldown = chargeDelay.i();
		} else {
			mc.player.getInventory().selectedSlot = explodeSlot.i() - 1;
			use(hit);
			cooldown = explodeDelay.i();
		}
	}

	private int findGlowstone() {
		for (int i = 0; i < 9; i++) if (mc.player.getInventory().getStack(i).isOf(Items.GLOWSTONE)) return i;
		return -1;
	}

	private void use(BlockHitResult hit) {
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		mc.player.swingHand(Hand.MAIN_HAND);
	}

	private void hint(String text) {
		if (notify.value && mc.player != null) mc.player.sendMessage(Text.literal("\u00a7b[Anchor] \u00a7f" + text), true);
	}
}
