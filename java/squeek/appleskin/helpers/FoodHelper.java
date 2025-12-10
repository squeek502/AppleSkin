package squeek.appleskin.helpers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.network.ClientSyncHandler;

public class FoodHelper
{
	public static boolean isFood(ItemStack itemStack)
	{
		return itemStack.contains(DataComponentTypes.FOOD) && itemStack.contains(DataComponentTypes.CONSUMABLE);
	}

	public static boolean canConsume(PlayerEntity player, FoodComponent foodComponent)
	{
		return player.canConsume(foodComponent.canAlwaysEat());
	}

	public static FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();
	public static ConsumableComponent DEFAULT_CONSUMABLE_COMPONENT = ConsumableComponents.FOOD;

	/**
	 * Assumes itemStack is known to be a food, always returns a non-null ConsumableFood
	 */
	public static ConsumableFood getDefaultFoodValues(ItemStack itemStack)
	{
		return new ConsumableFood(
			itemStack.getOrDefault(DataComponentTypes.FOOD, EMPTY_FOOD_COMPONENT),
			itemStack.getOrDefault(DataComponentTypes.CONSUMABLE, DEFAULT_CONSUMABLE_COMPONENT)
		);
	}

	public static class QueriedFoodResult
	{
		public FoodComponent defaultFoodComponent;
		public FoodComponent modifiedFoodComponent;
		public ConsumableComponent consumableComponent;

		public final ItemStack itemStack;

		public QueriedFoodResult(FoodComponent defaultFoodComponent, FoodComponent modifiedFoodComponent, ConsumableComponent consumableComponent, ItemStack itemStack)
		{
			this.defaultFoodComponent = defaultFoodComponent;
			this.modifiedFoodComponent = modifiedFoodComponent;
			this.consumableComponent = consumableComponent;
			this.itemStack = itemStack;
		}
	}

	@Nullable
	public static QueriedFoodResult query(ItemStack itemStack, PlayerEntity player)
	{
		if (!isFood(itemStack)) return null;

		ConsumableFood defaultFood = FoodHelper.getDefaultFoodValues(itemStack);

		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, itemStack, defaultFood.food(), defaultFood.food());
		FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent);

		return new QueriedFoodResult(foodValuesEvent.defaultFoodComponent, foodValuesEvent.modifiedFoodComponent, defaultFood.consumable(), itemStack);
	}

	public static boolean isRotten(ConsumableComponent consumableComponent)
	{
		for (var effect : consumableComponent.onConsumeEffects())
		{
			if (!(effect instanceof ApplyEffectsConsumeEffect)) continue;

			for (var statusEffect : ((ApplyEffectsConsumeEffect) effect).effects())
			{
				if (statusEffect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL)
					return true;
			}
		}
		return false;
	}

	public static float getEstimatedHealthIncrement(PlayerEntity player, ConsumableFood consumableFood)
	{
		if (!player.canFoodHeal())
			return 0;

		HungerManager stats = player.getHungerManager();

		int foodLevel = Math.min(stats.getFoodLevel() + consumableFood.food().nutrition(), 20);
		float healthIncrement = 0;

		// health for natural regen
		if (foodLevel >= 18.0F && ClientSyncHandler.naturalRegeneration)
		{
			float saturationLevel = Math.min(stats.getSaturationLevel() + consumableFood.food().saturation(), (float) foodLevel);
			float exhaustionLevel = ExhaustionHelper.getExhaustion(player);
			healthIncrement = getEstimatedHealthIncrement(foodLevel, saturationLevel, exhaustionLevel);
		}

		// health for regeneration effect
		for (var effect : consumableFood.consumable().onConsumeEffects())
		{
			if (!(effect instanceof ApplyEffectsConsumeEffect)) continue;

			for (var statusEffect : ((ApplyEffectsConsumeEffect) effect).effects())
			{
				if (statusEffect.getEffectType() == StatusEffects.REGENERATION)
				{
					int amplifier = statusEffect.getAmplifier();
					int duration = statusEffect.getDuration();

					// Refer: https://minecraft.fandom.com/wiki/Regeneration
					// Refer: net.minecraft.entity.effect.StatusEffect.canApplyUpdateEffect
					healthIncrement += (float) Math.floor(duration / Math.max(50 >> amplifier, 1));
					break;
				}
			}
		}

		return healthIncrement;
	}

	public static float REGEN_EXHAUSTION_INCREMENT = 6.0F;
	public static float MAX_EXHAUSTION = 4.0F;

	public static float getEstimatedHealthIncrement(int foodLevel, float saturationLevel, float exhaustionLevel)
	{
		float health = 0;

		if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel))
			return 0;

		while (foodLevel >= 18)
		{
			while (exhaustionLevel > MAX_EXHAUSTION)
			{
				exhaustionLevel -= MAX_EXHAUSTION;
				if (saturationLevel > 0)
					saturationLevel = Math.max(saturationLevel - 1, 0);
				else
					foodLevel -= 1;
			}
			// Without this Float.compare, it's possible for this function to get stuck in an infinite loop
			// if saturationLevel is small enough that exhaustionLevel does not actually change representation
			// when it's incremented. This Float.compare makes it so we treat such close-to-zero values as zero.
			if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0)
			{
				// fast regen health
				//
				// Because only health and exhaustionLevel increase in this branch,
				// we know that we will enter this branch again and again on each iteration
				// if exhaustionLevel is not incremented above MAX_EXHAUSTION before the
				// next iteration.
				//
				// So, instead of actually performing those iterations, we can calculate
				// the number of iterations it would take to reach max exhaustion, and
				// add all the health/exhaustion in one go. In practice, this takes the
				// worst-case number of iterations performed in this function from the millions
				// all the way down to around 18.
				//
				// Note: Due to how floating point works, the results of actually doing the
				// iterations and 'simulating' them using multiplication will differ. That is, small increments
				// in a loop can end up with a different (and higher) final result than multiplication
				// due to floating point rounding. In degenerate cases, the difference can be fairly high
				// (when testing, I found a case that had a difference of ~0.3), but this isn't a concern in
				// this particular instance because the 'real' difference as seen by the player
				// would likely take hundreds of thousands of ticks to materialize (since the
				// `limitedSaturationLevel / REGEN_EXHAUSTION_INCREMENT` value must be very
				// small for a difference to occur at all, and therefore numIterationsUntilAboveMax would
				// be very large).
				float limitedSaturationLevel = Math.min(saturationLevel, REGEN_EXHAUSTION_INCREMENT);
				float exhaustionUntilAboveMax = Math.nextUp(MAX_EXHAUSTION) - exhaustionLevel;
				int numIterationsUntilAboveMax = Math.max(1, (int) Math.ceil(exhaustionUntilAboveMax / limitedSaturationLevel));

				health += (limitedSaturationLevel / REGEN_EXHAUSTION_INCREMENT) * numIterationsUntilAboveMax;
				exhaustionLevel += limitedSaturationLevel * numIterationsUntilAboveMax;
			}
			else if (foodLevel >= 18)
			{
				// slow regen health
				health += 1;
				exhaustionLevel += REGEN_EXHAUSTION_INCREMENT;
			}
		}

		return health;
	}
}
