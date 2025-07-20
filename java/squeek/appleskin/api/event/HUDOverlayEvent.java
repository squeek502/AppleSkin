package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

public class HUDOverlayEvent
{
	/**
	 * If cancelled, will stop all rendering of the exhaustion meter.
	 */
	public static final class Exhaustion extends HUDOverlayEvent
	{
		public Exhaustion(float exhaustion, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.exhaustion = exhaustion;
		}

		public final float exhaustion;

		public static final Event<EventHandler<Exhaustion>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the saturation overlay.
	 */
	public static final class Saturation extends HUDOverlayEvent
	{
		public Saturation(float saturationLevel, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.saturationLevel = saturationLevel;
		}

		public final float saturationLevel;

		public static final Event<EventHandler<Saturation>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the hunger restored overlay.
	 */
	public static final class HungerRestored extends HUDOverlayEvent
	{
		public HungerRestored(int foodLevel, ItemStack itemStack, FoodComponent foodComponent, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.currentFoodLevel = foodLevel;
			this.itemStack = itemStack;
			this.foodComponent = foodComponent;
		}

		public final FoodComponent foodComponent;
		public final ItemStack itemStack;
		public final int currentFoodLevel;

		public static final Event<EventHandler<HungerRestored>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If cancelled, will stop all rendering of the estimated health overlay.
	 */
	public static final class HealthRestored extends HUDOverlayEvent
	{
		public HealthRestored(float modifiedHealth, ItemStack itemStack, FoodComponent foodComponent, int x, int y, DrawContext context)
		{
			super(x, y, context);
			this.modifiedHealth = modifiedHealth;
			this.itemStack = itemStack;
			this.foodComponent = foodComponent;
		}

		public final FoodComponent foodComponent;
		public final ItemStack itemStack;
		public final float modifiedHealth;

		public static final Event<EventHandler<HealthRestored>> EVENT = EventHandler.createArrayBacked();
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
	public boolean isCanceled;
}
