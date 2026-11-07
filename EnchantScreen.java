package com.fakedispenser;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Click an enchantment to raise its level (wraps back to 0 after the max level). */
public class EnchantScreen extends Screen {
	private static final int PER_PAGE = 12;
	private final Screen parent;
	private final Config.ItemDef def;
	private final List<ButtonWidget> blue = new ArrayList<>();
	private int page = 0;

	public EnchantScreen(Screen parent, Config.ItemDef def) {
		super(Text.literal("Enchantments"));
		this.parent = parent;
		this.def = def;
		if (def.enchants == null) def.enchants = new java.util.LinkedHashMap<>();
	}

	private static String pretty(String id) {
		String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		StringBuilder sb = new StringBuilder();
		for (String w : path.split("_")) {
			if (w.isEmpty()) continue;
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return sb.toString();
	}

	private ButtonWidget add(String label, ButtonWidget.PressAction action, int x, int y, int w, int h) {
		ButtonWidget b = ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, w, h).build();
		addDrawableChild(b);
		blue.add(b);
		return b;
	}

	@Override
	protected void init() {
		blue.clear();
		List<RegistryEntry.Reference<Enchantment>> list = Config.allEnchants();
		int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
		if (page >= pages) page = pages - 1;
		int cx = this.width / 2;
		int start = page * PER_PAGE;

		for (int i = 0; i < PER_PAGE && start + i < list.size(); i++) {
			RegistryEntry.Reference<Enchantment> ref = list.get(start + i);
			String id = ref.registryKey().getValue().toString();
			int max = Math.max(1, ref.value().getMaxLevel());
			int lv = def.enchants.getOrDefault(id, 0);
			String label = (lv > 0 ? "\u00a7b" : "") + pretty(id) + ": " + lv;
			int col = i % 2, row = i / 2;
			add(label, b -> {
				int cur = def.enchants.getOrDefault(id, 0);
				int next = cur + 1;
				if (next > max) next = 0;
				if (next == 0) def.enchants.remove(id);
				else def.enchants.put(id, next);
				this.clearAndInit();
			}, cx - 155 + col * 160, 38 + row * 24, 150, 20);
		}

		add("< Prev", b -> {
			if (page > 0) page--;
			this.clearAndInit();
		}, cx - 155, 190, 70, 20);
		add("Clear all", b -> {
			def.enchants.clear();
			this.clearAndInit();
		}, cx - 75, 190, 150, 20);
		add("Next >", b -> {
			if (page < pages - 1) page++;
			this.clearAndInit();
		}, cx + 85, 190, 70, 20);
		add("Back", b -> this.close(), cx - 75, 214, 150, 20);
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		BlueUi.background(ctx, this.width, this.height);
		BlueUi.panel(ctx, this.width / 2 - 165, 6, this.width / 2 + 165, 242);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		BlueUi.paintButtons(ctx, this.textRenderer, blue);
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, BlueUi.TITLE);
		ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Click an enchant to raise its level"),
				this.width / 2, 24, BlueUi.SUB);
	}
}
