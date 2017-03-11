package gravisuite;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.FastUtils;
import com.gamerforea.gravisuite.ModUtils;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.api.item.ElectricItem;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityPlasmaBall extends EntityThrowable
{
	private EntityLivingBase ownerEntity;
	private double startX;
	private double startY;
	private double startZ;
	private double maxRange;
	private double speedPerTick;
	private double dischargeArmorValue;
	private byte actionType;
	private ItemRelocator.TeleportPoint targetTpPoint;
	public static final byte RELOCATOR_TELEPORT = 0;
	public static final byte RELOCATOR_PORTAL = 1;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = new FakePlayerContainerEntity(ModUtils.profile, this);
	// TODO gamerforEA code end

	public EntityPlasmaBall(World world, EntityLivingBase entityLiving, ItemRelocator.TeleportPoint tpPoint, byte entityType)
	{
		super(world, entityLiving);
		this.ownerEntity = entityLiving;
		this.startX = super.posX;
		this.startY = super.posY;
		this.startZ = super.posZ;
		this.maxRange = 32.0D;
		this.speedPerTick = 1.33D;
		this.targetTpPoint = tpPoint;
		this.actionType = entityType;
		this.dischargeArmorValue = 500000.0D;
		super.dataWatcher.updateObject(30, Byte.valueOf(this.actionType));

		// TODO gamerforEA code start
		if (entityLiving instanceof EntityPlayer)
			this.fake.setRealPlayer((EntityPlayer) entityLiving);
		// TODO gamerforEA code end
	}

	public EntityPlasmaBall(World world)
	{
		super(world);
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		super.dataWatcher.addObject(30, Byte.valueOf(this.actionType));
		super.dataWatcher.setObjectWatched(30);
	}

	@Override
	protected float getGravityVelocity()
	{
		return 0.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getShadowSize()
	{
		return 0.0F;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (super.worldObj.isRemote)
			;

		if (!super.worldObj.isRemote)
		{
			double distance = this.getDistance(this.startX, this.startY, this.startZ);
			if (distance >= this.maxRange || super.ticksExisted > this.maxRange / this.speedPerTick)
				this.setDead();
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setByte("actionType", this.actionType);

		// TODO gamerforEA code start
		this.fake.writeToNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.actionType = nbt.getByte("actionType");

		// TODO gamerforEA code start
		this.fake.readFromNBT(nbt);
		// TODO gamerforEA code end
	}

	public byte getActionType()
	{
		return super.dataWatcher.getWatchableObjectByte(30);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		if (mop.entityHit != null)
		{
			if (this.actionType == 0)
			{
				// TODO gamerforEA code start
				if (this.fake.cantDamage(mop.entityHit))
				{
					this.setDead();
					return;
				}
				// TODO gamerforEA code end

				if (mop.entityHit instanceof EntityPlayer)
				{
					EntityPlayer player = (EntityPlayer) mop.entityHit;
					ItemStack itemstack = player.inventory.armorInventory[2];
					if (itemstack != null && itemstack.getItem() == GraviSuite.graviChestPlate)
					{
						if (ElectricItem.manager.getCharge(itemstack) < this.dischargeArmorValue)
							Helpers.teleportEntity(mop.entityHit, this.targetTpPoint);
						else if (GraviSuite.isSimulating())
						{
							// TODO gamerforEA code clear: EntityClientPlayerMP senderPlayer = (EntityClientPlayerMP) this.ownerEntity;

							ServerProxy.sendPlayerMessage(player, this.ownerEntity.getCommandSenderName() + " " + Helpers.formatMessage("message.relocator.text.messageToTarget"));
							ElectricItem.manager.discharge(itemstack, this.dischargeArmorValue, Integer.MAX_VALUE, true, false, false);
						}
					}
					else
						Helpers.teleportEntity(mop.entityHit, this.targetTpPoint);
				}
				else
					Helpers.teleportEntity(mop.entityHit, this.targetTpPoint);
			}
		}
		else if (this.actionType == 1)
		{
			int curPosX = mop.blockX;
			int curPosY = mop.blockY;
			int curPosZ = mop.blockZ;
			switch (mop.sideHit)
			{
				case 0:
					--curPosY;
					break;
				case 1:
					++curPosY;
					break;
				case 2:
					--curPosZ;
					break;
				case 3:
					++curPosZ;
					break;
				case 4:
					--curPosX;
					break;
				case 5:
					++curPosX;
			}

			if (GraviSuite.isSimulating())
				try
				{
					// TODO gamerforEA code start
					if (this.fake.cantBreak(curPosX, curPosY, curPosZ))
					{
						this.setDead();
						return;
					}
					// TODO gamerforEA code end

					super.worldObj.setBlockToAir(curPosX, curPosY, curPosZ);
					super.worldObj.setBlock(curPosX, curPosY, curPosZ, GraviSuite.blockRelocatorPortal);
					super.worldObj.markBlockForUpdate(curPosX, curPosY, curPosZ);
					MinecraftServer minecraftserver = MinecraftServer.getServer();
					WorldServer targetServer = minecraftserver.worldServerForDimension(this.targetTpPoint.dimID);
					targetServer.theChunkProviderServer.loadChunk((int) this.targetTpPoint.x >> 4, (int) this.targetTpPoint.z >> 4);

					// TODO gamerforEA code start
					if (EventUtils.cantBreak(FastUtils.getFake(targetServer, this.fake.getPlayer()), (int) this.targetTpPoint.x, (int) this.targetTpPoint.y, (int) this.targetTpPoint.z))
					{
						this.setDead();
						return;
					}
					// TODO gamerforEA code end

					Block block = targetServer.getBlock((int) this.targetTpPoint.x, (int) this.targetTpPoint.y, (int) this.targetTpPoint.z);
					if (!(block instanceof BlockRelocatorPortal))
					{
						targetServer.setBlock((int) this.targetTpPoint.x, (int) this.targetTpPoint.y, (int) this.targetTpPoint.z, GraviSuite.blockRelocatorPortal);
						targetServer.markBlockForUpdate((int) this.targetTpPoint.x, (int) this.targetTpPoint.y, (int) this.targetTpPoint.z);
					}

					TileEntity tileEntity = targetServer.getTileEntity((int) this.targetTpPoint.x, (int) this.targetTpPoint.y, (int) this.targetTpPoint.z);
					if (tileEntity instanceof TileEntityRelocatorPortal)
					{
						ItemRelocator.TeleportPoint tmpPoint = new ItemRelocator.TeleportPoint();
						tmpPoint.dimID = super.worldObj.provider.dimensionId;
						tmpPoint.x = curPosX;
						tmpPoint.y = curPosY;
						tmpPoint.z = curPosZ;
						((TileEntityRelocatorPortal) tileEntity).setParentPortal(tmpPoint);
					}

					TileEntity currentTileEntity = super.worldObj.getTileEntity(curPosX, curPosY, curPosZ);
					if (tileEntity instanceof TileEntityRelocatorPortal)
						((TileEntityRelocatorPortal) currentTileEntity).setParentPortal(this.targetTpPoint);
				}
				catch (Exception ignore)
				{
				}
		}

		if (!super.worldObj.isRemote)
			this.setDead();

	}
}
