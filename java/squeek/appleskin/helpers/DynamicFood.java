package squeek.appleskin.helpers;

import net.minecraft.item.ItemStack;

/**
 * An interface for foods whose hunger/saturation restoration can vary by the ItemStack.
 */
public interface DynamicFood
{
	/**
	 * @param stack the stack to check
	 * @return how much hunger a food will restore *on top of* its base hunger amount
	 */
	int getAdditionalHunger(ItemStack stack);

	/**
	 * @param stack the stack to check
	 * @return how much saturation a food will restore *on top of* its base saturation amount
	 */
	float getAdditionalSaturation(ItemStack stack);
}
