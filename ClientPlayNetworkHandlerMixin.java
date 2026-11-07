package com.fakedispenser.mixin;

import com.fakedispenser.Config;
import com.fakedispenser.FakeStackHolder;
import com.fakedispenser.FakeState;
import com.fakedispenser.FakeUtil;
import com.fakedispenser.PickupTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Unique
	private ItemStack fakedispenser$prev;
	@Unique
	private final Map<Integer, ItemStack> fakedispenser$prevAll = new HashMap<>();

	// Player picks up a faked item entity -> remember what it should turn into
	@Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
	private void fakedispenser$pickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!Config.enabled || !mc.isOnThread() || mc.world == null || mc.player == null) return;
		Entity e = mc.world.getEntityById(packet.getEntityId());
		if (e instanceof ItemEntity ie && packet.getCollectorEntityId() == mc.player.getId()) {
			FakeStackHolder h = (FakeStackHolder) ie;
			ItemStack fake = h.fakedispenser$getFake();
			if (fake == null) return;
			ItemStack real = h.fakedispenser$getReal();
			PickupTracker.Pending p = new PickupTracker.Pending();
			p.fake = fake.copy();
			p.real = real.getItem();
			p.amount = packet.getStackAmount();
			p.expire = mc.world.getTime() + 40;
			PickupTracker.pending = p;
		}
	}

	@Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
	private void fakedispenser$slotHead(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
		this.fakedispenser$prev = null;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!mc.isOnThread() || mc.player == null) return;
		if (packet.getSyncId() == -1) {
			ItemStack c = mc.player.currentScreenHandler.getCursorStack();
			if (Config.isFake(c)) this.fakedispenser$prev = c.copy();
			return;
		}
		Slot s = FakeUtil.slot(mc, packet.getSyncId(), packet.getSlot());
		if (s != null && Config.isFake(s.getStack())) this.fakedispenser$prev = s.getStack().copy();
	}

	@Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
	private void fakedispenser$slotTail(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.world == null) return;

		// Contents of an open dispenser are handled separately
		if (mc.player.currentScreenHandler instanceof Generic3x3ContainerScreenHandler dh
				&& packet.getSyncId() == dh.syncId && packet.getSlot() >= 0 && packet.getSlot() < 9) {
			this.fakedispenser$prev = null;
			FakeState.refreshInside(mc);
			return;
		}

		ItemStack incoming = packet.getStack();

		// Keep an already-fake stack fake when the server re-sends the real one
		if (this.fakedispenser$prev != null && !incoming.isEmpty()
				&& incoming.getCount() == this.fakedispenser$prev.getCount() && !Config.isFake(incoming)) {
			if (packet.getSyncId() == -1) {
				mc.player.currentScreenHandler.setCursorStack(this.fakedispenser$prev);
			} else {
				Slot s = FakeUtil.slot(mc, packet.getSyncId(), packet.getSlot());
				if (s != null) s.setStackNoCallbacks(this.fakedispenser$prev);
			}
			this.fakedispenser$prev = null;
			return;
		}

		// Freshly picked-up fake item landing in the inventory
		PickupTracker.Pending p = PickupTracker.pending;
		if (p != null) {
			if (mc.world.getTime() > p.expire) {
				PickupTracker.pending = null;
			} else if (Config.enabled && incoming.isOf(p.real) && incoming.getCount() == p.amount && packet.getSyncId() != -1) {
				Slot s = FakeUtil.slot(mc, packet.getSyncId(), packet.getSlot());
				if (s != null) {
					s.setStackNoCallbacks(p.fake.copyWithCount(p.amount));
					PickupTracker.pending = null;
				}
			}
		}
	}

	// Full inventory resync (happens after clicking/moving items, and when a container opens)
	@Inject(method = "onInventory", at = @At("HEAD"))
	private void fakedispenser$invHead(CallbackInfo ci) {
		this.fakedispenser$prevAll.clear();
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!mc.isOnThread() || mc.player == null) return;
		ScreenHandler h = mc.player.currentScreenHandler;
		for (int i = 0; i < h.slots.size(); i++) {
			ItemStack st = h.getSlot(i).getStack();
			if (Config.isFake(st)) this.fakedispenser$prevAll.put(i, st.copy());
		}
		ItemStack cur = h.getCursorStack();
		if (Config.isFake(cur)) this.fakedispenser$prevAll.put(-1, cur.copy());
	}

	@Inject(method = "onInventory", at = @At("TAIL"))
	private void fakedispenser$invTail(CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) return;
		ScreenHandler h = mc.player.currentScreenHandler;
		boolean dispenser = h instanceof Generic3x3ContainerScreenHandler;
		if (dispenser) this.fakedispenser$prevAll.keySet().removeIf(k -> k >= 0 && k < 9);
		for (Map.Entry<Integer, ItemStack> en : this.fakedispenser$prevAll.entrySet()) {
			int i = en.getKey();
			ItemStack fake = en.getValue();
			if (i == -1) {
				ItemStack now = h.getCursorStack();
				if (!now.isEmpty() && now.getCount() == fake.getCount() && !Config.isFake(now)) h.setCursorStack(fake);
			} else if (i < h.slots.size()) {
				Slot s = h.getSlot(i);
				ItemStack now = s.getStack();
				if (!now.isEmpty() && now.getCount() == fake.getCount() && !Config.isFake(now)) s.setStackNoCallbacks(fake);
			}
		}
		this.fakedispenser$prevAll.clear();
		if (dispenser) FakeState.refreshInside(mc);
	}
}
