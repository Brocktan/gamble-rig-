package com.fakedispenser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class Config {
	public static final int MAX_ITEMS = 6;

	/** One possible fake item (a dispenser can have several, picked at random). */
	public static class ItemDef {
		public String item = "minecraft:netherite_ingot";
		public String name = "1";
		public int count = 1;
		/** enchantment id -> level */
		public Map<String, Integer> enchants = new LinkedHashMap<>();

		public ItemDef copy() {
			ItemDef d = new ItemDef();
			d.item = item;
			d.name = name;
			d.count = count;
			d.enchants = enchants == null ? new LinkedHashMap<>() : new LinkedHashMap<>(enchants);
			return d;
		}

		/** Name used for the floating label on the ground: bold removed so the font isn't thick. */
		public String groundName() {
			return name == null ? "" : name.replaceAll("(?i)&l", "").replace('&', '\u00a7');
		}

		/** Builds the fake stack, or null if the item id is unknown. */
		public ItemStack build() {
			String id = item == null ? "" : item.trim().toLowerCase();
			if (!id.contains(":")) id = "minecraft:" + id;
			Item found = null;
			for (Item i : Registries.ITEM) {
				if (Registries.ITEM.getId(i).toString().equals(id)) {
					found = i;
					break;
				}
			}
			if (found == null) return null;
			ItemStack stack = new ItemStack(found, Math.max(1, Math.min(count, 99)));
			if (name != null && !name.isEmpty()) {
				stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name.replace('&', '\u00a7')));
			}
			if (enchants != null && !enchants.isEmpty()) {
				ItemEnchantmentsComponent.Builder b = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
				boolean any = false;
				for (RegistryEntry.Reference<Enchantment> ref : allEnchants()) {
					Integer lv = enchants.get(ref.registryKey().getValue().toString());
					if (lv != null && lv > 0) {
						b.add(ref, lv);
						any = true;
					}
				}
				if (any) stack.set(DataComponentTypes.ENCHANTMENTS, b.build());
			}
			return stack;
		}

		public boolean matches(ItemStack s) {
			ItemStack b = build();
			return b != null && s.isOf(b.getItem())
					&& Objects.equals(s.get(DataComponentTypes.CUSTOM_NAME), b.get(DataComponentTypes.CUSTOM_NAME))
					&& Objects.equals(s.get(DataComponentTypes.ENCHANTMENTS), b.get(DataComponentTypes.ENCHANTMENTS));
		}
	}

	public static class Entry {
		public String dimension = "minecraft:overworld";
		public int x, y, z;
		/** show the fake items inside this dispenser's GUI */
		public boolean showInside = true;
		public List<ItemDef> items = new ArrayList<>();

		public Entry() {
			items.add(new ItemDef());
		}

		public boolean at(String dim, int px, int py, int pz) {
			return dimension.equals(dim) && x == px && y == py && z == pz;
		}

		public Entry copy() {
			Entry e = new Entry();
			e.dimension = dimension;
			e.x = x;
			e.y = y;
			e.z = z;
			e.showInside = showInside;
			e.items.clear();
			if (items != null) for (ItemDef d : items) e.items.add(d.copy());
			return e;
		}

		/** Uniform random pick (2 items = 50/50). */
		public ItemDef pick() {
			return items.get(ThreadLocalRandom.current().nextInt(items.size()));
		}

		public boolean matches(ItemStack s) {
			if (items == null) return false;
			for (ItemDef d : items) if (d.matches(s)) return true;
			return false;
		}
	}

	private static class Data {
		boolean enabled = true;
		List<Entry> dispensers = new ArrayList<>();
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("fakedispenser2.json");
	public static List<Entry> entries = new ArrayList<>();
	public static boolean enabled = true;

	public static List<RegistryEntry.Reference<Enchantment>> allEnchants() {
		MinecraftClient mc = MinecraftClient.getInstance();
		List<RegistryEntry.Reference<Enchantment>> list = new ArrayList<>();
		if (mc.world == null) return list;
		mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).streamEntries().forEach(list::add);
		list.sort(Comparator.comparing(r -> r.registryKey().getValue().toString()));
		return list;
	}

	public static boolean isFake(ItemStack s) {
		if (!enabled || s == null || s.isEmpty() || entries.isEmpty()) return false;
		for (Entry e : entries) if (e.matches(s)) return true;
		return false;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE)) {
					Data d = GSON.fromJson(r, Data.class);
					if (d != null) {
						enabled = d.enabled;
						if (d.dispensers != null) entries = d.dispensers;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		try (Writer w = Files.newBufferedWriter(FILE)) {
			Data d = new Data();
			d.enabled = enabled;
			d.dispensers = entries;
			GSON.toJson(d, w);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Entry find(String dim, int x, int y, int z) {
		for (Entry e : entries) if (e.at(dim, x, y, z)) return e;
		return null;
	}
}
