package squeek.appleskin.api.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import squeek.appleskin.api.food.IFood;

@Cancelable
public class HUDOverlayEvent extends Event
{

    public static class Pre extends HUDOverlayEvent
    {
        public Pre(int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
        }
    }

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
        public Hunger(int foodLevel, ItemStack itemStack, IFood modifiedFood, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.foodLevel = foodLevel;
            this.itemStack = itemStack;
            this.modifiedFood = modifiedFood;
        }

        public IFood modifiedFood;
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

    @Override
    public boolean isCancelable() {
        return true;
    }
}
