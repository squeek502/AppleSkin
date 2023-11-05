package squeek.appleskin.api.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import squeek.appleskin.api.food.FoodValues;

public class TooltipOverlayEvent extends Event implements ICancellableEvent
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
		public Render(ItemStack itemStack, int x, int y, GuiGraphics guiGraphics, FoodValues defaultFood, FoodValues modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
			this.guiGraphics = guiGraphics;
			this.x = x;
			this.y = y;
		}

		public int x;
		public int y;
		public GuiGraphics guiGraphics;
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
}
