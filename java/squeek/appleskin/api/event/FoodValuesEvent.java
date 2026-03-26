package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

/**
 * Can be used to customize the displayed hunger/saturation values of foods.
 * Called whenever the food values of items are being determined.
 */
public record FoodValuesEvent(
	Player player,
	ItemStack itemStack,
	FoodProperties defaultFoodComponent,
	FoodProperties modifiedFoodComponent) {
	public static final Event<EventHandler<FoodValuesEvent>> EVENT = EventHandler.createArrayBacked();
}
