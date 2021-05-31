package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.util.math.MatrixStack;
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
		public Exhaustion(float exhaustion, int x, int y, MatrixStack matrixStack)
		{
			super(x, y, matrixStack);
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
		public Saturation(float saturationLevel, int x, int y, MatrixStack matrixStack)
		{
			super(x, y, matrixStack);
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
		public HungerRestored(int foodLevel, ItemStack itemStack, FoodValues foodValues, int x, int y, MatrixStack matrixStack)
		{
			super(x, y, matrixStack);
			this.currentFoodLevel = foodLevel;
			this.itemStack = itemStack;
			this.foodValues = foodValues;
		}

		public final FoodValues foodValues;
		public final ItemStack itemStack;
		public final int currentFoodLevel;

		public static Event<EventHandler<HungerRestored>> EVENT = EventHandler.createArrayBacked();
	}

	private HUDOverlayEvent(int x, int y, MatrixStack matrixStack)
	{
		this.x = x;
		this.y = y;
		this.matrixStack = matrixStack;
	}

	public int x;
	public int y;
	public MatrixStack matrixStack;
	public boolean isCanceled = false;
}
