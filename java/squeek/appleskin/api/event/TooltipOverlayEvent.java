package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

public class TooltipOverlayEvent
{
	public boolean isCanceled;

	/**
	 * If cancelled, will stop all rendering from happening.
	 */
	public static final class Pre extends TooltipOverlayEvent
	{
		public Pre(ItemStack itemStack, FoodComponent defaultFood, FoodComponent modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
		}

		public static final Event<EventHandler<Pre>> EVENT = EventHandler.createArrayBacked();
	}

	private TooltipOverlayEvent(ItemStack itemStack, FoodComponent defaultFood, FoodComponent modifiedFood)
	{
		this.itemStack = itemStack;
		this.defaultFood = defaultFood;
		this.modifiedFood = modifiedFood;
	}

	public final FoodComponent defaultFood;
	public final FoodComponent modifiedFood;

	public final ItemStack itemStack;

	/**
	 * If cancelled, will reserve space for the food values, but will not
	 * render them.
	 */
	public static final class Render extends TooltipOverlayEvent
	{
		public Render(ItemStack itemStack, int x, int y, DrawContext context, FoodComponent defaultFood, FoodComponent modifiedFood)
		{
			super(itemStack, defaultFood, modifiedFood);
			this.context = context;
			this.x = x;
			this.y = y;
		}

		public int x;
		public int y;
		public DrawContext context;

		public static final Event<EventHandler<Render>> EVENT = EventHandler.createArrayBacked();
	}
}
