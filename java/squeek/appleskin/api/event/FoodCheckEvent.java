package squeek.appleskin.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Can be used to treat non-standard items as food for display purposes
 * or if you have a food component specific to your mod.
 * Called whenever an item is checked if it should show food tooltips.
 * <p>
 * Note: if the item lacks default food components, make sure to also
 * listen to {@link FoodValuesEvent}, otherwise the tooltip will be empty.
 */
public class FoodCheckEvent extends Event
{
    public FoodCheckEvent(Player player, ItemStack itemStack, boolean isFood)
    {
        this.isFood = isFood;
        this.player = player;
        this.itemStack = itemStack;
    }

    public boolean isFood;
    public final ItemStack itemStack;
    public final Player player;
}
