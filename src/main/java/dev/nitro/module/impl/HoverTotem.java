package dev.nitro.module.impl;

import dev.nitro.mixin.HandledScreenAccessor;
import dev.nitro.module.Module;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

public class HoverTotem extends Module {
	private final NumSetting delay = add(new NumSetting("Delay (ms)", 40, 0, 300, true));
	private final BoolSetting fillHotbar = add(new BoolSetting("Fill hotbar slot", true));
	private final NumSetting hotbarSlot = add(new NumSetting("Hotbar slot", 9, 1, 9, true));
	private int cooldown;

	public HoverTotem() {
		super("Hover totem", "Hover a totem in any inventory to equip it.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || !(mc.currentScreen instanceof HandledScreen<?> screen)) return;
		if (cooldown > 0) { cooldown--; return; }

		Slot hovered = ((HandledScreenAccessor) screen).nitro$getFocusedSlot();
		if (hovered == null || !hovered.hasStack() || !InvUtil.isTotem(hovered.getStack())) return;
		// ignore armor / offhand / crafting-side slots of the player inventory
		if (hovered.inventory == mc.player.getInventory() && hovered.getIndex() > 35) return;

		int sync = screen.getScreenHandler().syncId;
		if (!InvUtil.isTotem(mc.player.getOffHandStack())) {
			InvUtil.swap(sync, hovered.id, 40);
			cooldown = delay.i() / 50;
		} else if (fillHotbar.value) {
			int hb = hotbarSlot.i() - 1;
			if (!InvUtil.isTotem(mc.player.getInventory().getStack(hb))) {
				InvUtil.swap(sync, hovered.id, hb);
				cooldown = delay.i() / 50;
			}
		}
	}
}
