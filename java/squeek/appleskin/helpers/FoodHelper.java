package squeek.appleskin.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import squeek.appleskin.AppleSkin;

public class FoodHelper
{
	public static class BasicFoodValues
	{
		public final int hunger;
		public final float saturationModifier;

		public BasicFoodValues(int hunger, float saturationModifier)
		{
			this.hunger = hunger;
			this.saturationModifier = saturationModifier;
		}

		public float getSaturationIncrement()
		{
			return Math.min(20, hunger * saturationModifier * 2f);
		}
	}

	public static boolean isFood(ItemStack itemStack)
	{
		if (AppleSkin.hasAppleCore)
			return AppleCoreHelper.isFood(itemStack);

		return itemStack.getItem() instanceof ItemFood;
	}

	public static BasicFoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		if (AppleSkin.hasAppleCore)
			return AppleCoreHelper.getDefaultFoodValues(itemStack);

		ItemFood itemFood = (ItemFood) itemStack.getItem();
		int hunger = itemFood.getHealAmount(itemStack);
		float saturationModifier = itemFood.getSaturationModifier(itemStack);

		return new BasicFoodValues(hunger, saturationModifier);
	}

	public static BasicFoodValues getModifiedFoodValues(ItemStack itemStack, EntityPlayer player)
	{
		if (AppleSkin.hasAppleCore)
			return AppleCoreHelper.getModifiedFoodValues(itemStack, player);

		return getDefaultFoodValues(itemStack);
	}
}
