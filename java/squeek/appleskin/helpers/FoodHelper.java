package squeek.appleskin.helpers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;

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
		return itemStack.getItem().getFood() != null;
	}

	public static BasicFoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		Food itemFood = itemStack.getItem().getFood();
		int hunger = itemFood != null ? itemFood.getHealing() : 0;
		float saturationModifier = itemFood != null ? itemFood.getSaturation() : 0;

		return new BasicFoodValues(hunger, saturationModifier);
	}

	public static BasicFoodValues getModifiedFoodValues(ItemStack itemStack, PlayerEntity player)
	{
		return getDefaultFoodValues(itemStack);
	}

	public static boolean isRotten(ItemStack itemStack) {
		if(!isFood(itemStack))
			return false;

		for (Pair<EffectInstance, Float> effect : itemStack.getItem().getFood().getEffects()) {
			if (effect.getFirst() != null && effect.getFirst().getPotion() != null && effect.getFirst().getPotion().getEffectType() == EffectType.HARMFUL) {
				return true;
			}
		}
		return false;
	}
}
