package com.gamerforea.gravisuite;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import com.gamerforea.eventhelper.util.FastUtils;

import net.minecraftforge.common.config.Configuration;

public final class EventConfig
{
	public static boolean advDDrillEvent = true;
	public static boolean disableAdvDDrillBigHoleMode = false;
	public static boolean disableGraviToolWrenchMode = false;

	static
	{
		Configuration config = FastUtils.getConfig("GraviSuite");
		advDDrillEvent = config.getBoolean("advDDrillEvent", CATEGORY_GENERAL, advDDrillEvent, "Улучшенный алмазный бур");
		disableAdvDDrillBigHoleMode = config.getBoolean("disableAdvDDrillBigHoleMode", "other", disableAdvDDrillBigHoleMode, "Выключить режим улучшенного алмазного бура \"Большие дыры\"");
		disableGraviToolWrenchMode = config.getBoolean("disableGraviToolWrenchMode", "other", disableGraviToolWrenchMode, "Выключить режим гравитационного инструмента \"Гаечный ключ\"");
		config.save();
	}
}