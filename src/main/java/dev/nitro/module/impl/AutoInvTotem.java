package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.setting.BoolSetting;
import dev.nitro.setting.NumSetting;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;

public class AutoInvTotem extends Module {
	private final NumSetting delay = add(new NumSetting("Delay (ms)", 60, 0, 300, true));
	private final BoolSetting fillHotbar = add(new BoolSetting("Fill hotbar slot", true));
	private final NumSetting hotbarSlot = add(new NumSetting("Hotbar slot", 9, 1, 9, true));
	private int cooldown;

	public AutoInvTotem() {
		super("Auto inv totem", "Equips and restocks totems while your inventory is open.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || !(mc.currentScreen instanceof InventoryScreen)) return;
		if (cooldown > 0) { cooldown--; return; }

		PlayerScreenHandler h = mc.player.playerScreenHandler;

		if (!InvUtil.isTotem(mc.player.getOffHandStack())) {
			int src = findTotem(h, 9, 44);
			if (src != -1) {
				InvUtil.swap(h.syncId, src, 40);
				cooldown = delay.i() / 50;
			}
			return;
		}

		if (fillHotbar.value) {
			int hb = hotbarSlot.i() - 1;
			if (!InvUtil.isTotem(h.getSlot(36 + hb).getStack())) {
				int src = findTotem(h, 9, 35);
				if (src != -1) {
					InvUtil.swap(h.syncId, src, hb);
					cooldown = delay.i() / 50;
				}
			}
		}
	}

	private static int findTotem(PlayerScreenHandler h, int from, int to) {
		for (int i = from; i <= to; i++) if (InvUtil.isTotem(h.getSlot(i).getStack())) return i;
		return -1;
	}
}
