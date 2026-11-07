package com.fakedispenser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FakeUtil {
	public static Slot slot(MinecraftClient mc, int syncId, int index) {
		if (mc.player == null) return null;
		ScreenHandler h = null;
		if (syncId == 0) h = mc.player.playerScreenHandler;
		else if (syncId == mc.player.currentScreenHandler.syncId) h = mc.player.currentScreenHandler;
		if (h == null || index < 0 || index >= h.slots.size()) return null;
		return h.getSlot(index);
	}
}
