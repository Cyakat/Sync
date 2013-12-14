package sync.common.tileentity;

import sync.client.entity.EntityPaintFX;
import sync.common.core.SessionState;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TileEntityShellConstructor extends TileEntity 
{

	public TileEntityShellConstructor pair;
	public boolean top;
	public int face;
	public String playerName;
	public float constructionProgress;
	
	public ResourceLocation locationSkin;
	
	public boolean resync;
	
	public TileEntityShellConstructor()
	{
		pair = null;
		top = false;
		face = 0;
		playerName = "";
		constructionProgress = 0.0F;
		
		resync = false;
	}
	
	@Override
	public void updateEntity()
	{
		if(resync)
		{
			TileEntity te = worldObj.getBlockTileEntity(xCoord, yCoord + (top ? -1 : 1), zCoord);
			if(te instanceof TileEntityShellConstructor)
			{
				TileEntityShellConstructor sc = (TileEntityShellConstructor)te;
				sc.pair = this;
				pair = sc;
			}
			if(worldObj.isRemote)
			{
	            locationSkin = AbstractClientPlayer.getLocationSkin(playerName);
	            AbstractClientPlayer.getDownloadImageSkin(this.locationSkin, playerName);
			}
		}
		if(top && pair != null)
		{
			playerName = pair.playerName;
			constructionProgress = pair.constructionProgress;
		}
		if(isPowered())
		{
			constructionProgress += powerAmount();
			if(constructionProgress > SessionState.shellConstructionPowerRequirement)
			{
				constructionProgress = SessionState.shellConstructionPowerRequirement;
			}
			
			if(worldObj.isRemote && !top)
			{
				spawnParticles();
			}
		}
	}
	
	public void setup(TileEntityShellConstructor scPair, boolean isTop, int placeYaw)
	{
		pair = scPair;
		top = isTop;
		face = placeYaw;
	}
	
	public boolean isPowered()
	{
		if(top && pair != null)
		{
			return pair.isPowered();
		}
		if(playerName.equalsIgnoreCase(""))
		{
			return false;
		}
		return true;
	}
	
	public float powerAmount()
	{
		return 400F;
	}
	
	@SideOnly(Side.CLIENT)
	public void spawnParticles()
	{
//		float prog = MathHelper.clamp_float(this.constructionProgress, 0.0F, SessionState.shellConstructionPowerRequirement) / (float)SessionState.shellConstructionPowerRequirement;
//		if(prog > 0.95F)
//		{
//			float angle = 0;
//			
//			System.out.println(face);
//			Minecraft.getMinecraft().effectRenderer.addEffect(new EntityPaintFX(worldObj, xCoord + 0.5D , yCoord + 0.5D, zCoord + 0.5D, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 1.0F));
//		}
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
		tag.setBoolean("top", top);
		tag.setInteger("face", face);
		tag.setString("playerName", playerName);
		tag.setFloat("constructionProgress", constructionProgress);
    }
	 
	@Override
    public void readFromNBT(NBTTagCompound tag)
    {
		super.readFromNBT(tag);
		top = tag.getBoolean("top");
		face = tag.getInteger("face");
		playerName = tag.getString("playerName");
		constructionProgress = tag.getFloat("constructionProgress");
		
		resync = true;
    }
	
	@Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return AxisAlignedBB.getAABBPool().getAABB(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1);
    }
}
