package gravisuite;

import java.awt.Color;
import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class Helpers
{
	public static boolean setBlockToAir(World w, int x, int y, int z)
	{
		return w.setBlock(x, y, z, Blocks.air, 0, 0);
	}

	public static Block getBlock(ItemStack stack)
	{
		Item item = stack.getItem();
		return item instanceof ItemBlock ? ((ItemBlock) item).field_150939_a : null;
	}

	public static boolean equals(Block block, ItemStack stack)
	{
		return block == getBlock(stack);
	}

	public static String formatMessage(String inputString)
	{
		ChatComponentTranslation cht = new ChatComponentTranslation(inputString, new Object[0]);
		return StatCollector.translateToLocal(cht.getUnformattedTextForChat());
	}

	public static int convertRGBcolorToInt(int r, int g, int b)
	{
		float divColor = 255.0F;
		Color tmpColor = new Color(r / divColor, g / divColor, b / divColor);
		return tmpColor.getRGB();
	}

	public static Color convertRGBtoColor(int r, int g, int b)
	{
		float divColor = 255.0F;
		Color tmpColor = new Color(r / divColor, g / divColor, b / divColor);
		return tmpColor;
	}

	public static void renderTooltip(int x, int y, List<String> tooltipData)
	{
		int color = convertRGBcolorToInt(0, 149, 218);
		int color2 = convertRGBcolorToInt(119, 187, 218);
		GL11.glDisable('耺');
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(2896);
		GL11.glDisable(2929);
		if (!tooltipData.isEmpty())
		{
			int var5 = 0;
			FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;

			for (int var6 = 0; var6 < tooltipData.size(); ++var6)
			{
				int var7 = fontRenderer.getStringWidth(tooltipData.get(var6));
				if (var7 > var5)
					var5 = var7;
			}

			int var141 = x + 12;
			int var7 = y - 12;
			int var9 = 8;
			if (tooltipData.size() > 1)
				var9 += 2 + (tooltipData.size() - 1) * 10;

			float z = 300.0F;
			drawGradientRect(var141 - 3, var7 - 4, z, var141 + var5 + 3, var7 - 3, color2, color2);
			drawGradientRect(var141 - 3, var7 + var9 + 3, z, var141 + var5 + 3, var7 + var9 + 4, color2, color2);
			drawGradientRect(var141 - 3, var7 - 3, z, var141 + var5 + 3, var7 + var9 + 3, color2, color2);
			drawGradientRect(var141 - 4, var7 - 3, z, var141 - 3, var7 + var9 + 3, color2, color2);
			drawGradientRect(var141 + var5 + 3, var7 - 3, z, var141 + var5 + 4, var7 + var9 + 3, color2, color2);
			int var12 = (color & 16777215) >> 1 | color & -16777216;
			drawGradientRect(var141 - 3, var7 - 3 + 1, z, var141 - 3 + 1, var7 + var9 + 3 - 1, color, var12);
			drawGradientRect(var141 + var5 + 2, var7 - 3 + 1, z, var141 + var5 + 3, var7 + var9 + 3 - 1, color, var12);
			drawGradientRect(var141 - 3, var7 - 3, z, var141 + var5 + 3, var7 - 3 + 1, color, color);
			drawGradientRect(var141 - 3, var7 + var9 + 2, z, var141 + var5 + 3, var7 + var9 + 3, var12, var12);

			for (int var13 = 0; var13 < tooltipData.size(); ++var13)
			{
				String var14 = tooltipData.get(var13);
				fontRenderer.drawStringWithShadow(var14, var141, var7, -1);
				if (var13 == 0)
					var7 += 2;

				var7 += 10;
			}
		}

	}

	public static void drawGradientRect(int par1, int par2, float z, int par3, int par4, int par5, int par6)
	{
		float var7 = (par5 >> 24 & 255) / 255.0F;
		float var8 = (par5 >> 16 & 255) / 255.0F;
		float var9 = (par5 >> 8 & 255) / 255.0F;
		float var10 = (par5 & 255) / 255.0F;
		float var11 = (par6 >> 24 & 255) / 255.0F;
		float var12 = (par6 >> 16 & 255) / 255.0F;
		float var13 = (par6 >> 8 & 255) / 255.0F;
		float var14 = (par6 & 255) / 255.0F;
		GL11.glDisable(3553);
		GL11.glEnable(3042);
		GL11.glDisable(3008);
		GL11.glBlendFunc(770, 771);
		GL11.glShadeModel(7425);
		Tessellator var15 = Tessellator.instance;
		var15.startDrawingQuads();
		var15.setColorRGBA_F(var8, var9, var10, var7);
		var15.addVertex(par3, par2, z);
		var15.addVertex(par1, par2, z);
		var15.setColorRGBA_F(var12, var13, var14, var11);
		var15.addVertex(par1, par4, z);
		var15.addVertex(par3, par4, z);
		var15.draw();
		GL11.glShadeModel(7424);
		GL11.glDisable(3042);
		GL11.glEnable(3008);
		GL11.glEnable(3553);
	}

	public static Entity teleportEntity(Entity entity, ItemRelocator.TeleportPoint tpPoint)
	{
		// TODO gamerforEA code start
		if (entity == null || entity.worldObj == null)
			return entity;
		if (tpPoint == null)
			return entity;
		// TODO gamerforEA code end

		boolean changeDim = entity.worldObj.provider.dimensionId != tpPoint.dimID;
		if (changeDim)
			teleportToDimensionNew(entity, tpPoint);
		else
		{
			Entity mount = entity.ridingEntity;
			if (entity.ridingEntity != null)
			{
				entity.mountEntity((Entity) null);
				mount = teleportEntity(mount, tpPoint);
			}

			if (entity instanceof EntityPlayerMP)
			{
				EntityPlayerMP player = (EntityPlayerMP) entity;
				player.setPositionAndUpdate(tpPoint.x, tpPoint.y, tpPoint.z);
			}
			else
				entity.setPosition(tpPoint.x, tpPoint.y, tpPoint.z);

			entity.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			if (mount != null)
				entity.mountEntity(mount);
		}

		return entity;
	}

	public static void teleportToDimension(Entity entity, int targetDimID, ItemRelocator.TeleportPoint tpPoint)
	{
		if (GraviSuite.isSimulating())
		{
			EntityPlayerMP player = (EntityPlayerMP) entity;
			int currentDim = entity.worldObj.provider.dimensionId;
			MinecraftServer minecraftserver = MinecraftServer.getServer();
			WorldServer currentServer = minecraftserver.worldServerForDimension(currentDim);
			WorldServer targetServer = minecraftserver.worldServerForDimension(targetDimID);
			player.dimension = targetDimID;
			player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, player.worldObj.difficultySetting, player.worldObj.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));
			currentServer.removePlayerEntityDangerously(player);
			player.isDead = false;
			player.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			targetServer.theChunkProviderServer.loadChunk((int) tpPoint.x >> 4, (int) tpPoint.z >> 4);
			targetServer.spawnEntityInWorld(player);
			targetServer.updateEntityWithOptionalForce(player, false);
			player.setWorld(targetServer);
			if (currentServer != null)
				currentServer.getPlayerManager().removePlayer(player);

			targetServer.getPlayerManager().addPlayer(player);
			targetServer.theChunkProviderServer.loadChunk((int) player.posX >> 4, (int) player.posZ >> 4);
			player.playerNetServerHandler.setPlayerLocation(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			player.theItemInWorldManager.setWorld(targetServer);
			player.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(player, targetServer);
			player.mcServer.getConfigurationManager().syncPlayerInventory(player);
			FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, currentDim, targetDimID);
			player.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			targetServer.updateEntityWithOptionalForce(player, true);
		}

	}

	public static Entity teleportToDimensionNew(Entity entity, ItemRelocator.TeleportPoint tpPoint)
	{
		if (GraviSuite.isSimulating())
		{
			Entity mount = entity.ridingEntity;
			if (entity.ridingEntity != null)
			{
				entity.mountEntity((Entity) null);
				mount = teleportToDimensionNew(mount, tpPoint);
			}

			System.out.println("Teleport entity: " + entity.toString());
			tpPoint.y += 0.5D;
			int currentDim = entity.worldObj.provider.dimensionId;
			MinecraftServer minecraftserver = MinecraftServer.getServer();
			WorldServer currentServer = minecraftserver.worldServerForDimension(currentDim);
			WorldServer targetServer = minecraftserver.worldServerForDimension(tpPoint.dimID);
			currentServer.updateEntityWithOptionalForce(entity, false);
			if (entity instanceof EntityPlayerMP)
			{
				EntityPlayerMP player = (EntityPlayerMP) entity;
				player.dimension = tpPoint.dimID;
				player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, player.worldObj.difficultySetting, player.worldObj.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));
				currentServer.removePlayerEntityDangerously(player);
				player.isDead = false;
			}
			else
			{
				entity.dimension = tpPoint.dimID;
				entity.isDead = false;
			}

			entity.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			targetServer.theChunkProviderServer.loadChunk((int) tpPoint.x >> 4, (int) tpPoint.z >> 4);
			targetServer.spawnEntityInWorld(entity);
			targetServer.updateEntityWithOptionalForce(entity, false);
			entity.setWorld(targetServer);
			if (!(entity instanceof EntityPlayerMP))
			{
				NBTTagCompound entityNBT = new NBTTagCompound();
				entity.isDead = false;
				entity.writeToNBTOptional(entityNBT);
				entity.isDead = true;
				entity = EntityList.createEntityFromNBT(entityNBT, targetServer);
				if (entity == null)
					return null;

				entity.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
				targetServer.spawnEntityInWorld(entity);
				entity.setWorld(targetServer);
				entity.dimension = tpPoint.dimID;
			}

			if (entity instanceof EntityPlayerMP)
			{
				EntityPlayerMP player = (EntityPlayerMP) entity;
				if (currentServer != null)
					currentServer.getPlayerManager().removePlayer(player);

				targetServer.getPlayerManager().addPlayer(player);
				targetServer.theChunkProviderServer.loadChunk((int) player.posX >> 4, (int) player.posZ >> 4);
				targetServer.updateEntityWithOptionalForce(entity, false);
				player.playerNetServerHandler.setPlayerLocation(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
				player.theItemInWorldManager.setWorld(targetServer);
				player.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(player, targetServer);
				player.mcServer.getConfigurationManager().syncPlayerInventory(player);
				FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, currentDim, tpPoint.dimID);
				player.setPositionAndUpdate(tpPoint.x, tpPoint.y, tpPoint.z);
				player.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			}

			entity.setLocationAndAngles(tpPoint.x, tpPoint.y, tpPoint.z, (float) tpPoint.yaw, (float) tpPoint.pitch);
			if (mount != null)
			{
				if (entity instanceof EntityPlayerMP)
					targetServer.updateEntityWithOptionalForce(entity, true);

				System.out.println("Mount entity");
				entity.mountEntity(mount);
				targetServer.updateEntities();
				teleportEntity(entity, tpPoint);
			}
		}

		return entity;
	}

	private static void removeEntityFromWorld(World world, Entity entity)
	{
		if (entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer) entity;
			player.closeScreen();
			world.playerEntities.remove(player);
			world.updateAllPlayersSleepingFlag();
			int i = entity.chunkCoordX;
			int j = entity.chunkCoordZ;
			if (entity.addedToChunk && world.getChunkProvider().chunkExists(i, j))
			{
				world.getChunkFromChunkCoords(i, j).removeEntity(entity);
				world.getChunkFromChunkCoords(i, j).isModified = true;
			}

			world.loadedEntityList.remove(entity);
			world.onEntityRemoved(entity);
		}

		entity.isDead = false;
	}
}
