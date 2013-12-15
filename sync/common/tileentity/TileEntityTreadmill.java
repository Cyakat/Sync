package sync.common.tileentity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import sync.common.Sync;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityTreadmill extends TileEntity 
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
	public void updateEntity()
	{
		if(resync)
		{
			TileEntity te = worldObj.getBlockTileEntity(back ? (face == 1 ? xCoord + 1 : face == 3 ? xCoord - 1 : xCoord) : (face == 1 ? xCoord - 1 : face == 3 ? xCoord + 1 : xCoord), yCoord, back ? (face == 0 ? zCoord - 1 : face == 2 ? zCoord + 1 : zCoord) : (face == 0 ? zCoord + 1 : face == 2 ? zCoord - 1 : zCoord));
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
					if(ent != null && ent.getDistance(getMidCoord(0), yCoord + 0.175D, getMidCoord(1)) < 0.3D)
					{
						latchedEnt = (EntityLiving)ent;
						latchedHealth = latchedEnt.getHealth();
					}
				}
				else
				{
					AxisAlignedBB aabb = AxisAlignedBB.getAABBPool().getAABB(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), getMidCoord(0), yCoord + 0.175D, getMidCoord(1)).expand(0.4D, 0.4D, 0.4D);
					List list = worldObj.getEntitiesWithinAABB(Entity.class, aabb);
					
					for(int i = 0 ; i < list.size(); i++)
					{
						Entity ent = (Entity)list.get(i);
						
						if(ent instanceof EntityLiving && !((EntityLiving)ent).isChild() && (ent instanceof EntityPig || ent instanceof EntityWolf && !((EntityWolf)ent).isSitting()))
						{
							if(ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ)
							{
								latchedEnt = (EntityLiving)ent;
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
		
		if(worldObj.isRemote && !back && latchedEnt != null)
		{
			latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
			timeRunning++;
			if(timeRunning > 12000)
			{
				timeRunning = 12000;
			}
		}
		if(!worldObj.isRemote && !back)
		{
			AxisAlignedBB aabb = latchedEnt != null ? latchedEnt.boundingBox.contract(0.1D, 0.1D, 0.1D) : AxisAlignedBB.getAABBPool().getAABB(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), getMidCoord(0), yCoord + 0.175D, getMidCoord(1)).expand(0.15D, 0.005D, 0.15D);
			List list = worldObj.getEntitiesWithinAABB(Entity.class, aabb);
	
			if(latchedEnt != null)
			{
				if(latchedEnt instanceof EntityWolf && ((EntityWolf)latchedEnt).isSitting())
				{
					EntityWolf wolf = (EntityWolf)latchedEnt;
					latchedEnt = null;
					timeRunning = 0;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}
				boolean remove = false;
				for(int i = 0 ; i < list.size(); i++)
				{
					Entity ent = (Entity)list.get(i);
					
					if(ent != latchedEnt && !(ent instanceof EntityPlayer))
					{
						remove = true;
						break;
					}
				}
				if(latchedEnt != null && (!list.contains(latchedEnt) || remove || latchedHealth > latchedEnt.getHealth()))
				{
					latchedEnt = null;
					timeRunning = 0;
					worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
				}
				if(latchedEnt != null)
				{
					latchedHealth = latchedEnt.getHealth();
					latchedEnt.setLocationAndAngles(getMidCoord(0), yCoord + 0.175D, getMidCoord(1), (face - 2) * 90F, 0.0F);
					latchedEnt.getNavigator().clearPathEntity();
					timeRunning++;
					if(timeRunning > 12000)
					{
						timeRunning = 12000;
					}
				}
			}
			else
			{
				for(int i = 0 ; i < list.size(); i++)
				{
					Entity ent = (Entity)list.get(i);
					
					if(ent instanceof EntityLiving && !((EntityLiving)ent).isChild() && (ent instanceof EntityPig || ent instanceof EntityWolf && !((EntityWolf)ent).isSitting()))
					{
						if(ent.posX > aabb.minX && ent.posX < aabb.maxX && ent.posY > aabb.minY && ent.posY < aabb.maxY && ent.posZ > aabb.minZ && ent.posZ < aabb.maxZ)
						{
							latchedEnt = (EntityLiving)ent;
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
		if(back)
		{
			return pair.powerOutput();
		}
		return 0F;
	}
	
	public void setup(TileEntityTreadmill sc, boolean b, int face2) 
	{
		pair = sc;
		back = b;
		face = face2;
	}
	
	@Override
	public void onDataPacket(INetworkManager net, Packet132TileEntityData pkt)
	{
		readFromNBT(pkt.data);
	}
	
	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new Packet132TileEntityData(xCoord, yCoord, zCoord, 0, tag);
	}
	
	@Override
    public void writeToNBT(NBTTagCompound tag)
    {
		super.writeToNBT(tag);
		tag.setBoolean("back", back);
		tag.setInteger("face", face);
		tag.setInteger("latchedID", latchedEnt != null ? latchedEnt.entityId : -1);
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
        return AxisAlignedBB.getAABBPool().getAABB(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 1, zCoord + 2);
    }

	@Override
    public Block getBlockType()
    {
        return Sync.blockDualVertical;
    }
}
