package squeek.appleskin.helpers;

import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;

public record ConsumableFood(FoodComponent food, ConsumableComponent consumable)
{
}
