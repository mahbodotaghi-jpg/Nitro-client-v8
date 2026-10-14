package dev.nitro.module.impl;

import dev.nitro.module.Module;
import dev.nitro.setting.BoolSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.text.Text;

/**
 * Removes an end crystal on your client the instant you hit it, instead of waiting for the server's
 * reply, so you can place the next one immediately. The attack packet is still sent as normal.
 * Called from ClientPlayerInteractionManagerMixin.
 */
public class CrystalOptimizer extends Module {
	private final BoolSetting notify = add(new BoolSetting("Action bar confirm", false));

	public CrystalOptimizer() {
		super("Crystal optimizer", "Instantly removes crystals you hit (client side).", Category.COMBAT);
	}

	public void onAttack(Entity target) {
		if (!(target instanceof EndCrystalEntity) || target.isRemoved()) return;
		target.setRemoved(Entity.RemovalReason.KILLED);
		target.onRemoved();
		if (notify.value && mc.player != null) mc.player.sendMessage(Text.literal("\u00a7b[Crystal] \u00a7fremoved"), true);
	}
}
