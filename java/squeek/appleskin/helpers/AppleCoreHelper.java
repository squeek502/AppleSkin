package squeek.appleskin.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.food.FoodValues;

public class AppleCoreHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return AppleCoreAPI.accessor.isFood(itemStack);
	}

	public static FoodHelper.BasicFoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		FoodValues foodValues = AppleCoreAPI.accessor.getFoodValues(itemStack);
		return new FoodHelper.BasicFoodValues(foodValues.hunger, foodValues.saturationModifier);
	}

	public static FoodHelper.BasicFoodValues getModifiedFoodValues(ItemStack itemStack, EntityPlayer player)
	{
		FoodValues foodValues = AppleCoreAPI.accessor.getFoodValuesForPlayer(itemStack, player);
		return new FoodHelper.BasicFoodValues(foodValues.hunger, foodValues.saturationModifier);
	}

	public static FoodHelper.BasicFoodValues getFoodValuesForDisplay(FoodHelper.BasicFoodValues values, EntityPlayer player)
	{
		int maxHunger = AppleCoreAPI.accessor.getMaxHunger(player);

		if (maxHunger == 20)
			return values;

		float scale = 20f / maxHunger;
		return new FoodHelper.BasicFoodValues(MathHelper.ceil((double) values.hunger * scale), values.saturationModifier);
	}

	public static float getMaxExhaustion(EntityPlayer player)
	{
		return AppleCoreAPI.accessor.getMaxExhaustion(player);
	}

	public static float getExhaustion(EntityPlayer player)
	{
		return AppleCoreAPI.accessor.getExhaustion(player);
	}

	public static void setExhaustion(EntityPlayer player, float exhaustion)
	{
		AppleCoreAPI.mutator.setExhaustion(player, exhaustion);
	}
}
