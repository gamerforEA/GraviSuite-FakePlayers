package gravisuite;

import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.gravisuite.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gravisuite.keyboard.Keyboard;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ItemVajra extends ItemTool implements IElectricItem
{
	private int maxCharge;
	private int tier;
	private float effPower;
	private int energyPerOperation;
	private int transferLimit;
	private int toolMode;

	protected ItemVajra(ToolMaterial toolMaterial)
	{
		super(0.0F, toolMaterial, new HashSet());
		this.setMaxDamage(27);
		this.maxCharge = 3000000;
		this.tier = 2;
		this.transferLimit = '\uea60';
		this.effPower = 20000.0F;
		this.efficiencyOnProperMaterial = this.effPower;
		this.energyPerOperation = 3333;
		this.setCreativeTab(GraviSuite.ic2Tab);
		this.toolMode = 0;
	}

	public static Integer readToolMode(ItemStack itemstack)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		Integer toolMode = nbttagcompound.getInteger("toolMode");
		if (toolMode < 0 || toolMode > 1)
			toolMode = 0;

		return toolMode;
	}

	public void saveToolMode(ItemStack itemstack, Integer toolMode)
	{
		NBTTagCompound nbttagcompound = GraviSuite.getOrCreateNbtData(itemstack);
		nbttagcompound.setInteger("toolMode", toolMode);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player)
	{
		if (Keyboard.isModeKeyDown(player))
			if (!GraviSuite.disableVajraAccurate)
			{
				Integer toolMode = readToolMode(itemStack);
				toolMode = toolMode + 1;
				if (toolMode > 1)
					toolMode = 0;

				this.saveToolMode(itemStack, toolMode);
				if (toolMode == 0)
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GOLD + Helpers.formatMessage("message.vajra.silkTouchMode") + ": " + EnumChatFormatting.RED + Helpers.formatMessage("message.text.disabled"));

				if (toolMode == 1)
					ServerProxy.sendPlayerMessage(player, EnumChatFormatting.GOLD + Helpers.formatMessage("message.vajra.silkTouchMode") + ": " + EnumChatFormatting.GREEN + Helpers.formatMessage("message.text.enabled"));
			}
			else
				ServerProxy.sendPlayerMessage(player, EnumChatFormatting.RED + Helpers.formatMessage("message.vajra.silkTouchDisabled"));

		return itemStack;
	}

	@Override
	public boolean onItemUse(ItemStack itemstack, EntityPlayer player, World world, int x, int y, int z, int side, float a, float b, float c)
	{
		int toolMode = readToolMode(itemstack);
		if (!GraviSuite.disableVajraAccurate && toolMode == 1)
			try
			{
				int meta = world.getBlockMetadata(x, y, z);
				Block block = world.getBlock(x, y, z);
				if (block != Blocks.bedrock && block != Blocks.mob_spawner && block.canHarvestBlock(player, meta) && block.getItemDropped(meta, world.rand, 1) != null)
				{
					if (!ElectricItem.manager.canUse(itemstack, this.energyPerOperation))
						return false;

					if (GraviSuite.isSimulating())
					{
						// TODO gamerforEA code start
						if (block.getBlockHardness(world, x, y, z) < 0)
							return false;
						if (EventConfig.inList(EventConfig.vajraSilkBlackList, block, meta))
							return false;
						if (EventUtils.cantBreak(player, x, y, z))
							return false;
						// TODO gamerforEA code end

						boolean dropFlag = false;
						if (block.canSilkHarvest(world, player, x, y, z, meta))
						{
							ArrayList<ItemStack> items = new ArrayList();
							ItemStack stack = this.createStackedBlock(block, meta);
							if (stack != null)
								items.add(stack);

							ForgeEventFactory.fireBlockHarvesting(items, world, block, x, y, z, meta, 0, 1.0F, true, player);

							for (ItemStack is : items)
							{
								ItemGraviTool.dropAsEntity(world, x, y, z, is);
							}

							dropFlag = true;
						}
						else
						{
							int count = block.quantityDropped(meta, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack), world.rand);
							if (count > 0)
							{
								int exp = block.getExpDrop(world, meta, EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemstack));
								block.dropXpOnBlockBreak(world, x, y, z, exp);

								/* TODO gamerforEA code replace, old code:
								block.harvestBlock(world, entityplayer, i, j, k, metaData);
								block.onBlockHarvested(world, i, j, k, metaData, entityplayer);
								float blockHardness = block.getBlockHardness(world, i, j, k); */
								float blockHardness = block.getBlockHardness(world, x, y, z);

								block.onBlockHarvested(world, x, y, z, meta, player);
								if (block.removedByPlayer(world, player, x, y, z, true))
								{
									block.onBlockDestroyedByPlayer(world, x, y, z, meta);
									block.harvestBlock(world, player, x, y, z, meta);
								}
								// TODO gamerforEA code end

								if (blockHardness > 0.0F)
									this.onBlockDestroyed(itemstack, world, block, x, y, z, player);

								world.func_147479_m(x, y, z);
								dropFlag = true;
							}
						}

						if (dropFlag)
						{
							world.setBlockToAir(x, y, z);
							world.func_147479_m(x, y, z);
							world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block) + (meta << 12));
							ElectricItem.manager.use(itemstack, this.energyPerOperation, player);
						}
					}

					return true;
				}
			}
			catch (Exception var22)
			{
				GraviSuite.addLog("Vajra: Error in destroy function (" + var22.getLocalizedMessage() + ")");
			}
			finally
			{
			}

		return false;
	}

	protected ItemStack createStackedBlock(Block block, int meta)
	{
		int j = 0;
		Item item = Item.getItemFromBlock(block);
		if (item != null && item.getHasSubtypes())
			j = meta;

		return new ItemStack(item, 1, j);
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
	public boolean canHarvestBlock(Block block, ItemStack stack)
	{
		return block != Blocks.bedrock;
	}

	@Override
	public int getHarvestLevel(ItemStack stack, String toolClass)
	{
		return this.toolMaterial.getHarvestLevel();
	}

	@Override
	public float getDigSpeed(ItemStack tool, Block block, int meta)
	{
		return !ElectricItem.manager.canUse(tool, this.energyPerOperation) ? 1.0F : this.canHarvestBlock(block, tool) ? this.efficiencyOnProperMaterial : 1.0F;
	}

	@Override
	public boolean hitEntity(ItemStack itemstack, EntityLivingBase entityliving, EntityLivingBase attacker)
	{
		if (ElectricItem.manager.use(itemstack, this.energyPerOperation * 2, attacker))
			entityliving.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 25.0F);
		else
			entityliving.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 1.0F);

		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister)
	{
		this.itemIcon = iconRegister.registerIcon("gravisuite:itemVajra");
	}

	public float getStrVsBlock(ItemStack itemstack, Block par2Block)
	{
		return ElectricItem.manager.canUse(itemstack, this.energyPerOperation) ? this.effPower : 0.5F;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack itemstack, World world, Block block, int xPos, int yPos, int zPos, EntityLivingBase entityliving)
	{
		if (block.getBlockHardness(world, xPos, yPos, zPos) != 0.0D)
			if (entityliving != null)
				ElectricItem.manager.use(itemstack, this.energyPerOperation, entityliving);
			else
				ElectricItem.manager.discharge(itemstack, this.energyPerOperation, this.tier, true, false, false);

		return true;
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
	public EnumRarity getRarity(ItemStack var1)
	{
		return EnumRarity.epic;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4)
	{
		super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
		Integer toolMode = readToolMode(par1ItemStack);
		if (toolMode == 0)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.vajra.silkTouchMode") + ": " + EnumChatFormatting.RED + Helpers.formatMessage("message.text.disabled"));

		if (toolMode == 1)
			par3List.add(EnumChatFormatting.GOLD + Helpers.formatMessage("message.vajra.silkTouchMode") + ": " + EnumChatFormatting.GREEN + Helpers.formatMessage("message.text.enabled"));

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
