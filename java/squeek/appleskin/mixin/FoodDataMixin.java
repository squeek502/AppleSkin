package squeek.appleskin.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import squeek.appleskin.helpers.ExhaustionHelper;

@Mixin(FoodData.class)
public class FoodDataMixin implements ExhaustionHelper.ExhaustionManipulator
{
	@Shadow
	private float exhaustionLevel;

	@Unique
	public void setSaturation(float value)
	{
		this.exhaustionLevel = value;
	}

	@Unique
	public float getSaturationLevel()
	{
		return this.exhaustionLevel;
	}
}
