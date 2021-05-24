package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.food.IFood;
import squeek.appleskin.api.handler.EventHandler;

public class HUDOverlayEvent
{
    public static class Pre extends HUDOverlayEvent
    {
        public Pre(int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
        }

        public static Event<EventHandler<Pre>> EVENT = EventHandler.createArrayBacked();
    }

    public static class Exhaustion extends HUDOverlayEvent
    {
        public Exhaustion(float exhaustion, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.exhaustion = exhaustion;
        }

        public float exhaustion;

        public static Event<EventHandler<Exhaustion>> EVENT = EventHandler.createArrayBacked();
    }

    public static class Saturation extends HUDOverlayEvent
    {
        public Saturation(float saturationLevel, int x, int y, MatrixStack matrixStack)
        {
            super(x, y, matrixStack);
            this.saturationLevel = saturationLevel;
        }

        public float saturationLevel;

        public static Event<EventHandler<Saturation>> EVENT = EventHandler.createArrayBacked();
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

        public static Event<EventHandler<Hunger>> EVENT = EventHandler.createArrayBacked();
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
