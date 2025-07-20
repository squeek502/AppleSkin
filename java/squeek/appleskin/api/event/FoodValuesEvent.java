package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
public final class FoodValuesEvent
{
	public FoodValuesEvent(PlayerEntity player, ItemStack itemStack, FoodComponent defaultFoodValues, FoodComponent modifiedFoodComponent)
	{
		this.player = player;
		this.itemStack = itemStack;
		this.defaultFoodComponent = defaultFoodValues;
		this.modifiedFoodComponent = modifiedFoodComponent;
	}

	public FoodComponent defaultFoodComponent;
	public FoodComponent modifiedFoodComponent;
	public final ItemStack itemStack;
	public final PlayerEntity player;

	public static final Event<EventHandler<FoodValuesEvent>> EVENT = EventHandler.createArrayBacked();
}
