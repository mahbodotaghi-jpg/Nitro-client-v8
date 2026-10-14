package dev.nitro.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

final class InvUtil {
	private InvUtil() {}

	static boolean isTotem(ItemStack stack) {
		return stack.isOf(Items.TOTEM_OF_UNDYING);
	}

	/** SWAP click: button 40 = offhand, 0-8 = hotbar slot. */
	static void swap(int syncId, int slotId, int button) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.interactionManager == null || mc.player == null) return;
		mc.interactionManager.clickSlot(syncId, slotId, button, SlotActionType.SWAP, mc.player);
	}

	/** Screen-handler slot id (player handler) of the first totem in the inventory, hotbar first. -1 if none. */
	static int findTotemSlot() {
		MinecraftClient mc = MinecraftClient.getInstance();
		for (int i = 0; i < 9; i++) if (isTotem(mc.player.getInventory().getStack(i))) return 36 + i;
		for (int i = 9; i < 36; i++) if (isTotem(mc.player.getInventory().getStack(i))) return i;
		return -1;
	}
}
