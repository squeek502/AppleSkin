package squeek.appleskin.api.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import squeek.appleskin.api.food.FoodValues;

public class HUDOverlayEvent extends Event implements ICancellableEvent
{
	/**
	 * If cancelled, will stop all rendering of the exhaustion meter.
	 */
	public static class Exhaustion extends HUDOverlayEvent
	{
		public Exhaustion(float exhaustion, int x, int y, GuiGraphics guiGraphics)
		{
			super(x, y, guiGraphics);
			this.exhaustion = exhaustion;
		}

		public final float exhaustion;
	}

	/**
	 * If cancelled, will stop all rendering of the saturation overlay.
	 */
	public static class Saturation extends HUDOverlayEvent
	{
		public Saturation(float saturationLevel, int x, int y, GuiGraphics guiGraphics)
		{
			super(x, y, guiGraphics);
			this.saturationLevel = saturationLevel;
		}

		public final float saturationLevel;
	}

	/**
	 * If cancelled, will stop all rendering of the hunger restored overlay.
	 */
	public static class HungerRestored extends HUDOverlayEvent
	{
		public HungerRestored(int foodLevel, ItemStack itemStack, FoodValues foodValues, int x, int y, GuiGraphics guiGraphics)
		{
			super(x, y, guiGraphics);
			this.currentFoodLevel = foodLevel;
			this.itemStack = itemStack;
			this.foodValues = foodValues;
		}

		public final FoodValues foodValues;
		public final ItemStack itemStack;
		public final int currentFoodLevel;
	}

	/**
	 * If cancelled, will stop all rendering of the estimated health overlay.
	 */
	public static class HealthRestored extends HUDOverlayEvent
	{
		public HealthRestored(float modifiedHealth, ItemStack itemStack, FoodValues foodValues, int x, int y, GuiGraphics guiGraphics)
		{
			super(x, y, guiGraphics);
			this.modifiedHealth = modifiedHealth;
			this.itemStack = itemStack;
			this.foodValues = foodValues;
		}

		public final FoodValues foodValues;
		public final ItemStack itemStack;
		public final float modifiedHealth;
	}

	private HUDOverlayEvent(int x, int y, GuiGraphics guiGraphics)
	{
		this.x = x;
		this.y = y;
		this.guiGraphics = guiGraphics;
	}

	public int x;
	public int y;
	public GuiGraphics guiGraphics;
}
