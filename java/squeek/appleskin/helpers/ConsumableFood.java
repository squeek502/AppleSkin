package squeek.appleskin.helpers;

import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.food.FoodProperties;

public record ConsumableFood(FoodProperties food, Consumable consumable) { }