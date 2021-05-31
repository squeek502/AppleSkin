package squeek.appleskin.helpers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * An interface for foods whose hunger/saturation restoration can vary by the ItemStack.
 * <p>
 * Note: squeek.appleskin.api.event.FoodValuesEvent can be used for the
 * same purpose. This interface remains mostly for backwards-compatibility.
 */
public interface DynamicFood
{
	/**
	 * @param stack  the stack to check
	 * @param player the player to check
	 * @return the amount of hunger restored by the stack
	 */
	int getDynamicHunger(ItemStack stack, PlayerEntity player);

	/**
	 * @param stack  the stack to check
	 * @param player the player to check
	 * @return the saturation modifier of the stack
	 */
	float getDynamicSaturation(ItemStack stack, PlayerEntity player);
}
