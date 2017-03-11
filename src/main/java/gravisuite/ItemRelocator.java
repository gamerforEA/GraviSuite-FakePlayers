package gravisuite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.FastUtils;
import com.gamerforea.gravisuite.EventConfig;
import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gravisuite.keyboard.Keyboard;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class ItemRelocator extends ItemTool implements IElectricItem
{
	private double maxCharge = 1.0E7D;
	private int tier = 3;
	private int transferLimit = '썐';
	private int energyPerStandartTp = GraviSuite.relocatorEnergyPerStandartTp;
	private int energyPerDimesionTp = GraviSuite.relocatorEnergyPerDimesionTp;
	private int energyPerPortal = GraviSuite.relocatorEnergyPerPortal;
	private int energyPerShoot = GraviSuite.relocatorEnergyPerTranslocator;
	public static final int maxPoints = 10;
	public static final String nbt_tpList_name = "nbt_tpList_name";
	public static final String nbt_tpList_dimID = "nbt_tpList_dimID";
	public static final String nbt_tpList_pointName = "nbt_tpList_pointName";
	public static final String nbt_tpList_xPos = "nbt_tpList_xPos";
	public static final String nbt_tpList_yPos = "nbt_tpList_yPos";
	public static final String nbt_tpList_zPos = "nbt_tpList_zPos";
	public static final String nbt_tpList_yaw = "nbt_tpList_yaw";
	public static final String nbt_tpList_pitch = "nbt_tpList_pitch";
	public static final String nbt_tpList_defPoint = "nbt_tpList_defPoint";

	protected ItemRelocator(ToolMaterial toolMaterial)
	{
		super(0.0F, toolMaterial, new HashSet());
		this.setMaxDamage(27);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player)
	{
		if (Keyboard.isModeKeyDown(player))
		{
			Integer toolMode = readToolMode(itemStack);
			toolMode = Integer.valueOf(toolMode.intValue() + 1);
			if (toolMode.intValue() > 2)
				toolMode = Integer.valueOf(0);

			this.saveToolMode(itemStack, toolMode);
			if (toolMode.intValue() == 0)
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GREEN + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.relocator.mode.personal"));

			if (toolMode.intValue() == 1)
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.relocator.mode.translocator"));

			if (toolMode.intValue() == 2)
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.AQUA + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.relocator.mode.portal"));

			return itemStack;
		}
		else
		{
			Integer toolMode = readToolMode(itemStack);
			if (player.isSneaking())
			{
				if (toolMode.intValue() != 1 && toolMode.intValue() != 2)
					player.openGui(GraviSuite.instance, 1, world, (int) player.posX, (int) player.posY, (int) player.posZ);
				else
					player.openGui(GraviSuite.instance, 3, world, (int) player.posX, (int) player.posY, (int) player.posZ);
			}
			else if (toolMode.intValue() == 0)
				player.openGui(GraviSuite.instance, 2, world, (int) player.posX, (int) player.posY, (int) player.posZ);
			else if (toolMode.intValue() == 1 || toolMode.intValue() == 2)
			{
				ItemRelocator.TeleportPoint tpPoint = getDefaultPoint(itemStack);
				if (tpPoint != null)
				{
					int energyPerOperation = 0;
					byte launchType = 0;
					if (toolMode.intValue() == 1)
					{
						energyPerOperation = this.energyPerShoot;
						launchType = 0;
						if (GraviSuite.disableRelocatorTranslocator)
						{
							ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.modeTranslocatorDisabled"));
							return itemStack;
						}
					}
					else
					{
						energyPerOperation = this.energyPerPortal;
						launchType = 1;
						if (GraviSuite.disableRelocatorPortal)
						{
							ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.modePortalDisabled"));
							return itemStack;
						}
					}

					if (!ElectricItem.manager.canUse(itemStack, energyPerOperation) && !player.capabilities.isCreativeMode)
						ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.text.noenergy"));
					else
					{
						if (GraviSuite.isSimulating() && !player.capabilities.isCreativeMode)
							ElectricItem.manager.use(itemStack, energyPerOperation, player);

						EntityPlasmaBall plasmaBall = new EntityPlasmaBall(world, player, tpPoint, launchType);
						if (GraviSuite.isSimulating())
							world.spawnEntityInWorld(plasmaBall);

						player.swingItem();
					}
				}
				else
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.noDefaultPoint"));
			}

			return itemStack;
		}
	}

	public static List<ItemRelocator.TeleportPoint> loadTeleportPoints(ItemStack itemStack)
	{
		if (itemStack == null)
			return null;
		else
		{
			if (itemStack.stackTagCompound == null)
				itemStack.stackTagCompound = new NBTTagCompound();

			NBTTagList list = itemStack.stackTagCompound.getTagList("nbt_tpList_name", 10);
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();

			for (int i = 0; i < list.tagCount(); ++i)
			{
				ItemRelocator.TeleportPoint newPoint = new ItemRelocator.TeleportPoint();
				NBTTagCompound loadedPoint = list.getCompoundTagAt(i);
				newPoint.pointName = loadedPoint.getString("nbt_tpList_pointName");
				newPoint.dimID = loadedPoint.getInteger("nbt_tpList_dimID");
				newPoint.x = loadedPoint.getDouble("nbt_tpList_xPos");
				newPoint.y = loadedPoint.getDouble("nbt_tpList_yPos");
				newPoint.z = loadedPoint.getDouble("nbt_tpList_zPos");
				newPoint.yaw = loadedPoint.getDouble("nbt_tpList_yaw");
				newPoint.pitch = loadedPoint.getDouble("nbt_tpList_pitch");
				newPoint.defPoint = loadedPoint.getBoolean("nbt_tpList_defPoint");
				tpList.add(newPoint);
			}

			return tpList;
		}
	}

	public static void saveTeleportPoints(ItemStack itemStack, List<ItemRelocator.TeleportPoint> tpList)
	{
		if (itemStack.stackTagCompound == null)
			itemStack.stackTagCompound = new NBTTagCompound();

		NBTTagList nbtList = new NBTTagList();

		for (ItemRelocator.TeleportPoint point : tpList)
		{
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setString("nbt_tpList_pointName", point.pointName);
			nbt.setInteger("nbt_tpList_dimID", point.dimID);
			nbt.setDouble("nbt_tpList_xPos", point.x);
			nbt.setDouble("nbt_tpList_yPos", point.y);
			nbt.setDouble("nbt_tpList_zPos", point.z);
			nbt.setDouble("nbt_tpList_yaw", point.yaw);
			nbt.setDouble("nbt_tpList_pitch", point.pitch);
			nbt.setBoolean("nbt_tpList_defPoint", point.defPoint);
			nbtList.appendTag(nbt);
		}

		itemStack.getTagCompound().setTag("nbt_tpList_name", nbtList);
	}

	public static void addNewTeleportPoint(EntityPlayer player, ItemStack itemStack, ItemRelocator.TeleportPoint newPoint)
	{
		if (itemStack != null && newPoint != null)
		{
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();
			tpList.addAll(loadTeleportPoints(itemStack));
			if (tpList.size() >= 10)
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.memoryFull"));
			else
			{
				Boolean pointExists = Boolean.valueOf(false);

				for (ItemRelocator.TeleportPoint point : tpList)
					if (point.pointName.equalsIgnoreCase(newPoint.pointName))
					{
						ServerProxy.sendPlayerMessage(player, EnumChatFormatting.YELLOW + newPoint.pointName + " " + EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.pointExists"));
						pointExists = Boolean.valueOf(true);
						return;
					}

				if (!pointExists.booleanValue())
				{
					// TODO gamerforEA code start
					if (EventConfig.relocatorToEvent)
					{
						boolean changeDim = player.worldObj.provider.dimensionId != newPoint.dimID;
						World world = changeDim ? MinecraftServer.getServer().worldServerForDimension(newPoint.dimID) : player.worldObj;
						if (world != null)
						{
							EntityPlayer fakePlayer = FastUtils.getFake(world, player.getGameProfile());
							int x = (int) newPoint.x;
							int y = (int) newPoint.y;
							int z = (int) newPoint.z;
							if (EventUtils.cantBreak(fakePlayer, x, y, z))
								return;
						}
					}
					// TODO gamerforEA code end

					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.YELLOW + newPoint.pointName + " " + EnumChatFormatting.GREEN + Helpers.formatMessage("message.relocator.text.poindAdded"));
					tpList.add(newPoint);
					saveTeleportPoints(itemStack, tpList);
				}

			}
		}
	}

	public static ItemRelocator.TeleportPoint getDefaultPoint(ItemStack itemStack)
	{
		if (itemStack == null)
			return null;
		else
		{
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();
			tpList.addAll(loadTeleportPoints(itemStack));

			for (ItemRelocator.TeleportPoint point : tpList)
				if (point.defPoint)
					return point;

			return null;
		}
	}

	public static Boolean setDefaultPoint(EntityPlayer player, ItemStack itemStack, String ptName)
	{
		if (itemStack == null)
			return Boolean.valueOf(false);
		else
		{
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();
			tpList.addAll(loadTeleportPoints(itemStack));
			Boolean pointExists = Boolean.valueOf(false);

			for (ItemRelocator.TeleportPoint point : tpList)
				if (point.pointName.equalsIgnoreCase(ptName))
				{
					point.defPoint = true;
					pointExists = Boolean.valueOf(true);
				}
				else
					point.defPoint = false;

			if (pointExists.booleanValue())
			{
				saveTeleportPoints(itemStack, tpList);
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.WHITE + Helpers.formatMessage("message.relocator.text.defaultPointSet") + " " + EnumChatFormatting.YELLOW + ptName);
			}
			else
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.relocator.text.noPoint"));

			return null;
		}
	}

	public static ItemRelocator.TeleportPoint getTeleportPointByName(ItemStack itemStack, String ptName)
	{
		if (itemStack != null && ptName != "")
		{
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();
			tpList.addAll(loadTeleportPoints(itemStack));

			for (ItemRelocator.TeleportPoint point : tpList)
				if (point.pointName.equalsIgnoreCase(ptName))
					return point;

			return null;
		}
		else
			return null;
	}

	public static void removeTeleportPoint(ItemStack itemStack, String tpName)
	{
		if (itemStack != null)
		{
			List<ItemRelocator.TeleportPoint> tpList = Lists.newArrayList();
			tpList.addAll(loadTeleportPoints(itemStack));

			for (int i = 0; i < ((List) tpList).size(); ++i)
			{
				ItemRelocator.TeleportPoint tmpPoint = tpList.get(i);
				if (tmpPoint.pointName.equalsIgnoreCase(tpName))
				{
					tpList.remove(i);
					saveTeleportPoints(itemStack, tpList);
					return;
				}
			}

		}
	}

	public void teleportPlayer(EntityPlayer player, ItemStack itemStack, String tpName)
	{
		if (itemStack != null && player != null)
		{
			ItemRelocator.TeleportPoint point = getTeleportPointByName(itemStack, tpName);
			if (point != null)
			{
				int currentDim = player.worldObj.provider.dimensionId;
				boolean changeDim = false;
				int energyPerOperation;
				if (currentDim == point.dimID)
					energyPerOperation = this.energyPerStandartTp;
				else
				{
					energyPerOperation = this.energyPerDimesionTp;
					changeDim = true;
				}

				if (!ElectricItem.manager.canUse(itemStack, energyPerOperation) && !player.capabilities.isCreativeMode)
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.text.noenergy"));
				else
				{
					if (GraviSuite.isSimulating() && !player.capabilities.isCreativeMode)
						ElectricItem.manager.use(itemStack, energyPerOperation, player);

					// TODO gamerforEA code start
					if (EventConfig.relocatorFromEvent)
					{
						int x1 = MathHelper.floor_double(player.posX);
						int y1 = MathHelper.floor_double(player.posY);
						int z1 = MathHelper.floor_double(player.posZ);
						if (EventUtils.cantBreak(player, x1, y1, z1))
							return;
					}

					if (EventConfig.relocatorToEvent)
					{
						World world = changeDim ? MinecraftServer.getServer().worldServerForDimension(point.dimID) : player.worldObj;
						if (world != null)
						{
							EntityPlayer fakePlayer = FastUtils.getFake(world, player.getGameProfile());
							int x = MathHelper.floor_double(point.x);
							int y = MathHelper.floor_double(point.y);
							int z = MathHelper.floor_double(point.z);
							if (EventUtils.cantBreak(fakePlayer, x, y, z))
								return;
						}
					}
					// TODO gamerforEA code end

					Helpers.teleportEntity(player, point);
				}

			}
		}
	}

	private static List getCollidingWorldGeometry(World world, AxisAlignedBB axisalignedbb, Entity entity)
	{
		ArrayList collidingBoundingBoxes = new ArrayList();
		int i = MathHelper.floor_double(axisalignedbb.minX);
		int j = MathHelper.floor_double(axisalignedbb.maxX + 1.0D);
		int k = MathHelper.floor_double(axisalignedbb.minY);
		int l = MathHelper.floor_double(axisalignedbb.maxY + 1.0D);
		int i1 = MathHelper.floor_double(axisalignedbb.minZ);
		int j1 = MathHelper.floor_double(axisalignedbb.maxZ + 1.0D);

		for (int k1 = i; k1 < j; ++k1)
			for (int l1 = i1; l1 < j1; ++l1)
				for (int i2 = k - 1; i2 < l; ++i2)
				{
					Block block = world.getBlock(k1, i2, l1);
					if (block != null)
						block.addCollisionBoxesToList(world, k1, i2, l1, axisalignedbb, collidingBoundingBoxes, entity);
				}

		return collidingBoundingBoxes;
	}

	public static Integer readToolMode(ItemStack itemstack)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		Integer toolMode = Integer.valueOf(nbttagcompound.getInteger("toolMode"));
		if (toolMode.intValue() < 0 || toolMode.intValue() > 3)
			toolMode = Integer.valueOf(0);

		return toolMode;
	}

	public void saveToolMode(ItemStack itemstack, Integer toolMode)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		nbttagcompound.setInteger("toolMode", toolMode.intValue());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4)
	{
		super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
		Integer toolMode = readToolMode(par1ItemStack);
		if (toolMode.intValue() == 0)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + EnumChatFormatting.WHITE + Helpers.formatMessage("message.relocator.mode.personal"));

		if (toolMode.intValue() == 1)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + EnumChatFormatting.WHITE + Helpers.formatMessage("message.relocator.mode.translocator"));

		if (toolMode.intValue() == 2)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + EnumChatFormatting.WHITE + Helpers.formatMessage("message.relocator.mode.portal"));

		ItemRelocator.TeleportPoint tpPoint = getDefaultPoint(par1ItemStack);
		if (tpPoint != null)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.relocator.defPoint") + ": " + EnumChatFormatting.WHITE + tpPoint.pointName);
		else
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.relocator.defPoint") + ": -");

	}

	@Override
	public boolean canProvideEnergy(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public Item getChargedItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public Item getEmptyItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public double getMaxCharge(ItemStack itemStack)
	{
		return this.maxCharge;
	}

	@Override
	public int getTier(ItemStack itemStack)
	{
		return this.tier;
	}

	@Override
	public double getTransferLimit(ItemStack itemStack)
	{
		return this.transferLimit;
	}

	@Override
	public boolean isRepairable()
	{
		return false;
	}

	@Override
	public int getItemEnchantability()
	{
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister)
	{
		super.itemIcon = par1IconRegister.registerIcon("gravisuite:itemRelocator");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack var1)
	{
		return EnumRarity.epic;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs var2, List var3)
	{
		ItemStack var4 = new ItemStack(this, 1);
		ElectricItem.manager.charge(var4, 2.147483647E9D, Integer.MAX_VALUE, true, false);
		var3.add(var4);
		var3.add(new ItemStack(this, 1, this.getMaxDamage()));
	}

	public static class TeleportPoint
	{
		public int dimID;
		public String pointName;
		public double x;
		public double y;
		public double z;
		public double yaw;
		public double pitch;
		public boolean defPoint;
	}
}
