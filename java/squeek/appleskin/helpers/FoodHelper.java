package squeek.appleskin.helpers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import squeek.appleskin.api.food.FoodValues;

public class FoodHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.getItem().getFoodProperties() != null;
	}

	public static boolean canConsume(ItemStack itemStack, Player player)
	{
		// item is not a food that can be consume
		if (!isFood(itemStack))
			return false;

		FoodProperties itemFood = itemStack.getItem().getFoodProperties();
		if (itemFood == null)
			return false;

		return player.canEat(itemFood.canAlwaysEat());
	}

	public static FoodValues getDefaultFoodValues(ItemStack itemStack)
	{
		FoodProperties itemFood = itemStack.getItem().getFoodProperties();
		int hunger = itemFood != null ? itemFood.getNutrition() : 0;
		float saturationModifier = itemFood != null ? itemFood.getSaturationModifier() : 0;

		return new FoodValues(hunger, saturationModifier);
	}

	public static FoodValues getModifiedFoodValues(ItemStack itemStack, Player player)
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

		for (Pair<MobEffectInstance, Float> effect : itemStack.getItem().getFoodProperties().getEffects())
		{
			if (effect.getFirst() != null && effect.getFirst().getEffect() != null && effect.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL)
			{
				return true;
			}
		}
		return false;
	}


	public static float getEstimatedHealthIncrement(ItemStack itemStack, FoodValues modifiedFoodValues, Player player)
	{
		if (!isFood(itemStack))
			return 0;

		if (!player.isHurt())
			return 0;

		FoodData stats = player.getFoodData();
		Level world = player.getCommandSenderWorld();

		int foodLevel = Math.min(stats.getFoodLevel() + modifiedFoodValues.hunger, 20);
		float healthIncrement = 0;

		// health for natural regen
		if (foodLevel >= 18.0F && world != null && world.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION))
		{
			float saturationLevel = Math.min(stats.getSaturationLevel() + modifiedFoodValues.getSaturationIncrement(), (float) foodLevel);
			float exhaustionLevel = stats.getExhaustionLevel();
			healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel);
		}

		// health for regeneration effect
		for (Pair<MobEffectInstance, Float> effect : itemStack.getItem().getFoodProperties().getEffects())
		{
			MobEffectInstance effectInstance = effect.getFirst();
			if (effectInstance != null && effectInstance.getEffect() == MobEffects.REGENERATION)
			{
				int amplifier = effectInstance.getAmplifier();
				int duration = effectInstance.getDuration();

				// Refer: https://minecraft.fandom.com/wiki/Regeneration
				// Refer: net.minecraft.world.effect.MobEffect.isDurationEffectTick
				healthIncrement += (float) Math.floor(duration / Math.max(50 >> amplifier, 1));
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

		if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(exhaustionLevel))
			return 0;

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
