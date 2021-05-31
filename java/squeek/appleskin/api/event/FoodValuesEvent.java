package squeek.appleskin.api.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import squeek.appleskin.api.food.FoodValues;

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
public class FoodValuesEvent extends Event
{
	public FoodValuesEvent(PlayerEntity player, ItemStack itemStack, FoodValues defaultFoodValues, FoodValues modifiedFoodValues)
	{
		this.player = player;
		this.itemStack = itemStack;
		this.defaultFoodValues = defaultFoodValues;
		this.modifiedFoodValues = modifiedFoodValues;
	}

	public FoodValues defaultFoodValues;
	public FoodValues modifiedFoodValues;
	public final ItemStack itemStack;
	public final PlayerEntity player;
}
