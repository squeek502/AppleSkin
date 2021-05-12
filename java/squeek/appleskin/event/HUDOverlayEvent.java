package squeek.appleskin.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import squeek.appleskin.helpers.FoodHelper;


public class HUDOverlayEvent
{
    public static class Exhaustion extends HUDOverlayEvent
    {
        public Exhaustion(float exhaustion, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.exhaustion = exhaustion;
        }

        public float exhaustion;
    }

    public static class Saturation extends HUDOverlayEvent
    {
        public Saturation(float saturationLevel, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.saturationLevel = saturationLevel;
        }

        public float saturationLevel;
    }

    public static class Hunger extends HUDOverlayEvent
    {
        public Hunger(int foodLevel, ItemStack itemStack, FoodHelper.BasicFoodValues modifiedFoodValues, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.foodLevel = foodLevel;
            this.itemStack = itemStack;
            this.modifiedFoodValues = modifiedFoodValues;
        }

        public FoodHelper.BasicFoodValues modifiedFoodValues;
        public ItemStack itemStack;
        public int foodLevel;
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
