package studio.interpretation.whales.item;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import studio.interpretation.whales.WhalesMod;
import studio.interpretation.whales.entity.ModEntities;

public class ModItems {
	public static final Item BARNACLE = register("barnacle", Item::new, new Item.Properties());

	public static final Item WHALE_SPAWN_EGG = register(
			"whale_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.WHALE)
	);

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, WhalesMod.id(name));
		T item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.accept(BARNACLE));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(WHALE_SPAWN_EGG));
	}
}
