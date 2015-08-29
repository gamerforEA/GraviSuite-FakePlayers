package com.gamerforea.gravisuite;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

import java.io.File;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.config.Configuration;

public final class EventConfig
{
	public static boolean advDDrillEvent = true;
	public static boolean disableAdvDDrillBigHoleMode = false;
	public static boolean disableGraviToolWrenchMode = false;

	static
	{
		File mainDirectory = FMLCommonHandler.instance().getMinecraftServerInstance().getFile(".");
		Configuration config = new Configuration(new File(mainDirectory, "config/Events/GraviSuite.cfg"));
		config.load();
		advDDrillEvent = config.getBoolean("advDDrillEvent", CATEGORY_GENERAL, advDDrillEvent, "Улучшенный алмазный бур");
		disableAdvDDrillBigHoleMode = config.getBoolean("disableAdvDDrillBigHoleMode", "other", disableAdvDDrillBigHoleMode, "Выключить режим улучшенного алмазного бура \"Большие дыры\"");
		disableGraviToolWrenchMode = config.getBoolean("disableGraviToolWrenchMode", "other", disableGraviToolWrenchMode, "Выключить режим гравитационного инструмента \"Гаечный ключ\"");
		config.save();
	}
}