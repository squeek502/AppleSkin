package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.api.handler.EventHandler;

public class HUDOverlayEvent
{
	/**
	 * If cancelled, will stop all rendering of the exhaustion meter.
	 */
	public static class Exhaustion extends HUDOverlayEvent
	{
		public Exhaustion(float exhaustion, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.exhaustion = exhaustion;
		}

		public final float exhaustion;

		public static Event<EventHandler<Exhaustion>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the saturation overlay.
	 */
	public static class Saturation extends HUDOverlayEvent
	{
		public Saturation(float saturationLevel, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.saturationLevel = saturationLevel;
		}

		public final float saturationLevel;

		public static Event<EventHandler<Saturation>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the hunger restored overlay.
	 */
	public static class HungerRestored extends HUDOverlayEvent
	{
		public HungerRestored(int foodLevel, ItemStack itemStack, FoodValues foodValues, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.currentFoodLevel = foodLevel;
			this.itemStack = itemStack;
			this.foodValues = foodValues;
		}

		public final FoodValues foodValues;
		public final ItemStack itemStack;
		public final int currentFoodLevel;

		public static Event<EventHandler<HungerRestored>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the estimated health overlay.
	 */
	public static class HealthRestored extends HUDOverlayEvent
	{
		public HealthRestored(float modifiedHealth, ItemStack itemStack, FoodValues foodValues, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.modifiedHealth = modifiedHealth;
			this.itemStack = itemStack;
			this.foodValues = foodValues;
		}

		public final FoodValues foodValues;
		public final ItemStack itemStack;
		public final float modifiedHealth;

		public static Event<EventHandler<HealthRestored>> EVENT = EventHandler.createArrayBacked();
	}

	private HUDOverlayEvent(int x, int y, DrawContext context)
	{
		this.x = x;
		this.y = y;
		this.context = context;
	}

	public int x;
	public int y;
	public DrawContext context;
	public boolean isCanceled = false;
}
