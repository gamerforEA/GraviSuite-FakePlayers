package gravisuite;

import buildcraft.api.tools.IToolWrench;
import com.eloraam.redpower.core.IRotatable;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.gravisuite.EventConfig;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gravisuite.audio.AudioManagerClient;
import gravisuite.audio.PositionSpec;
import gravisuite.keyboard.Keyboard;
import gravisuite.keyboard.KeyboardClient;
import gravisuite.redpower.coreLib;
import ic2.api.item.ElectricItem;
import ic2.api.item.IC2Items;
import ic2.api.item.IElectricItem;
import ic2.api.tile.IWrenchable;
import ic2.core.block.machine.tileentity.TileEntityTerra;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockLever;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.UseHoeEvent;

import java.lang.reflect.Method;
import java.util.*;

public class ItemGraviTool extends ItemTool implements IElectricItem, IToolWrench
{
	public static final IIcon[] iconsList = new IIcon[4];
	private final Set<Class<? extends Block>> shiftRotations = Sets.newHashSet(BlockLever.class, BlockButton.class, BlockChest.class);
	public static int hoeTextureIndex = 0;
	public static int treeTapTextureIndex = 1;
	public static int wrenchTextureIndex = 2;
	public static int screwDriverTextureIndex = 3;
	private int maxCharge = 300000;
	private int tier = 2;
	private int energyPerHoe = 50;
	private int energyPerTreeTap = 50;
	private int energyPerSwitchSide = 50;
	private int energyPerWrenchStandartOperation = 500;
	private int energyPerWrenchFineOperation = 10000;
	private int privateToolMode;
	private int transferLimit = 10000;

	protected ItemGraviTool(ToolMaterial toolMaterial)
	{
		super(0F, toolMaterial, new HashSet());
		this.setMaxDamage(27);
		this.efficiencyOnProperMaterial = 16F;
		this.setCreativeTab(GraviSuite.ic2Tab);
		this.addTexturesPath();
	}

	public void addTexturesPath()
	{
	}

	public boolean canDischarge(ItemStack stack, int amount)
	{
		return ElectricItem.manager.discharge(stack, amount, Integer.MAX_VALUE, true, false, true) == amount;
	}

	public void init()
	{
	}

	public void dischargeItem(ItemStack stack, EntityPlayer player, int amount)
	{
		ElectricItem.manager.use(stack, amount, player);
	}

	public static void setToolName(ItemStack stack)
	{
		Integer mode = readToolMode(stack);
		if (mode.intValue() == 1)
			stack.setStackDisplayName(Helpers.formatMessage("item.graviTool.name") + " (" + Helpers.formatMessage("graviTool.snap.Hoe") + ")");

		if (mode.intValue() == 2)
			stack.setStackDisplayName(Helpers.formatMessage("item.graviTool.name") + " (" + Helpers.formatMessage("graviTool.snap.TreeTap") + ")");

		if (mode.intValue() == 3)
			stack.setStackDisplayName(Helpers.formatMessage("item.graviTool.name") + " (" + Helpers.formatMessage("graviTool.snap.Wrench") + ")");

		if (mode.intValue() == 4)
			stack.setStackDisplayName(Helpers.formatMessage("item.graviTool.name") + " (" + Helpers.formatMessage("graviTool.snap.Screwdriver") + ")");
	}

	private boolean isShiftRotation(Class<? extends Block> clazz)
	{
		Iterator<Class<? extends Block>> iter = this.shiftRotations.iterator();

		Class<? extends Block> shift;
		do
		{
			if (!iter.hasNext())
				return false;

			shift = iter.next();
		}
		while (!shift.isAssignableFrom(clazz));

		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (GraviSuite.isSimulating() && Keyboard.isModeKeyDown(player))
		{
			int mode = readToolMode(stack);
			mode++;

			// TODO gamerforEA code start
			if (mode == 3 && EventConfig.disableGraviToolWrenchMode)
				mode++;
			// TODO gamerforEA code end

			if (mode > 4)
				mode = 1;

			saveToolMode(stack, mode);
			setToolName(stack);
			if (mode == 1)
				ServerProxy.sendPlayerMessage(player, "§2" + Helpers.formatMessage("graviTool.snap.Hoe") + " " + "§a" + Helpers.formatMessage("message.text.activated"));
			else if (mode == 2)
				ServerProxy.sendPlayerMessage(player, "§6" + Helpers.formatMessage("graviTool.snap.TreeTap") + " " + "§a" + Helpers.formatMessage("message.text.activated"));
			else if (mode == 3)
				ServerProxy.sendPlayerMessage(player, "§b" + Helpers.formatMessage("graviTool.snap.Wrench") + " " + "§a" + Helpers.formatMessage("message.text.activated"));
			else if (mode == 4)
				ServerProxy.sendPlayerMessage(player, "§d" + Helpers.formatMessage("graviTool.snap.Screwdriver") + " " + "§a" + Helpers.formatMessage("message.text.activated"));
		}

		if (!GraviSuite.isSimulating() && KeyboardClient.isModeKeyPress(player))
			AudioManagerClient.playSound(player, PositionSpec.Hand, "toolChange");

		return stack;
	}

	@Override
	public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		setToolName(stack);
		Integer mode = readToolMode(stack);
		if (mode.intValue() == 3)
			return this.onWrenchUse(stack, player, world, x, y, z, side, a, b, c);
		else if (mode.intValue() == 4)
			return this.onScrewdriverUse(stack, player, world, x, y, z, side, a, b, c);
		else
			return false;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		Integer mode = readToolMode(stack);
		if (mode.intValue() == 1)
			return this.onHoeUse(stack, player, world, x, y, z, side, a, b, c);
		else if (mode.intValue() == 2)
			return this.onTreeTapUse(stack, player, world, x, y, z, side, a, b, c);
		else
			return false;
	}

	public boolean onHoeUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		if (!player.canPlayerEdit(x, y, z, side, stack))
			return false;
		else if (!this.canDischarge(stack, this.energyPerHoe))
		{
			ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));
			return false;
		}
		else
		{
			UseHoeEvent event = new UseHoeEvent(player, stack, world, x, y, z);
			if (MinecraftForge.EVENT_BUS.post(event))
				return false;
			else if (event.getResult() == Result.ALLOW)
			{
				this.dischargeItem(stack, player, this.energyPerHoe);
				return true;
			}
			else
			{
				Block block = world.getBlock(x, y, z);
				if (side != 0 && world.getBlock(x, y + 1, z).isAir(world, x, y + 1, z) && (block == Blocks.grass || block == Blocks.dirt))
				{
					Block farmland = Blocks.farmland;
					world.playSoundEffect(x + 0.5F, y + 0.5F, z + 0.5F, farmland.stepSound.getStepResourcePath(), (farmland.stepSound.getVolume() + 1F) / 2F, farmland.stepSound.getPitch() * 0.8F);
					if (!GraviSuite.isSimulating())
						return true;
					else
					{
						this.dischargeItem(stack, player, this.energyPerHoe);
						world.setBlock(x, y, z, farmland);
						return true;
					}
				}
				else
					return false;
			}
		}
	}

	public boolean onTreeTapUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		Block block = world.getBlock(x, y, z);
		if (Helpers.equals(block, IC2Items.getItem("blockBarrel")))
			try
			{
				Method method = world.getTileEntity(x, y, z).getClass().getMethod("useTreetapOn", EntityPlayer.class, Integer.TYPE);
				return (Boolean) method.invoke(null, player, side);
			}
			catch (Throwable throwable)
			{
			}

		if (Helpers.equals(block, IC2Items.getItem("rubberWood")))
		{
			this.attemptExtract(stack, player, world, x, y, z, side, null);
			return true;
		}
		else
			return false;
	}

	public boolean onWrenchUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
	{
		if (!this.canDischarge(stack, this.energyPerSwitchSide))
			return false;
		else
		{
			// TODO gamerforEA code start
			if (EventUtils.cantBreak(player, x, y, z))
				return false;
			// TODO gamerforEA code end

			Block block = world.getBlock(x, y, z);
			int meta = world.getBlockMetadata(x, y, z);
			TileEntity tile = world.getTileEntity(x, y, z);

			if (tile instanceof IWrenchable)
			{
				if (tile instanceof TileEntityTerra)
					if (((TileEntityTerra) tile).ejectBlueprint())
					{
						if (GraviSuite.isSimulating())
							this.dischargeItem(stack, player, this.energyPerSwitchSide);

						if (!GraviSuite.isSimulating())
							AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

						return GraviSuite.isSimulating();
					}

				IWrenchable wrenchable = (IWrenchable) tile;
				if (Keyboard.isAltKeyDown(player))
				{
					if (player.isSneaking())
						side = (wrenchable.getFacing() + 5) % 6;
					else
						side = (wrenchable.getFacing() + 1) % 6;
				}
				else if (player.isSneaking())
					side += side % 2 * -2 + 1;

				if (wrenchable.wrenchCanSetFacing(player, side))
				{
					if (GraviSuite.isSimulating())
					{
						wrenchable.setFacing((short) side);
						this.dischargeItem(stack, player, this.energyPerSwitchSide);
					}

					if (!GraviSuite.isSimulating())
						AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

					return GraviSuite.isSimulating();
				}

				if (this.canDischarge(stack, this.energyPerWrenchStandartOperation) && wrenchable.wrenchCanRemove(player))
				{
					if (GraviSuite.isSimulating())
					{
						boolean dropOriginalBlock = false;
						if (wrenchable.getWrenchDropRate() < 1F && this.overrideWrenchSuccessRate(stack))
						{
							if (!this.canDischarge(stack, this.energyPerWrenchFineOperation))
							{
								ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));
								return true;
							}

							dropOriginalBlock = true;
							this.dischargeItem(stack, player, this.energyPerWrenchFineOperation);
						}
						else
						{
							dropOriginalBlock = world.rand.nextFloat() <= wrenchable.getWrenchDropRate();
							this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);
						}

						ArrayList<ItemStack> drops = block.getDrops(world, x, y, z, meta, 0);

						if (dropOriginalBlock)
							if (drops.isEmpty())
								drops.add(wrenchable.getWrenchDrop(player));
							else
								drops.set(0, wrenchable.getWrenchDrop(player));

						for (ItemStack itemStack : drops)
						{
							dropAsEntity(world, x, y, z, itemStack);
						}

						world.setBlockToAir(x, y, z);
					}

					if (!GraviSuite.isSimulating())
						AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");
				}
			}

			if (player.isSneaking() != this.isShiftRotation(block.getClass()))
				return false;
			else
			{
				if (this.canDischarge(stack, this.energyPerWrenchStandartOperation))
				{
					if (block.rotateBlock(world, x, y, z, ForgeDirection.getOrientation(side)))
					{
						if (GraviSuite.isSimulating())
						{
							player.swingItem();
							this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);
						}
						else
							AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

						return true;
					}
				}
				else
					ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

				return false;
			}
		}
	}

	public boolean overrideWrenchSuccessRate(ItemStack var1)
	{
		return true;
	}

	public static void dropAsEntity(World world, int x, int y, int z, ItemStack stack)
	{
		if (stack != null)
		{
			double xOffset = world.rand.nextFloat() * 0.7D + (1D - 0.7D) * 0.5D;
			double yOffset = world.rand.nextFloat() * 0.7D + (1D - 0.7D) * 0.5D;
			double zOffset = world.rand.nextFloat() * 0.7D + (1D - 0.7D) * 0.5D;
			EntityItem entity = new EntityItem(world, x + xOffset, y + yOffset, z + zOffset, stack.copy());
			entity.delayBeforeCanPickup = 10;
			world.spawnEntityInWorld(entity);
		}
	}

	public void ejectHarz(World world, int x, int y, int z, int side, int quantity)
	{
		double ejectX = x + 0.5D;
		double ejectY = y + 0.5D;
		double ejectZ = z + 0.5D;
		if (side == 2)
			ejectZ -= 0.3D;
		else if (side == 5)
			ejectX += 0.3D;
		else if (side == 3)
			ejectZ += 0.3D;
		else if (side == 4)
			ejectX -= 0.3D;

		for (int i = 0; i < quantity; ++i)
		{
			EntityItem entityitem = new EntityItem(world, ejectX, ejectY, ejectZ, IC2Items.getItem("resin").copy());
			entityitem.delayBeforeCanPickup = 10;
			world.spawnEntityInWorld(entityitem);
		}
	}

	public boolean attemptExtract(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, List<ItemStack> stacks)
	{
		int meta = world.getBlockMetadata(x, y, z);
		if (meta >= 2 && meta % 6 == side)
		{
			if (meta < 6)
			{
				if (!this.canDischarge(stack, this.energyPerTreeTap))
				{
					ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));
					return false;
				}
				else
				{
					if (GraviSuite.isSimulating())
					{
						world.setBlockMetadataWithNotify(x, y, z, meta + 6, 3);
						if (stacks != null)
							stacks.add(copyWithSize(IC2Items.getItem("resin"), world.rand.nextInt(3) + 1));
						else
							this.ejectHarz(world, x, y, z, side, world.rand.nextInt(3) + 1);

						Block woodBlock = getBlock(IC2Items.getItem("rubberWood"));
						world.scheduleBlockUpdate(x, y, z, woodBlock, woodBlock.tickRate(world));
						this.dischargeItem(stack, player, this.energyPerTreeTap);
					}

					if (!GraviSuite.isSimulating())
						AudioManagerClient.playSound(player, PositionSpec.Hand, "Treetap");

					return true;
				}
			}
			else
			{
				if (world.rand.nextInt(5) == 0 && GraviSuite.isSimulating())
					world.setBlockMetadataWithNotify(x, y, z, 1, 3);

				if (world.rand.nextInt(5) == 0)
				{
					if (!this.canDischarge(stack, this.energyPerTreeTap))
					{
						ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));
						return false;
					}
					else
					{
						if (GraviSuite.isSimulating())
						{
							this.ejectHarz(world, x, y, z, side, 1);
							if (stacks != null)
								stacks.add(copyWithSize(IC2Items.getItem("resin"), 1));
							else
								this.ejectHarz(world, x, y, z, side, 1);

							this.dischargeItem(stack, player, this.energyPerTreeTap);
						}

						return true;
					}
				}
				else
					return false;
			}
		}
		else
			return false;
	}

	public boolean onScrewdriverUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		boolean isSneaking = false;
		if (player != null && player.isSneaking())
			isSneaking = true;

		Block block = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		if (block != Blocks.unpowered_repeater && block != Blocks.powered_repeater)
		{
			if (block == Blocks.dispenser)
			{
				if (!this.canDischarge(stack, this.energyPerWrenchStandartOperation))
				{
					if (GraviSuite.isSimulating())
						ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

					return false;
				}
				else
				{
					meta = meta & 3 ^ meta >> 2;
					meta += 2;
					if (!GraviSuite.isSimulating())
						;

					if (GraviSuite.isSimulating())
						this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);

					return GraviSuite.isSimulating();
				}
			}
			else if (block != Blocks.piston && block != Blocks.sticky_piston)
			{
				TileEntity tile = world.getTileEntity(x, y, z);
				if (tile instanceof IRotatable)
				{
					if (!this.canDischarge(stack, this.energyPerWrenchStandartOperation))
					{
						if (GraviSuite.isSimulating())
							ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

						return false;
					}
					else
					{
						MovingObjectPosition mop = coreLib.retraceBlock(world, player, x, y, z);
						if (mop == null)
							return false;
						else
						{
							int maxRotation = ((IRotatable) tile).getPartMaxRotation(mop.subHit, isSneaking);
							if (maxRotation == 0)
								return false;
							else
							{
								int rotation = ((IRotatable) tile).getPartRotation(mop.subHit, isSneaking) + 1;
								if (rotation > maxRotation)
									rotation = 0;

								if (!GraviSuite.isSimulating())
									AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

								if (GraviSuite.isSimulating())
								{
									this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);
									((IRotatable) tile).setPartRotation(mop.subHit, isSneaking, rotation);
								}

								return GraviSuite.isSimulating();
							}
						}
					}
				}
				else
					return false;
			}
			else
			{
				++meta;
				if (!this.canDischarge(stack, this.energyPerWrenchStandartOperation))
				{
					if (GraviSuite.isSimulating())
						ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

					return false;
				}
				else
				{
					if (meta > 5)
						meta = 0;

					if (!GraviSuite.isSimulating())
						AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

					if (GraviSuite.isSimulating())
					{
						this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);
						world.setBlockMetadataWithNotify(x, y, z, meta, 7);
					}

					return GraviSuite.isSimulating();
				}
			}
		}
		else if (!this.canDischarge(stack, this.energyPerWrenchStandartOperation))
		{
			if (GraviSuite.isSimulating())
				ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

			return false;
		}
		else
		{
			if (!GraviSuite.isSimulating())
				AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

			if (GraviSuite.isSimulating())
			{
				this.dischargeItem(stack, player, this.energyPerWrenchStandartOperation);
				world.setBlockMetadataWithNotify(x, y, z, meta & 12 | meta + 1 & 3, 7);
			}

			return GraviSuite.isSimulating();
		}
	}

	public static Block getBlock(ItemStack stack)
	{
		Item item = stack.getItem();
		return item instanceof ItemBlock ? ((ItemBlock) item).field_150939_a : null;
	}

	public static ItemStack copyWithSize(ItemStack stack, int size)
	{
		ItemStack ret = stack.copy();
		ret.stackSize = size;
		return ret;
	}

	@Override
	public boolean canProvideEnergy(ItemStack stack)
	{
		return false;
	}

	@Override
	public double getMaxCharge(ItemStack stack)
	{
		return this.maxCharge;
	}

	@Override
	public int getTier(ItemStack stack)
	{
		return this.tier;
	}

	@Override
	public double getTransferLimit(ItemStack stack)
	{
		return this.transferLimit;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker)
	{
		return false;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity)
	{
		return true;
	}

	public void damage(ItemStack stack, int amount, EntityPlayer player)
	{
		ElectricItem.manager.use(stack, amount, player);
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
	public EnumRarity getRarity(ItemStack stack)
	{
		return EnumRarity.uncommon;
	}

	public static Integer readToolMode(ItemStack itemstack)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		int mode = nbttagcompound.getInteger("toolMode");

		// TODO gamerforEA code start
		if (mode == 3 && EventConfig.disableGraviToolWrenchMode)
			mode++;
		// TODO gamerforEA code end

		if (mode <= 0 || mode > 4)
			mode = 1;

		return mode;
	}

	public static Integer readTextureIndex(ItemStack itemstack)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		Integer textureIndex = nbttagcompound.getInteger("textureIndex");
		if (textureIndex.intValue() <= 0)
			textureIndex = hoeTextureIndex;

		return textureIndex;
	}

	public static boolean saveToolMode(ItemStack itemstack, Integer toolMode)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		nbttagcompound.setInteger("toolMode", toolMode.intValue());
		if (toolMode.intValue() == 1)
			nbttagcompound.setInteger("textureIndex", hoeTextureIndex);

		if (toolMode.intValue() == 2)
			nbttagcompound.setInteger("textureIndex", treeTapTextureIndex);

		if (toolMode.intValue() == 3)
			nbttagcompound.setInteger("textureIndex", wrenchTextureIndex);

		if (toolMode.intValue() == 4)
			nbttagcompound.setInteger("textureIndex", screwDriverTextureIndex);

		return true;
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

	@Override
	public boolean canWrench(EntityPlayer player, int x, int y, int z)
	{
		ItemStack itemstack = player.inventory.getCurrentItem();
		Integer toolMode = readToolMode(itemstack);
		if (toolMode.intValue() == 3)
		{
			if (this.canDischarge(itemstack, this.energyPerWrenchStandartOperation))
				return true;
			else
			{
				if (GraviSuite.isSimulating())
					ServerProxy.sendPlayerMessage(player, Helpers.formatMessage("message.text.noenergy"));

				return false;
			}
		}
		else
			return false;
	}

	@Override
	public void wrenchUsed(EntityPlayer player, int x, int y, int z)
	{
		if (GraviSuite.isSimulating())
		{
			ItemStack itemstack = player.inventory.getCurrentItem();
			Integer toolMode = readToolMode(itemstack);
			this.dischargeItem(itemstack, player, this.energyPerWrenchStandartOperation);
		}
		else
			AudioManagerClient.playSound(player, PositionSpec.Hand, "wrench");

	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister)
	{
		iconsList[0] = iconRegister.registerIcon("gravisuite:itemGraviToolHoe");
		iconsList[1] = iconRegister.registerIcon("gravisuite:itemGraviToolTreeTap");
		iconsList[2] = iconRegister.registerIcon("gravisuite:itemGraviToolWrench");
		iconsList[3] = iconRegister.registerIcon("gravisuite:itemGraviToolScrewdriver");
		this.itemIcon = iconsList[0];
	}

	@Override
	public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player)
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean requiresMultipleRenderPasses()
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(ItemStack itemStack, int pass)
	{
		Integer myIndex = readTextureIndex(itemStack);
		return iconsList[myIndex.intValue()];
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
}
