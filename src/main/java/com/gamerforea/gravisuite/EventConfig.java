package com.gamerforea.gravisuite;

import java.util.Set;

import com.gamerforea.eventhelper.util.FastUtils;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.common.config.Configuration;

public final class EventConfig
{
	public static boolean advDDrillEvent = true;
	public static boolean relocatorToEvent = true;
	public static boolean relocatorFromEvent = true;
	public static boolean disableAdvDDrillBigHoleMode = false;
	public static boolean disableGraviToolWrenchMode = false;
	public static final Set<String> vajraSilkBlackList = Sets.newHashSet("minecraft:stone", "IC2:blockMachine:5");

	static
	{
		Configuration cfg = FastUtils.getConfig("GraviSuite");
		advDDrillEvent = cfg.getBoolean("advDDrillEvent", "general", advDDrillEvent, "Улучшенный алмазный бур");
		relocatorToEvent = cfg.getBoolean("relocatorToEvent", "general", relocatorToEvent, "Релокатор (в приват)");
		relocatorFromEvent = cfg.getBoolean("relocatorFromEvent", "general", relocatorFromEvent, "Релокатор (из привата)");
		disableAdvDDrillBigHoleMode = cfg.getBoolean("disableAdvDDrillBigHoleMode", "other", disableAdvDDrillBigHoleMode, "Выключить режим улучшенного алмазного бура \"Большие дыры\"");
		disableGraviToolWrenchMode = cfg.getBoolean("disableGraviToolWrenchMode", "other", disableGraviToolWrenchMode, "Выключить режим гравитационного инструмента \"Гаечный ключ\"");
		readStringSet(cfg, "vajraSilkBlackList", "general", "Чёрный список блоков для Шёлкового касания Ваджры", vajraSilkBlackList);
		cfg.save();
	}

	public static final boolean inList(Set<String> blackList, Item item, int meta)
	{
		if (item instanceof ItemBlock)
			return inList(blackList, ((ItemBlock) item).field_150939_a, meta);

		return inList(blackList, getId(item), meta);
	}

	public static final boolean inList(Set<String> blackList, Block block, int meta)
	{
		return inList(blackList, getId(block), meta);
	}

	private static final boolean inList(Set<String> blackList, String id, int meta)
	{
		return id != null && (blackList.contains(id) || blackList.contains(id + ':' + meta));
	}

	private static final void readStringSet(final Configuration cfg, final String name, final String category, final String comment, final Set<String> def)
	{
		final Set<String> temp = getStringSet(cfg, name, category, comment, def);
		def.clear();
		def.addAll(temp);
	}

	private static final Set<String> getStringSet(final Configuration cfg, final String name, final String category, final String comment, final Set<String> def)
	{
		return getStringSet(cfg, name, category, comment, def.toArray(new String[def.size()]));
	}

	private static final Set<String> getStringSet(final Configuration cfg, final String name, final String category, final String comment, final String... def)
	{
		return Sets.newHashSet(cfg.getStringList(name, category, def, comment));
	}

	private static final String getId(Item item)
	{
		return GameData.getItemRegistry().getNameForObject(item);
	}

	private static final String getId(Block block)
	{
		return GameData.getBlockRegistry().getNameForObject(block);
	}
}