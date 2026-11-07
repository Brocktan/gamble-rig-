package com.fakedispenser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/** State for the currently open dispenser GUI. */
public class FakeState {
	public static int syncId = -999;
	public static BlockPos openPos = null;
	public static final ItemStack[] insideReal = new ItemStack[9];
	public static final int[] insideVar = new int[9];

	static {
		Arrays.fill(insideVar, -1);
	}

	public static void reset() {
		Arrays.fill(insideReal, null);
		Arrays.fill(insideVar, -1);
	}

	public static Config.Entry entry(MinecraftClient mc) {
		if (openPos == null || mc.world == null) return null;
		String dim = mc.world.getRegistryKey().getValue().toString();
		return Config.find(dim, openPos.getX(), openPos.getY(), openPos.getZ());
	}

	/** Swap the 9 dispenser slots to fake (or back to real) depending on the toggles. */
	public static void refreshInside(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) return;
		if (!(mc.player.currentScreenHandler instanceof Generic3x3ContainerScreenHandler h)) return;
		Config.Entry e = entry(mc);
		boolean on = Config.enabled && e != null && e.showInside && !e.items.isEmpty();
		for (int i = 0; i < 9 && i < h.slots.size(); i++) {
			Slot s = h.getSlot(i);
			ItemStack cur = s.getStack();
			if (cur.isEmpty()) {
				insideReal[i] = null;
				insideVar[i] = -1;
				continue;
			}
			boolean isF = e != null && e.matches(cur);
			if (on) {
				if (!isF) {
					int v = insideVar[i];
					if (v < 0 || v >= e.items.size()) {
						v = ThreadLocalRandom.current().nextInt(e.items.size());
						insideVar[i] = v;
					}
					ItemStack t = e.items.get(v).build();
					if (t != null) {
						insideReal[i] = cur.copy();
						s.setStackNoCallbacks(t.copyWithCount(cur.getCount()));
					}
				}
			} else if (isF && insideReal[i] != null) {
				s.setStackNoCallbacks(insideReal[i].copy());
			}
		}
	}
}
