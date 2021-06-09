package squeek.appleskin.helpers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import squeek.appleskin.api.food.FoodValues;

public class FoodHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.getItem().isFood();
	}

	public static FoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		FoodComponent itemFood = itemStack.getItem().getFoodComponent();
		int hunger = itemFood.getHunger();
		float saturationModifier = itemFood.getSaturationModifier();
		return new FoodValues(hunger, saturationModifier);
	}

	public static FoodValues getModifiedFoodValues(ItemStack itemStack, PlayerEntity player)
	{
		if (itemStack.getItem() instanceof DynamicFood)
		{
			DynamicFood food = (DynamicFood) itemStack.getItem();
			int hunger = food.getDynamicHunger(itemStack, player);
			float saturationModifier = food.getDynamicSaturation(itemStack, player);
			return new FoodValues(hunger, saturationModifier);
		}
		return getDefaultFoodValues(itemStack);
	}

	public static boolean isRotten(ItemStack itemStack)
	{
		if (!isFood(itemStack))
			return false;

		for (Pair<StatusEffectInstance, Float> effect : itemStack.getItem().getFoodComponent().getStatusEffects())
		{
			if (effect.getFirst() != null && effect.getFirst().getEffectType() != null && effect.getFirst().getEffectType().getType() == StatusEffectType.HARMFUL)
				return true;
		}
		return false;
	}

	public static float getEstimatedHealthIncrement(ItemStack itemStack, FoodValues modifiedFoodValues, PlayerEntity player)
	{
		if (!isFood(itemStack))
			return 0;

		if (!player.canFoodHeal())
			return 0;

		HungerManager stats = player.getHungerManager();
		World world = player.getEntityWorld();

		int foodLevel = Math.min(stats.getFoodLevel() + modifiedFoodValues.hunger, 20);
		float healthIncrement = 0;

		// health for natural regen
		if (foodLevel >= 18.0F && world != null && world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION))
		{
			float saturationLevel = Math.min(stats.getSaturationLevel() + modifiedFoodValues.getSaturationIncrement(), (float)foodLevel);
			float exhaustionLevel = HungerHelper.getExhaustion(player);
			healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel);
		}

		// health for regeneration effect
		for (Pair<StatusEffectInstance, Float> effect : itemStack.getItem().getFoodComponent().getStatusEffects())
		{
			StatusEffectInstance effectInstance = effect.getFirst();
			if (effectInstance != null && effectInstance.getEffectType() == StatusEffects.REGENERATION)
			{
				int amplifier = effectInstance.getAmplifier();
				int duration = effectInstance.getDuration();

				// when is a permanent status effect, just have to make a big duration
				if (effectInstance.isPermanent())
					duration = Integer.MAX_VALUE;

				healthIncrement += (float)Math.floor(duration / (50 >> amplifier));
				break;
			}
		}

		return healthIncrement;
	}

	public static float getEstimatedHealthIncrement(int foodLevel, float saturationLevel, float exhaustionLevel)
	{
		float health = 0;
		float exhaustionForRegen = 6.0F;
		float exhaustionForConsumed = 4.0F;

		while (foodLevel >= 18)
		{
			while (exhaustionLevel > exhaustionForConsumed)
			{
				exhaustionLevel -= exhaustionForConsumed;
				if (saturationLevel > 0)
					saturationLevel = Math.max(saturationLevel - 1, 0);
				else
					foodLevel -= 1;
			}
			if (foodLevel >= 20 && saturationLevel > 0)
			{
				// fast regen health
				float limitedSaturationLevel = Math.min(saturationLevel, exhaustionForRegen);
				health += limitedSaturationLevel / exhaustionForRegen;
				exhaustionLevel += limitedSaturationLevel;
			}
			else if (foodLevel >= 18)
			{
				// slow regen health
				health += 1;
				exhaustionLevel += exhaustionForRegen;
			}
		}

		return health;
	}
}
