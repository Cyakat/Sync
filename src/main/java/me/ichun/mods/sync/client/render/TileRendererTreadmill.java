package me.ichun.mods.sync.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper; import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import me.ichun.mods.sync.client.model.ModelTreadmill;
import me.ichun.mods.sync.common.tileentity.TileEntityTreadmill;

public class TileRendererTreadmill extends TileEntitySpecialRenderer<TileEntityTreadmill>
{

	public static final ResourceLocation txTreadmill = new ResourceLocation("sync", "textures/model/treadmill.png");
	
	public ModelTreadmill modelTreadmill;
	
	public TileRendererTreadmill()
	{
		modelTreadmill = new ModelTreadmill();
	}

	@Override
	public void renderTileEntityAt(TileEntityTreadmill tm, double d, double d1, double d2, float partialTicks, int destroyStage)
	{
		if(tm.back)
		{
			return;
		}
		GL11.glPushMatrix();
		
		GL11.glTranslated(d + 0.5D, d1 + 0.75, d2 + 0.5D);
		GL11.glScalef(-0.5F, -0.5F, 0.5F);
		
		GL11.glRotatef((tm.face * 90F), 0.0F, 1.0F, 0.0F);
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		Minecraft.getMinecraft().renderEngine.bindTexture(txTreadmill);
		modelTreadmill.render(0.0625F);

		if(tm.latchedEnt != null)
		{
			tm.latchedEnt.prevRenderYawOffset = tm.latchedEnt.renderYawOffset = (tm.face - 2) * 90F;
			tm.latchedEnt.limbSwingAmount = 1.5F + 3.5F * MathHelper.clamp_float(((float)tm.timeRunning / 12000F), 0.0F, 1.0F);
		}
		
		GL11.glDisable(GL11.GL_BLEND);
		
		GL11.glPopMatrix();
	}
}
