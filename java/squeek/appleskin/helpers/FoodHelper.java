package squeek.appleskin.helpers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import squeek.appleskin.api.food.FoodValues;

public class FoodHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.getItem().getFood() != null;
	}

	public static FoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		Food itemFood = itemStack.getItem().getFood();
		int hunger = itemFood != null ? itemFood.getHealing() : 0;
		float saturationModifier = itemFood != null ? itemFood.getSaturation() : 0;

		return new FoodValues(hunger, saturationModifier);
	}

	public static FoodValues getModifiedFoodValues(ItemStack itemStack, PlayerEntity player)
	{
		// Previously, this would use AppleCore to get the modified values, but since AppleCore doesn't
		// exist on this MC version and https://github.com/MinecraftForge/MinecraftForge/pull/7266
		// hasn't been merged, we just return the defaults here.
		return getDefaultFoodValues(itemStack);
	}

	public static boolean isRotten(ItemStack itemStack)
    {
		if (!isFood(itemStack))
			return false;

		for (Pair<EffectInstance, Float> effect : itemStack.getItem().getFood().getEffects()) {
			if (effect.getFirst() != null && effect.getFirst().getPotion() != null && effect.getFirst().getPotion().getEffectType() == EffectType.HARMFUL) {
				return true;
			}
		}
		return false;
	}
}
