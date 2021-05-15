package squeek.appleskin.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import squeek.appleskin.api.food.IFood;
import squeek.appleskin.api.handler.EventHandler;

public class TooltipOverlayEvent
{
    public static class Pre extends TooltipOverlayEvent
    {
        public Pre(ItemStack itemStack, IFood defaultFood, IFood modifiedFood)
        {
            super(itemStack, defaultFood, modifiedFood);
        }

        public static Event<EventHandler<Pre>> EVENT = EventHandler.createArrayBacked();
    }

    public static class Post extends TooltipOverlayEvent
    {
        public Post(ItemStack itemStack, int x, int y, MatrixStack matrixStack, IFood defaultFood, IFood modifiedFood)
        {
            super(itemStack, defaultFood, modifiedFood);
            this.matrixStack = matrixStack;
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;
        public MatrixStack matrixStack;

        public static Event<EventHandler<Post>> EVENT = EventHandler.createArrayBacked();
    }

    private TooltipOverlayEvent(ItemStack itemStack, IFood defaultFood, IFood modifiedFood)
    {
        this.itemStack = itemStack;
        this.defaultFood = defaultFood;
        this.modifiedFood = modifiedFood;
    }

    public IFood defaultFood;
    public IFood modifiedFood;

    public ItemStack itemStack;

    public boolean isCanceled = false;
}
