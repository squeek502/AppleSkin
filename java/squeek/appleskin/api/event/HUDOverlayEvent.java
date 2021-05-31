package squeek.appleskin.api.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import squeek.appleskin.api.food.FoodValues;

@Cancelable
public class HUDOverlayEvent extends Event
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

	@Override
	public boolean isCancelable()
	{
		return true;
	}
}
