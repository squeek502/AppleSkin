package squeek.appleskin.api.food;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * An interface for foods whose hunger/saturation restoration can vary by the ItemStack.
 */
public interface IFood
{
	/**
	 * @param stack the stack to check
	 * @param player the player to check
	 * @return how much hunger an instance of this food will restore in total, including base restoration.
	 */
	int getHunger(ItemStack stack, PlayerEntity player);

	/**
	 * @param stack the stack to check
	 * @param player the player to check
	 * @return how much saturation an instance of this food will restore in total, including base restoration..
	 */
	float getSaturationIncrement(ItemStack stack, PlayerEntity player);
}
