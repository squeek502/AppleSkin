package squeek.appleskin.api.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import squeek.appleskin.api.food.IFood;

@Cancelable
public class TooltipOverlayEvent extends Event
{
    public static class Pre extends TooltipOverlayEvent
    {
        public Pre(ItemStack itemStack, IFood food, IFood modifiedFood)
        {
            super(itemStack, food, modifiedFood);
        }
    }

    public static class Post extends TooltipOverlayEvent
    {
        public Post(ItemStack itemStack, int x, int y, MatrixStack matrixStack, IFood food, IFood modifiedFood)
        {
            super(itemStack, food, modifiedFood);
            this.matrixStack = matrixStack;
            this.x = x;
            this.y = y;
        }

        public int x;
        public int y;
        public MatrixStack matrixStack;
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

    @Override
    public boolean isCancelable()
    {
        return true;
    }
}
