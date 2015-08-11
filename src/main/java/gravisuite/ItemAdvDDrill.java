package gravisuite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gamerforea.gravisuite.EventConfig;
import com.gamerforea.gravisuite.FakePlayerUtils;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gravisuite.keyboard.Keyboard;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

public class ItemAdvDDrill extends ItemTool implements IElectricItem
{
	public static final Set mineableBlocks = Sets.newHashSet(Blocks.grass, Blocks.dirt, Blocks.mycelium, Blocks.sand, Blocks.gravel, Blocks.snow, Blocks.snow_layer, Blocks.clay, Blocks.soul_sand);
	private static final Set<Material> materials = Sets.newHashSet(Material.rock, Material.grass, Material.ground, Material.sand, Material.clay);
	private static final Set<String> toolType = ImmutableSet.of("pickaxe", "shovel");
	private float effPower = 35F;
	private float bigHolePower = 16F;
	private float normalPower = 35F;
	private float lowPower = 16F;
	private float ultraLowPower = 10F;
	private int maxCharge = 45000;
	private int tier = 2;
	private int maxWorkRange = 1;
	private int energyPerOperation = 160;
	private int energyPerLowOperation = 80;
	private int energyPerUltraLowOperation = 50;
	private int transferLimit = 500;
	public int soundTicker;
	public int damageVsEntity = 1;

	protected ItemAdvDDrill(ToolMaterial toolMaterial)
	{
		super(0F, toolMaterial, new HashSet());
		this.setMaxDamage(27);
		super.efficiencyOnProperMaterial = this.normalPower;
		this.setCreativeTab(GraviSuite.ic2Tab);
	}

	public void init()
	{
	}

	@Override
	public boolean canProvideEnergy(ItemStack itemStack)
	{
		return false;
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
	public Set<String> getToolClasses(ItemStack stack)
	{
		return toolType;
	}

	@Override
	public boolean canHarvestBlock(Block block, ItemStack stack)
	{
		return Items.diamond_pickaxe.canHarvestBlock(block, stack) || Items.diamond_pickaxe.func_150893_a(stack, block) > 1F || Items.diamond_shovel.canHarvestBlock(block, stack) || Items.diamond_shovel.func_150893_a(stack, block) > 1F || ItemAdvDDrill.mineableBlocks.contains(block);
	}

	@Override
	public float getDigSpeed(ItemStack tool, Block block, int meta)
	{
		return !ElectricItem.manager.canUse(tool, this.energyPerOperation) ? 1F : this.canHarvestBlock(block, tool) ? super.efficiencyOnProperMaterial : 1F;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolType)
	{
		return !toolType.equals("pickaxe") && !toolType.equals("shovel") ? super.getHarvestLevel(stack, toolType) : super.toolMaterial.getHarvestLevel();
	}

	public boolean hitEntity(ItemStack stack, EntityLiving entity1, EntityLiving entity2)
	{
		return true;
	}

	public int getDamageVsEntity(Entity entity)
	{
		return this.damageVsEntity;
	}

	@Override
	public boolean isRepairable()
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register)
	{
		super.itemIcon = register.registerIcon("gravisuite:itemAdvancedDDrill");
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player)
	{
		if (readToolMode(stack) != 3)
			return false;
		else
		{
			World world = player.worldObj;
			Block block = world.getBlock(x, y, z);
			int meta = world.getBlockMetadata(x, y, z);
			if (block == null)
				return super.onBlockStartBreak(stack, x, y, z, player);
			else
			{
				MovingObjectPosition mop = raytraceFromEntity(world, player, true, 4.5D);
				if (mop != null && (materials.contains(block.getMaterial()) || block == Blocks.monster_egg))
				{
					byte xRange = 1;
					byte yRange = 1;
					byte zRange = 1;
					switch (mop.sideHit)
					{
						case 0:
						case 1:
							yRange = 0;
							break;
						case 2:
						case 3:
							zRange = 0;
							break;
						case 4:
						case 5:
							xRange = 0;
					}

					boolean lowPower = false;
					boolean silktouch = EnchantmentHelper.getSilkTouchModifier(player);
					int fortune = EnchantmentHelper.getFortuneModifier(player);

					for (int xPos = x - xRange; xPos <= x + xRange; ++xPos)
						for (int yPos = y - yRange; yPos <= y + yRange; ++yPos)
							for (int zPos = z - zRange; zPos <= z + zRange; ++zPos)
								if (ElectricItem.manager.canUse(stack, this.energyPerOperation))
								{
									Block localBlock = world.getBlock(xPos, yPos, zPos);
									if (localBlock != null && this.canHarvestBlock(localBlock, stack))
									{
										// TODO gamerforEA code start
										if (EventConfig.advDDrillEvent && FakePlayerUtils.cantBreak(player, xPos, yPos, zPos))
											continue;
										// TODO gamerforEA code end
										if (localBlock.getBlockHardness(world, xPos, yPos, zPos) >= 0F)
											if (materials.contains(localBlock.getMaterial()) || block == Blocks.monster_egg)
											{
												if (!player.capabilities.isCreativeMode)
												{
													int localMeta = world.getBlockMetadata(xPos, yPos, zPos);

													if (localBlock.removedByPlayer(world, player, xPos, yPos, zPos))
														localBlock.onBlockDestroyedByPlayer(world, xPos, yPos, zPos, localMeta);

													if (!silktouch)
														localBlock.dropXpOnBlockBreak(world, xPos, yPos, zPos, localBlock.getExpDrop(world, localMeta, fortune));

													localBlock.harvestBlock(world, player, xPos, yPos, zPos, localMeta);
													localBlock.onBlockHarvested(world, xPos, yPos, zPos, localMeta, player);
													if (block.getBlockHardness(world, xPos, yPos, zPos) > 0F)
														this.onBlockDestroyed(stack, world, localBlock, xPos, yPos, zPos, player);

													ElectricItem.manager.use(stack, this.energyPerOperation, player);
												}
												else
													world.setBlockToAir(xPos, yPos, zPos);

												world.func_147479_m(xPos, yPos, zPos);
											}
									}
								}
								else
								{
									lowPower = true;
									break;
								}

					if (lowPower)
						ServerProxy.sendPlayerMessage(player, "Not enough energy to complete this operation !");
					else if (!GraviSuite.isSimulating())
						world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block) + (meta << 12));

					return true;
				}
				else
					return super.onBlockStartBreak(stack, x, y, z, player);
			}
		}
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int xPos, int yPos, int zPos, EntityLivingBase entity)
	{
		if (!GraviSuite.isSimulating())
			return true;
		else if (block == null)
			return false;
		else
		{
			if (entity != null)
			{
				int toolMode = readToolMode(stack);
				float energy;

				switch (toolMode)
				{
					case 0:
						energy = this.energyPerOperation;
						break;
					case 1:
						energy = this.energyPerLowOperation;
						break;
					case 2:
						energy = this.energyPerUltraLowOperation;
						break;
					default:
						energy = 0F;
						break;
				}

				if (energy != 0F && block.getBlockHardness(world, xPos, yPos, zPos) != 0F)
					ElectricItem.manager.use(stack, energy, entity);
			}

			return true;
		}
	}

	public static int readToolMode(ItemStack itemstack)
	{
		NBTTagCompound nbt = GraviSuite.getOrCreateNbtData(itemstack);
		int toolMode = nbt.getInteger("toolMode");
		// TODO gamerforEA code start
		if (EventConfig.disableAdvDDrillBigHoleMode && toolMode == 3)
			toolMode = 0;
		// TODO gamerforEA code end
		if (toolMode < 0 || toolMode > 3)
			toolMode = 0;

		return toolMode;
	}

	public void saveToolMode(ItemStack itemstack, int toolMode)
	{
		NBTTagCompound nbt = GraviSuite.getOrCreateNbtData(itemstack);
		nbt.setInteger("toolMode", toolMode);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		for (int i = 0; i < player.inventory.mainInventory.length; ++i)
		{
			ItemStack torchStack = player.inventory.mainInventory[i];
			if (torchStack != null && torchStack.getUnlocalizedName().toLowerCase().contains("torch"))
			{
				Item item = torchStack.getItem();
				if (item instanceof ItemBlock)
				{
					int oldMeta = torchStack.getItemDamage();
					int oldSize = torchStack.stackSize;
					boolean result = torchStack.tryPlaceItemIntoWorld(player, world, x, y, z, side, xOffset, yOffset, zOffset);
					if (player.capabilities.isCreativeMode)
					{
						torchStack.setItemDamage(oldMeta);
						torchStack.stackSize = oldSize;
					}
					else if (torchStack.stackSize <= 0)
					{
						ForgeEventFactory.onPlayerDestroyItem(player, torchStack);
						player.inventory.mainInventory[i] = null;
					}

					if (result)
						return true;
				}
			}
		}

		return super.onItemUse(stack, player, world, x, y, z, side, xOffset, yOffset, zOffset);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player)
	{
		if (Keyboard.isModeKeyDown(player))
		{
			int toolMode = readToolMode(itemStack) + 1;
			// TODO gamerforEA code start
			if (EventConfig.disableAdvDDrillBigHoleMode && toolMode == 3)
				toolMode = 0;
			// TODO gamerforEA code end
			if (toolMode > 3)
				toolMode = 0;

			this.saveToolMode(itemStack, toolMode);
			switch (toolMode)
			{
				case 0:
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GREEN + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.advDDrill.mode.normal"));
					super.efficiencyOnProperMaterial = this.normalPower;
					break;
				case 1:
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.advDDrill.mode.lowPower"));
					super.efficiencyOnProperMaterial = this.lowPower;
					break;
				case 2:
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.AQUA + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.advDDrill.mode.fine"));
					super.efficiencyOnProperMaterial = this.ultraLowPower;
					break;
				case 3:
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.LIGHT_PURPLE + Helpers.formatMessage("message.text.mode") + ": " + Helpers.formatMessage("message.advDDrill.mode.bigHoles"));
					super.efficiencyOnProperMaterial = this.bigHolePower;
					break;
			}
		}

		return itemStack;
	}

	public static MovingObjectPosition raytraceFromEntity(World world, Entity player, boolean par3, double range)
	{
		float pitch = player.rotationPitch;
		float yaw = player.rotationYaw;
		double x = player.posX;
		double y = player.posY;
		double z = player.posZ;

		if (!world.isRemote && player instanceof EntityPlayer)
			++y;

		Vec3 vec3 = Vec3.createVectorHelper(x, y, z);
		float f3 = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
		float f4 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
		float f5 = -MathHelper.cos(-pitch * 0.017453292F);
		float f6 = MathHelper.sin(-pitch * 0.017453292F);
		float f7 = f4 * f5;
		float f8 = f3 * f5;

		if (player instanceof EntityPlayerMP)
			range = ((EntityPlayerMP) player).theItemInWorldManager.getBlockReachDistance();

		Vec3 vec31 = vec3.addVector(range * f7, range * f6, range * f8);
		return world.func_147447_a(vec3, vec31, par3, !par3, par3);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4)
	{
		super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
		int toolMode = readToolMode(par1ItemStack);
		String mode = null;

		switch (toolMode)
		{
			case 0:
				mode = "normal";
				break;
			case 1:
				mode = "lowPower";
				break;
			case 2:
				mode = "fine";
				break;
			case 3:
				mode = "bigHoles";
				break;
		}

		if (mode != null)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.text.mode") + ": " + EnumChatFormatting.WHITE + Helpers.formatMessage("message.advDDrill.mode." + mode));
	}

	public String getRandomDrillSound()
	{
		switch (GraviSuite.random.nextInt(4))
		{
			case 1:
				return "drillOne";
			case 2:
				return "drillTwo";
			case 3:
				return "drillThree";
			default:
				return "drill";
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List subs)
	{
		ItemStack stack = new ItemStack(this, 1);
		ElectricItem.manager.charge(stack, 2.147483647E9D, Integer.MAX_VALUE, true, false);
		subs.add(stack);
		subs.add(new ItemStack(this, 1, this.getMaxDamage()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public EnumRarity getRarity(ItemStack var1)
	{
		return EnumRarity.uncommon;
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