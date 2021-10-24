package squeek.appleskin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.HungerManager;
import squeek.appleskin.ModConfig;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;

import java.text.DecimalFormat;
import java.util.List;

public class DebugInfoHandler
{
	public static DebugInfoHandler INSTANCE;

	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	public static void init()
	{
		INSTANCE = new DebugInfoHandler();
	}

	public void onTextRender(List<String> leftDebugInfo)
	{
		if (leftDebugInfo == null)
			return;

		if (!ModConfig.INSTANCE.showFoodDebugInfo)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.player == null || mc.player.getHungerManager() == null)
			return;

		if (!mc.options.debugEnabled)
			return;

		HungerManager stats = mc.player.getHungerManager();
		float curExhaustion = HungerHelper.getExhaustion(mc.player);
		float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
		leftDebugInfo.add("hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
	}
}
