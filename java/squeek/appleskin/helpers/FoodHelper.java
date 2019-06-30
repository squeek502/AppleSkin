package squeek.appleskin.helpers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;

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
			return hunger * saturationModifier * 2f;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (!(o instanceof BasicFoodValues)) return false;

			BasicFoodValues that = (BasicFoodValues) o;

			return hunger == that.hunger && Float.compare(that.saturationModifier, saturationModifier) == 0;
		}

		@Override
		public int hashCode()
		{
			int result = hunger;
			result = 31 * result + (saturationModifier != +0.0f ? Float.floatToIntBits(saturationModifier) : 0);
			return result;
		}
	}

	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.getItem().isFood();
	}

	public static BasicFoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		FoodComponent itemFood = itemStack.getItem().getFoodComponent();
		int hunger = itemFood.getHunger();
		float saturationModifier = itemFood.getSaturationModifier();
		return new BasicFoodValues(hunger, saturationModifier);
	}

	public static BasicFoodValues getModifiedFoodValues(ItemStack itemStack, PlayerEntity player)
	{
		if (itemStack.getItem() instanceof DynamicFood) {
			DynamicFood food = (DynamicFood)itemStack.getItem();
			int hunger = food.getDynamicHunger(itemStack, player);
			float saturationModifier = food.getDynamicSaturation(itemStack, player);
			return new BasicFoodValues(hunger, saturationModifier);
		}
		return getDefaultFoodValues(itemStack);
	}
}
