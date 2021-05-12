package squeek.appleskin.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import squeek.appleskin.helpers.FoodHelper;

public class TooltipOverlayEvent
{
    public static class Pre extends TooltipOverlayEvent
    {
        public Pre(ItemStack itemStack, FoodHelper.BasicFoodValues defaultFoodValues, FoodHelper.BasicFoodValues modifiedFoodValues)
        {
            super(itemStack, defaultFoodValues, modifiedFoodValues);
        }
    }

    public static class Post extends TooltipOverlayEvent
    {
        public Post(ItemStack itemStack, int x, int y, MatrixStack matrixStack, FoodHelper.BasicFoodValues defaultFoodValues, FoodHelper.BasicFoodValues modifiedFoodValues)
        {
            super(itemStack, defaultFoodValues, modifiedFoodValues);
            this.matrixStack = matrixStack;
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;
        public MatrixStack matrixStack;
    }

    private TooltipOverlayEvent(ItemStack itemStack, FoodHelper.BasicFoodValues defaultFoodValues, FoodHelper.BasicFoodValues modifiedFoodValues)
    {
        this.itemStack = itemStack;
        this.defaultFoodValues = defaultFoodValues;
        this.modifiedFoodValues = modifiedFoodValues;
    }

    public ItemStack itemStack;
    public FoodHelper.BasicFoodValues defaultFoodValues;
    public FoodHelper.BasicFoodValues modifiedFoodValues;

    public boolean isCanceled = false;
}
