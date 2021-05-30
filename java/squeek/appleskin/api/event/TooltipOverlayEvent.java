package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.api.handler.EventHandler;

public class TooltipOverlayEvent
{
    /**
     * If cancelled, will stop all rendering from happening.
     */
    public static class Pre extends TooltipOverlayEvent
    {
        public Pre(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood)
        {
            super(itemStack, defaultFood, modifiedFood);
        }

        public static Event<EventHandler<Pre>> EVENT = EventHandler.createArrayBacked();
    }

    /**
     * If cancelled, will reserve space for the food values, but will not
     * render them.
     */
    public static class Render extends TooltipOverlayEvent
    {
        public Render(ItemStack itemStack, int x, int y, MatrixStack matrixStack, FoodValues defaultFood, FoodValues modifiedFood)
        {
            super(itemStack, defaultFood, modifiedFood);
            this.matrixStack = matrixStack;
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;
        public MatrixStack matrixStack;

        public static Event<EventHandler<Render>> EVENT = EventHandler.createArrayBacked();
    }

    private TooltipOverlayEvent(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood)
    {
        this.itemStack = itemStack;
        this.defaultFood = defaultFood;
        this.modifiedFood = modifiedFood;
    }

    public final FoodValues defaultFood;
    public final FoodValues modifiedFood;

    public final ItemStack itemStack;

    public boolean isCanceled = false;
}
