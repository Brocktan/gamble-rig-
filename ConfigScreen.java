package com.fakedispenser;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
	private final BlockPos pos;
	private final Config.Entry draft;
	private final List<ButtonWidget> blue = new ArrayList<>();
	private TextFieldWidget itemField, nameField, countField;
	private int selected = 0;
	private String status = "";

	public ConfigScreen(BlockPos pos) {
		super(Text.literal("Fake Dispenser"));
		this.pos = pos;
		String dim = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
		Config.Entry existing = Config.find(dim, pos.getX(), pos.getY(), pos.getZ());
		if (existing != null) {
			this.draft = existing.copy();
		} else {
			this.draft = new Config.Entry();
			this.draft.dimension = dim;
			this.draft.x = pos.getX();
			this.draft.y = pos.getY();
			this.draft.z = pos.getZ();
		}
		if (this.draft.items.isEmpty()) this.draft.items.add(new Config.ItemDef());
	}

	private ButtonWidget add(String label, ButtonWidget.PressAction action, int x, int y, int w, int h) {
		ButtonWidget b = ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, w, h).build();
		addDrawableChild(b);
		blue.add(b);
		return b;
	}

	private void syncDraft() {
		if (itemField == null) return;
		Config.ItemDef d = draft.items.get(selected);
		d.item = itemField.getText().trim();
		d.name = nameField.getText();
		try {
			d.count = Integer.parseInt(countField.getText().trim());
		} catch (NumberFormatException ex) {
			d.count = 1;
		}
	}

	@Override
	protected void init() {
		blue.clear();
		if (selected >= draft.items.size()) selected = draft.items.size() - 1;
		Config.ItemDef cur = draft.items.get(selected);
		int cx = this.width / 2;

		// tabs: one per possible item (random pick), plus "+"
		int n = draft.items.size();
		boolean canAdd = n < Config.MAX_ITEMS;
		int tabs = n + (canAdd ? 1 : 0);
		int startX = cx - (tabs * 40 - 4) / 2;
		for (int i = 0; i < n; i++) {
			final int idx = i;
			add(i == selected ? "[" + (i + 1) + "]" : String.valueOf(i + 1), b -> {
				syncDraft();
				selected = idx;
				this.clearAndInit();
			}, startX + i * 40, 40, 36, 18);
		}
		if (canAdd) {
			add("+", b -> {
				syncDraft();
				draft.items.add(new Config.ItemDef());
				selected = draft.items.size() - 1;
				this.clearAndInit();
			}, startX + n * 40, 40, 36, 18);
		}

		itemField = new TextFieldWidget(this.textRenderer, cx - 100, 82, 200, 20, Text.literal("Item"));
		itemField.setMaxLength(100);
		itemField.setText(cur.item);
		addDrawableChild(itemField);

		nameField = new TextFieldWidget(this.textRenderer, cx - 100, 116, 200, 20, Text.literal("Name"));
		nameField.setMaxLength(100);
		nameField.setText(cur.name == null ? "" : cur.name);
		addDrawableChild(nameField);

		countField = new TextFieldWidget(this.textRenderer, cx - 100, 150, 60, 20, Text.literal("Count"));
		countField.setMaxLength(2);
		countField.setText(String.valueOf(cur.count));
		addDrawableChild(countField);

		int ec = cur.enchants == null ? 0 : cur.enchants.size();
		add("Enchants (" + ec + ")...", b -> {
			syncDraft();
			this.client.setScreen(new EnchantScreen(this, draft.items.get(selected)));
		}, cx - 34, 150, 134, 20);

		add("Save", b -> save(), cx - 100, 178, 98, 20);
		add("Delete this item", b -> deleteItem(), cx + 2, 178, 98, 20);
		add("Remove dispenser", b -> remove(), cx - 100, 202, 98, 20);
		add("Done", b -> this.close(), cx + 2, 202, 98, 20);
	}

	private void deleteItem() {
		if (draft.items.size() <= 1) {
			status = "A dispenser needs at least 1 item.";
			return;
		}
		syncDraft();
		draft.items.remove(selected);
		if (selected > 0) selected--;
		this.clearAndInit();
	}

	private void save() {
		syncDraft();
		for (int i = 0; i < draft.items.size(); i++) {
			if (draft.items.get(i).build() == null) {
				status = "Item " + (i + 1) + ": unknown id '" + draft.items.get(i).item + "'";
				return;
			}
		}
		Config.Entry e = draft.copy();
		Config.entries.removeIf(o -> o.at(e.dimension, e.x, e.y, e.z));
		Config.entries.add(e);
		Config.save();
		int n = e.items.size();
		status = n > 1 ? "Saved! Each item has a 1 in " + n + " chance." : "Saved!";
	}

	private void remove() {
		Config.entries.removeIf(o -> o.at(draft.dimension, pos.getX(), pos.getY(), pos.getZ()));
		Config.save();
		status = "Dispenser removed.";
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		BlueUi.background(ctx, this.width, this.height);
		BlueUi.panel(ctx, this.width / 2 - 120, 6, this.width / 2 + 120, 250);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		BlueUi.paintButtons(ctx, this.textRenderer, blue);
		int cx = this.width / 2;
		ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, BlueUi.TITLE);
		ctx.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Dispenser at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
						+ "  -  random pick between items"), cx, 25, BlueUi.SUB);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Item " + (selected + 1) + " ID (e.g. mace)"), cx - 100, 71, BlueUi.TEXT);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Display name (& color codes ok)"), cx - 100, 105, BlueUi.TEXT);
		ctx.drawTextWithShadow(this.textRenderer, Text.literal("Count"), cx - 100, 139, BlueUi.TEXT);
		if (!status.isEmpty()) {
			ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), cx, 230, BlueUi.STATUS);
		}
	}
}
