package com.gamerforea.gravisuite;

import org.bukkit.Bukkit;

import com.gamerforea.wgew.cauldron.event.CauldronBlockBreakEvent;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayer;

public final class FakePlayerUtils
{
	public static final boolean cantBreak(EntityPlayer player, int x, int y, int z)
	{
		try
		{
			CauldronBlockBreakEvent event = new CauldronBlockBreakEvent(player, x, y, z);
			Bukkit.getServer().getPluginManager().callEvent(event);
			return event.getBukkitEvent().isCancelled();
		}
		catch (Throwable t)
		{
			GameProfile profile = player.getGameProfile();
			System.err.println(String.format("Failed call CauldronBlockBreakEvent [Name: %s, UUID: %s, X: %d, Y: %d, Z: %d]", profile.getName(), profile.getId().toString(), x, y, z));
			return true;
		}
	}
}