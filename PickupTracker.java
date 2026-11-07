package com.fakedispenser;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class PickupTracker {
	public static class Pending {
		public ItemStack fake;
		public Item real;
		public int amount;
		public long expire;
	}

	public static Pending pending;
}
