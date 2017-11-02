package squeek.appleskin.helpers;

import betterwithmods.api.FeatureEnabledEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BetterWithModsHelper
{
	private static String HC_HUNGER_FEATURE_KEY = "hchunger";
	public static boolean HC_HUNGER_ENABLED = false;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new BetterWithModsHelper());
	}

	@SubscribeEvent
	public void bwmFeatureEnabled(FeatureEnabledEvent event)
	{
		if (event.getFeature().equals(HC_HUNGER_FEATURE_KEY))
			BetterWithModsHelper.HC_HUNGER_ENABLED = event.isEnabled();
	}

	public static FoodHelper.BasicFoodValues getFoodValuesForDisplay(FoodHelper.BasicFoodValues values)
	{
		if (HC_HUNGER_ENABLED)
			return new BWMFoodValues(values.hunger, values.saturationModifier);
		else
			return values;
	}

	public static class BWMFoodValues extends FoodHelper.BasicFoodValues
	{
		public BWMFoodValues(int hunger, float saturationModifier)
		{
			super(hunger, saturationModifier);
		}

		// This is actually quite variable, as BWM multiples the returned value by the
		// amount above the max hunger level that eating the food gets you to
		// but this seems like the best way to display that, I guess
		public float getSaturationIncrement()
		{
			return Math.min(20, saturationModifier / 3f);
		}
	}
}
