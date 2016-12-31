package squeek.appleskin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.FoodStats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import squeek.appleskin.ModConfig;
import squeek.appleskin.helpers.HungerHelper;

import java.text.DecimalFormat;

@SideOnly(Side.CLIENT)
public class DebugInfoHandler
{
	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new DebugInfoHandler());
	}

	@SubscribeEvent
	public void onTextRender(RenderGameOverlayEvent.Text textEvent)
	{
		if (textEvent.getType() != RenderGameOverlayEvent.ElementType.TEXT)
			return;

		if (!ModConfig.SHOW_FOOD_DEBUG_INFO)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		if (mc.gameSettings.showDebugInfo)
		{
			FoodStats stats = mc.player.getFoodStats();
			float curExhaustion = HungerHelper.getExhaustion(mc.player);
			float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
			textEvent.getLeft().add("hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
		}
	}
}
