package com.fakedispenser;

import net.minecraft.item.ItemStack;

public interface FakeStackHolder {
	void fakedispenser$setFake(ItemStack stack);
	ItemStack fakedispenser$getFake();
	/** The real stack the server says this entity holds. */
	ItemStack fakedispenser$getReal();
}
