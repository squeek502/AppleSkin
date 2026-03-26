package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import squeek.appleskin.api.handler.EventHandler;

public class HUDOverlayEvent
{
	/**
	 * If canceled, will stop all rendering of the exhaustion meter.
	 */
	public static class Exhaustion extends HUDOverlayEvent
	{
		public Exhaustion(float exhaustion, int x, int y, GuiGraphicsExtractor graphics)
		{
			super(x, y, graphics);
			this.exhaustion = exhaustion;
		}

		public final float exhaustion;

		public static final Event<EventHandler<Exhaustion>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If canceled, will stop all rendering of the saturation overlay.
	 */
	public static class Saturation extends HUDOverlayEvent
	{
		public Saturation(float saturationLevel, int x, int y, GuiGraphicsExtractor graphics)
		{
			super(x, y, graphics);
			this.saturationLevel = saturationLevel;
		}

		public final float saturationLevel;

		public static final Event<EventHandler<Saturation>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If canceled, will stop all rendering of the hunger restored overlay.
	 */
	public static class HungerRestored extends HUDOverlayEvent
	{
		public HungerRestored(int foodLevel, ItemStack itemStack, FoodProperties foodComponent, int x, int y, GuiGraphicsExtractor graphics)
		{
			super(x, y, graphics);
			this.currentFoodLevel = foodLevel;
			this.itemStack = itemStack;
			this.foodComponent = foodComponent;
		}

		public final FoodProperties foodComponent;
		public final ItemStack itemStack;
		public final int currentFoodLevel;

		public static final Event<EventHandler<HungerRestored>> EVENT = EventHandler.createArrayBacked();
	}

	/**
	 * If canceled, will stop all rendering of the estimated health overlay.
	 */
	public static class HealthRestored extends HUDOverlayEvent
	{
		public HealthRestored(float modifiedHealth, ItemStack itemStack, FoodProperties foodComponent, int x, int y, GuiGraphicsExtractor graphics)
		{
			super(x, y, graphics);
			this.modifiedHealth = modifiedHealth;
			this.itemStack = itemStack;
			this.foodComponent = foodComponent;
		}

		public final FoodProperties foodComponent;
		public final ItemStack itemStack;
		public final float modifiedHealth;

		public static final Event<EventHandler<HealthRestored>> EVENT = EventHandler.createArrayBacked();
	}

	private HUDOverlayEvent(int x, int y, GuiGraphicsExtractor graphics)
	{
		this.x = x;
		this.y = y;
		this.graphics = graphics;
	}

	public final int x;
	public final int y;
	public final GuiGraphicsExtractor graphics;
	public boolean isCanceled = false;
}
