package com.fakedispenser.mixin;

import com.fakedispenser.Config;
import com.fakedispenser.FakeStackHolder;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin implements FakeStackHolder {
	@Unique
	private ItemStack fakedispenser$fake;
	@Unique
	private boolean fakedispenser$bypass;

	@Override
	public void fakedispenser$setFake(ItemStack stack) {
		this.fakedispenser$fake = stack;
	}

	@Override
	public ItemStack fakedispenser$getFake() {
		return this.fakedispenser$fake;
	}

	@Override
	public ItemStack fakedispenser$getReal() {
		this.fakedispenser$bypass = true;
		try {
			return ((ItemEntity) (Object) this).getStack();
		} finally {
			this.fakedispenser$bypass = false;
		}
	}

	@Inject(method = "getStack", at = @At("HEAD"), cancellable = true)
	private void fakedispenser$getStack(CallbackInfoReturnable<ItemStack> cir) {
		if (Config.enabled && this.fakedispenser$fake != null && !this.fakedispenser$bypass) {
			cir.setReturnValue(this.fakedispenser$fake);
		}
	}
}
