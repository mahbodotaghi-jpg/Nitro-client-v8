package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.setting.NumSetting;

public class AutoTotem extends Module {
	private final NumSetting delay = add(new NumSetting("Delay (ms)", 50, 0, 300, true));
	private final NumSetting health = add(new NumSetting("Health (20 = always)", 20, 1, 20, true));
	private int cooldown;

	public AutoTotem() {
		super("Auto totem", "Keeps a totem in your offhand.", Category.COMBAT);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.interactionManager == null) return;
		if (cooldown > 0) { cooldown--; return; }
		if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) return;
		if (InvUtil.isTotem(mc.player.getOffHandStack())) return;
		if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.value) return;

		int slot = InvUtil.findTotemSlot();
		if (slot == -1) return;
		InvUtil.swap(mc.player.playerScreenHandler.syncId, slot, 40);
		cooldown = delay.i() / 50;
	}
}
