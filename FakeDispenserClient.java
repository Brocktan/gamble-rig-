package com.fakedispenser;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

public class FakeDispenserClient implements ClientModInitializer {
	public static KeyBinding openKey;
	public static KeyBinding toggleKey;

	private static Text insideLabel(boolean on) {
		return Text.literal("Fake items inside: " + (on ? "ON" : "OFF"));
	}

	@Override
	public void onInitializeClient() {
		Config.load();

		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fakedispenser.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.fakedispenser.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Silent global on/off
			while (toggleKey.wasPressed()) {
				Config.enabled = !Config.enabled;
				Config.save();
				FakeState.refreshInside(client);
			}
			// Open the menu for the dispenser you're looking at
			while (openKey.wasPressed()) {
				if (client.world == null || client.player == null || client.currentScreen != null) continue;
				if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
					BlockPos p = hit.getBlockPos();
					if (client.world.getBlockState(p).getBlock() instanceof DispenserBlock) {
						client.setScreen(new ConfigScreen(p.toImmutable()));
						return;
					}
				}
			}
		});

		// Items popping out of a configured dispenser
		ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!Config.enabled) return;
			if (!(entity instanceof ItemEntity itemEntity)) return;
			if (Config.entries.isEmpty()) return;
			String dim = world.getRegistryKey().getValue().toString();
			for (Config.Entry e : Config.entries) {
				if (!e.dimension.equals(dim)) continue;
				BlockPos dp = new BlockPos(e.x, e.y, e.z);
				BlockState st = world.getBlockState(dp);
				if (!(st.getBlock() instanceof DispenserBlock)) continue;
				Direction facing = st.get(DispenserBlock.FACING);
				BlockPos front = dp.offset(facing);
				double dx = itemEntity.getX() - (front.getX() + 0.5);
				double dy = itemEntity.getY() - (front.getY() + 0.5);
				double dz = itemEntity.getZ() - (front.getZ() + 0.5);
				if (dx * dx + dy * dy + dz * dz >= 1.0) continue;
				if (e.items.isEmpty()) continue;
				Config.ItemDef def = e.pick();
				ItemStack fake = def.build();
				if (fake == null) continue;
				((FakeStackHolder) itemEntity).fakedispenser$setFake(fake);
				if (def.name != null && !def.name.isEmpty()) {
					itemEntity.setCustomName(Text.literal(def.groundName()));
				}
				return;
			}
		});

		// Opening a dispenser: add a per-dispenser toggle button and fake the contents
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof Generic3x3ContainerScreen)) return;
			if (client.world == null || client.player == null) return;
			if (client.player.currentScreenHandler.syncId != FakeState.syncId) {
				FakeState.syncId = client.player.currentScreenHandler.syncId;
				FakeState.reset();
				FakeState.openPos = null;
				if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
						&& client.world.getBlockState(hit.getBlockPos()).getBlock() instanceof DispenserBlock) {
					FakeState.openPos = hit.getBlockPos().toImmutable();
				}
			}
			Config.Entry entry = FakeState.entry(client);
			if (entry == null) return;
			ButtonWidget btn = ButtonWidget.builder(insideLabel(entry.showInside), b -> {
				Config.Entry en = FakeState.entry(client);
				if (en == null) return;
				en.showInside = !en.showInside;
				Config.save();
				b.setMessage(insideLabel(en.showInside));
				FakeState.refreshInside(client);
			}).dimensions(scaledWidth / 2 - 75, 6, 150, 20).build();
			Screens.getButtons(screen).add(btn);
			FakeState.refreshInside(client);
		});
	}
}
