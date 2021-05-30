package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.api.handler.EventHandler;

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
public class FoodValuesEvent
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

	public static Event<EventHandler<FoodValuesEvent>> EVENT = EventHandler.createArrayBacked();
}
