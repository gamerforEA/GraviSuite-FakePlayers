package com.gamerforea.gravisuite;

import java.util.UUID;

import com.gamerforea.eventhelper.util.FastUtils;
import com.mojang.authlib.GameProfile;

import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public final class ModUtils
{
	public static final GameProfile profile = new GameProfile(UUID.fromString("25eed880-f461-49d0-9db2-32369ec2b0e8"), "[GraviSuite]");
	private static FakePlayer player = null;

	public static final FakePlayer getModFake(World world)
	{
		if (player == null)
			player = FastUtils.getFake(world, profile);
		else
			player.worldObj = world;

		return player;
	}
}