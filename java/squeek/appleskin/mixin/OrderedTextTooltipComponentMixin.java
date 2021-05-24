package squeek.appleskin.mixin;

import net.minecraft.client.gui.tooltip.OrderedTextTooltipComponent;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import squeek.appleskin.duck.ITooltipGetOrderedText;

@SuppressWarnings("unused")
@Mixin(OrderedTextTooltipComponent.class)
public class OrderedTextTooltipComponentMixin implements ITooltipGetOrderedText {

    @Shadow
    private OrderedText text;

    public OrderedText getText() {
        return text;
    }
}
