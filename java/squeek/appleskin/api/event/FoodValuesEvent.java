package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
public class FoodValuesEvent
{
	public FoodValuesEvent(Player player, ItemStack itemStack, FoodProperties defaultFoodValues, FoodProperties modifiedFoodComponent)
	{
		this.player = player;
		this.itemStack = itemStack;
		this.defaultFoodComponent = defaultFoodValues;
		this.modifiedFoodComponent = modifiedFoodComponent;
	}

	public FoodProperties defaultFoodComponent;
	public FoodProperties modifiedFoodComponent;
	public final ItemStack itemStack;
	public final Player player;

	public static Event<EventHandler<FoodValuesEvent>> EVENT = EventHandler.createArrayBacked();
}
