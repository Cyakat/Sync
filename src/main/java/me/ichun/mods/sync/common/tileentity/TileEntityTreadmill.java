package me.ichun.mods.sync.common.tileentity;

import cofh.api.energy.IEnergyHandler;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import me.ichun.mods.sync.common.Sync;

import java.util.List;

@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore")
public class TileEntityTreadmill extends TileEntity implements IEnergyHandler, ITickable
{
	public TileEntityTreadmill pair;
	
	public boolean back;
	
	public int face;
	
	public EntityLiving latchedEnt;
	public int latchedEntId;
	
	public float latchedHealth;
	public int timeRunning;
	
	public boolean resync;

	public TileEntityTreadmill()
	{
		pair = null;
		back = false;
		latchedEnt = null;
		
		face = 0;
		
		resync = false;
	}
	
	@Override
	public void update()
	{
		if(resync)
		{
			TileEntity te = worldObj.getTileEntity(back ? (face == 1 ? xCoord + 1 : face == 3 ? xCoord - 1 : xCoord) : (face == 1 ? xCoord - 1 : face == 3 ? xCoord + 1 : xCoord), yCoord, back ? (face == 0 ? zCoord - 1 : face == 2 ? zCoord + 1 : zCoord) : (face == 0 ? zCoord + 1 : face == 2 ? zCoord - 1 : zCoord));
			if(te != null && te.getClass() == this.getClass())
			{
				TileEntityTreadmill sc = (TileEntityTreadmill)te;
				sc.pair = this;
				pair = sc;
			}
			if(latchedEntId != -1)
			{
				if(worldObj.isRemote)
				{
					Entity ent = worldObj.getEntityByID(latchedEntId);
					if(ent != null && ent.getDistance(getMidCoord(0), yCoord + 0.175D, getMidCoord(1)) < 7D)
					{
						latchedEnt = (EntityLiving)ent;
						latchedHealth = latchedEnt.getHealth();
					}
				}
				else
				{
					AxisAlignedBB aabb = new AxisAlignedBB(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), getMidCoord(0), yCoord + 0.175D, getMidCoord(1)).expand(0.4D, 0.4D, 0.4D);
					List list = worldObj.getEntitiesWithinAABB(Entity.class, aabb);

                    for (Object aList : list) {
                        Entity ent = (Entity) aList;

                        if (isEntityValidForTreadmill(ent)) {
                            if (ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ) {
                                latchedEnt = (EntityLiving) ent;
                                latchedHealth = latchedEnt.getHealth();
                                latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
                                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                                break;
                            }
                        }
                    }
				}
			}
			else if(latchedEnt != null)
			{
				latchedEnt = null;
				timeRunning = 0;
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
		}
		resync = false;
		
		if(worldObj.isRemote && !back)
		{
			if(latchedEnt == null && latchedEntId != -1 && worldObj.getWorldTime() % 27L == 0L)
			{
				Entity ent = worldObj.getEntityByID(latchedEntId);
				if(ent != null && ent.getDistance(getMidCoord(0), yCoord + 0.175D, getMidCoord(1)) < 3D)
				{
					latchedEnt = (EntityLiving)ent;
					latchedHealth = latchedEnt.getHealth();
				}
			}
			if(latchedEnt != null && latchedEnt.isDead)
			{
				Entity ent = worldObj.getEntityByID(latchedEntId);
				if(ent != null && ent.getDistance(getMidCoord(0), yCoord + 0.175D, getMidCoord(1)) < 7D)
				{
					latchedEnt = (EntityLiving)ent;
					latchedHealth = latchedEnt.getHealth();
				}
			}
			if(latchedEnt != null)
			{
				latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
				timeRunning++;
				if(timeRunning > 12000)
				{
					timeRunning = 12000;
				}
				
				if(0.3F + (MathHelper.clamp_float((float)timeRunning / 12000F, 0.0F, 1.0F) * 0.7F) > worldObj.rand.nextFloat())
				{
					spawnParticles();
				}
			}
		}
		if(!worldObj.isRemote && !back)
		{
			AxisAlignedBB aabb = latchedEnt != null ? latchedEnt.getEntityBoundingBox().contract(0.1D) : new AxisAlignedBB(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), getMidCoord(0), yCoord + 0.175D, getMidCoord(1)).expand(0.15D, 0.005D, 0.15D);
			List list = worldObj.getEntitiesWithinAABB(Entity.class, aabb);
	
			if(latchedEnt != null)
			{
				boolean remove = false;
				if(latchedEnt instanceof EntityTameable)
				{
					EntityTameable entityTameable = (EntityTameable)latchedEnt;
					//Remove sitting entities
					if(entityTameable.isSitting())
					{
						timeRunning = 0;
						worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
						
						remove = true;
					}
					if(entityTameable.isTamed())
					{
						latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
						
						aabb = latchedEnt.getEntityBoundingBox().contract(0.1D);
						list = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
					}
					else
					{
						entityTameable.ticksExisted = 1200; //anti despawn methods
					}
				}
				for (Object aList : list) {
					Entity ent = (Entity) aList;

					if (ent != latchedEnt && ent instanceof EntityLivingBase && !(ent instanceof EntityPlayer)) {
						double velo = 0.9D;
						switch (face) {
							case 0: {
								ent.motionZ = velo;
								break;
							}
							case 1: {
								ent.motionX = -velo;
								break;
							}
							case 2: {
								ent.motionZ = -velo;
								break;
							}
							case 3: {
								ent.motionX = velo;
								break;
							}
						}

						remove = true;
					}
				}
				if(latchedEnt != null && (!list.contains(latchedEnt) || remove || latchedHealth > latchedEnt.getHealth()))
				{
					if(latchedHealth <= latchedEnt.getHealth())
					{
						double velo = 1.3D;
						switch(face)
						{
							case 0:
							{
								latchedEnt.motionZ = velo;
								break;
							}
							case 1:
							{
								latchedEnt.motionX = -velo;
								break;
							}
							case 2:
							{
								latchedEnt.motionZ = -velo;
								break;
							}
							case 3:
							{
								latchedEnt.motionX = velo;
								break;
							}
						}
					}
					latchedEnt = null;
					timeRunning = 0;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}
				if(latchedEnt != null)
				{
					latchedHealth = latchedEnt.getHealth();
					latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
					latchedEnt.getNavigator().clearPathEntity();
					if (timeRunning < 12000) {
						timeRunning++;
					}
					
					//Still running. This sends RF power to nearby IEnergyHandlers
                    if (Sync.hasCoFHCore) {
                        this.sendRFEnergyToNearbyDevices();
                    }
				}
			}
			else
			{
				for (Object aList : list) {
					Entity ent = (Entity) aList;

					if (TileEntityTreadmill.isEntityValidForTreadmill(ent)) {
						if (ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ) {
							latchedEnt = (EntityLiving) ent;
							latchedHealth = latchedEnt.getHealth();
							timeRunning = 0;
							latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
							worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
							break;
						}
					}
				}
			}
		}
	}

    @Optional.Method(modid = "CoFHCore")
    private void sendRFEnergyToNearbyDevices() {
        float power = powerOutput() / (float)Sync.config.ratioRF; //2PW = 1RF
        int handlerCount = 0;
        IEnergyHandler[] handlers = new IEnergyHandler[ForgeDirection.VALID_DIRECTIONS.length];
        for(ForgeDirection dir:ForgeDirection.VALID_DIRECTIONS)
        {
            if(dir == ForgeDirection.UP)
            {
                continue;
            }
            TileEntity te = worldObj.getTileEntity(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ);
            if(te instanceof IEnergyHandler && !(te instanceof TileEntityDualVertical))
            {
                IEnergyHandler energy = (IEnergyHandler) te;
                if(energy.canConnectEnergy(dir.getOpposite()))
                {
                    handlerCount++;
                    //Test if they can recieve power via simulate
                    if(energy.receiveEnergy(dir.getOpposite(), (int)power, true) > 0)
                    {
                        handlers[dir.getOpposite().ordinal()] = energy;
                    }
                }
            }
        }
        for(int i = 0; i < handlers.length; i++)
        {
            IEnergyHandler handler = handlers[i];
            if(handler != null)
            {
                //Sends power equally to all nearby IEnergyHandlers that can receive it
                handler.receiveEnergy(ForgeDirection.getOrientation(i), Math.max(Math.round(power / (float)handlerCount), 1), false);
            }
        }
    }
	
	@SideOnly(Side.CLIENT)
	public void spawnParticles() 
	{
		if(latchedEnt != null && pair != null)
		{
			double xVelo = (face == 1 ? -30D : face == 3 ? 30.0D : 0.0D);
			double zVelo = face == 0 ? 30D : face == 2 ? -30D : 0.0D;
			if(worldObj.rand.nextFloat() < 0.5F)
			{
				Minecraft.getMinecraft().effectRenderer.addEffect((new EntityDiggingFX(worldObj, pair.xCoord + worldObj.rand.nextFloat(), pair.yCoord + 0.4D, pair.zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo, Sync.blockDualVertical, 2)).applyRenderColor(2));
			}
			else
			{
				Minecraft.getMinecraft().effectRenderer.addEffect((new EntityDiggingFX(worldObj, xCoord + worldObj.rand.nextFloat(), yCoord + 0.4D, zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo, Sync.blockDualVertical, 2)).applyRenderColor(2));
			}
			
			if(timeRunning == 12000 && worldObj.rand.nextFloat() < 0.2F)
			{
				xVelo *= 0.01D;
				zVelo *= 0.01D;
				Minecraft.getMinecraft().effectRenderer.addEffect(new EntitySmokeFX(worldObj, xCoord + worldObj.rand.nextFloat(), yCoord + 0.4D, zCoord + worldObj.rand.nextFloat(), xVelo, 0.0D, zVelo));
			}
		}
	}

	public double getMidCoord(int i)
	{
		if(back && pair != null)
		{
			return pair.getMidCoord(i);
		}
		if(i == 0)//x coord
		{
			return (face == 1 ? xCoord : face == 3 ? xCoord + 1 : xCoord + 0.5D);
		}
		else //z coord
		{
			return (face == 0 ? zCoord + 1 : face == 2 ? zCoord : zCoord + 0.5D);
		}
	}
	
	public float powerOutput()
	{
		if(back && pair != null)
		{
			return pair.powerOutput();
		}
		float power = 0.0F;
		if(latchedEnt != null)
		{
			power = Sync.TREADMILL_ENTITY_HASH_MAP.get(latchedEnt.getClass());
			if (latchedEnt instanceof EntityTameable && ((EntityTameable) latchedEnt).isTamed()) power = (power / 2) + (power / 4); //Decrease power if the entity isn't tamed
			power += MathHelper.clamp_float((float)timeRunning / 12000F, 0.0F, 1.0F) * 2F;
		}
		return power;
	}
	
	public void setup(TileEntityTreadmill sc, boolean b, int face2) 
	{
		pair = sc;
		back = b;
		face = face2;
	}
	
	@Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		readFromNBT(pkt.func_148857_g());
	}

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

	@Override
	public void writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		tag.setBoolean("back", back);
		tag.setInteger("face", face);
		tag.setInteger("latchedID", latchedEnt != null ? latchedEnt.getEntityId() : -1);
		tag.setInteger("timeRunning", timeRunning);
	}
	 
	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		back = tag.getBoolean("back");
		face = tag.getInteger("face");
		latchedEntId = tag.getInteger("latchedID");
		timeRunning = tag.getInteger("timeRunning");
		
		resync = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return AxisAlignedBB.getBoundingBox(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 1, zCoord + 2);
	}

	@Override
	public Block getBlockType()
	{
		return Sync.blockDualVertical;
	}

	//Will return true if the entity can use the treadmill
	public static boolean isEntityValidForTreadmill(Entity entity) {
		return Sync.TREADMILL_ENTITY_HASH_MAP.containsKey(entity.getClass()) && !((EntityLiving) entity).isChild() && !(entity instanceof EntityTameable && ((EntityTameable) entity).isSitting());
	}
	
	// TE methods
	@Override
	@Optional.Method(modid = "CoFHCore")
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		return 0;
	}

	@Override
	@Optional.Method(modid = "CoFHCore")
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean doExtract)
	{
		return 0;
	}

	@Override
	@Optional.Method(modid = "CoFHCore")
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return !back;
	}

	@Override
	@Optional.Method(modid = "CoFHCore")
	public int getEnergyStored(ForgeDirection from)
	{
		return 0;
	}

	@Override
	@Optional.Method(modid = "CoFHCore")
	public int getMaxEnergyStored(ForgeDirection from)
	{
		return 0;
	}
}
