package squeek.appleskin.helpers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.util.FoodStats;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import squeek.appleskin.api.food.FoodValues;

public class FoodHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.getItem().getFood() != null;
	}

	public static boolean canConsume(ItemStack itemStack, PlayerEntity player)
	{
		// item is not a food that can be consume
		if (!isFood(itemStack))
			return false;

		Food itemFood = itemStack.getItem().getFood();
		if (itemFood == null)
			return false;

		return player.canEat(itemFood.canEatWhenFull());
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

		for (Pair<EffectInstance, Float> effect : itemStack.getItem().getFood().getEffects())
		{
			if (effect.getFirst() != null && effect.getFirst().getPotion() != null && effect.getFirst().getPotion().getEffectType() == EffectType.HARMFUL)
			{
				return true;
			}
		}
		return false;
	}


	public static float getEstimatedHealthIncrement(ItemStack itemStack, FoodValues modifiedFoodValues, PlayerEntity player)
	{
		if (!isFood(itemStack))
			return 0;

		if (!player.shouldHeal())
			return 0;

		FoodStats stats = player.getFoodStats();
		World world = player.getEntityWorld();

		int foodLevel = Math.min(stats.getFoodLevel() + modifiedFoodValues.hunger, 20);
		float healthIncrement = 0;

		// health for natural regen
		if (foodLevel >= 18.0F && world != null && world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION))
		{
			float saturationLevel = Math.min(stats.getSaturationLevel() + modifiedFoodValues.getSaturationIncrement(), (float) foodLevel);
			float exhaustionLevel = HungerHelper.getExhaustion(player);
			healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel);
		}

		// health for regeneration effect
		for (Pair<EffectInstance, Float> effect : itemStack.getItem().getFood().getEffects())
		{
			EffectInstance effectInstance = effect.getFirst();
			if (effectInstance != null && effectInstance.getPotion() == Effects.REGENERATION)
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

		if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel))
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
