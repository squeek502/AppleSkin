package squeek.appleskin.api.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import squeek.appleskin.api.food.FoodValues;

@Cancelable
public class TooltipOverlayEvent extends Event
{
	/**
	 * If cancelled, will stop all rendering from happening.
	 */
	public static class Pre extends TooltipOverlayEvent
	{
		public Pre(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
		}
	}

	/**
	 * If cancelled, will reserve space for the food values, but will not
	 * render them.
	 */
	public static class Render extends TooltipOverlayEvent
	{
		public Render(ItemStack itemStack, int x, int y, MatrixStack matrixStack, FoodValues defaultFood, FoodValues modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
			this.matrixStack = matrixStack;
			this.x = x;
			this.y = y;
		}

		public int x;
		public int y;
		public MatrixStack matrixStack;
	}

	private TooltipOverlayEvent(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood)
	{
		this.itemStack = itemStack;
		this.defaultFood = defaultFood;
		this.modifiedFood = modifiedFood;
	}

	public final FoodValues defaultFood;
	public final FoodValues modifiedFood;

	public final ItemStack itemStack;

	@Override
	public boolean isCancelable()
	{
		return true;
	}
}
