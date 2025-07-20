package squeek.appleskin.client;


import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.HungerManager;
import squeek.appleskin.ModConfig;
import squeek.appleskin.helpers.ExhaustionHelper;
import squeek.appleskin.helpers.FoodHelper;

import java.text.DecimalFormat;
import java.util.List;

public final class DebugInfoHandler
{
	public static DebugInfoHandler instance;

	private static final DecimalFormat SATURATION_DF = new DecimalFormat("#.##");
	private static final DecimalFormat EXHAUSTION_VAL_DF = new DecimalFormat("0.00");
	private static final DecimalFormat EXHAUSTION_MAX_DF = new DecimalFormat("#.##");

	public static void init()
	{
		instance = new DebugInfoHandler();
	}

	public void onTextRender(List<String> leftDebugInfo)
	{
		if (leftDebugInfo == null)
			return;

		if (!ModConfig.instance.showFoodDebugInfo)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.player == null || mc.player.getHungerManager() == null)
			return;

		if (!mc.getDebugHud().shouldShowDebugHud())
			return;

		HungerManager stats = mc.player.getHungerManager();
		float curExhaustion = ExhaustionHelper.getExhaustion(mc.player);
		float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
		leftDebugInfo.add("hunger: " + stats.getFoodLevel() + ", sat: " + SATURATION_DF.format(stats.getSaturationLevel()) + ", exh: " + EXHAUSTION_VAL_DF.format(curExhaustion) + "/" + EXHAUSTION_MAX_DF.format(maxExhaustion));
	}
}
