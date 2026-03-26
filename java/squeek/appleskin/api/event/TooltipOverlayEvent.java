package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

public class TooltipOverlayEvent
{
	/**
	 * If canceled, will stop all rendering from happening.
	 */
	public static class Pre extends TooltipOverlayEvent
	{
		public Pre(ItemStack itemStack, FoodProperties defaultFood, FoodProperties modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
		}

		public static final Event<EventHandler<Pre>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If canceled, will reserve space for the food values, but will not
	 * render them.
	 */
	public static class Render extends TooltipOverlayEvent
	{
		public Render(ItemStack itemStack, int x, int y, GuiGraphicsExtractor graphics, FoodProperties defaultFood, FoodProperties modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
			this.graphics = graphics;
			this.x = x;
			this.y = y;
		}

		public final int x;
		public final int y;
		public final GuiGraphicsExtractor graphics;

		public static final Event<EventHandler<Render>> EVENT = EventHandler.createArrayBacked();
	}

	private TooltipOverlayEvent(ItemStack itemStack, FoodProperties defaultFood, FoodProperties modifiedFood)
	{
		this.itemStack = itemStack;
		this.defaultFood = defaultFood;
		this.modifiedFood = modifiedFood;
	}

	public final FoodProperties defaultFood;
	public final FoodProperties modifiedFood;

	public final ItemStack itemStack;

	public final boolean isCanceled = false;
}
