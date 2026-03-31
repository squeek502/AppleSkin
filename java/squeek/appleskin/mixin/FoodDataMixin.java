package squeek.appleskin.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import squeek.appleskin.helpers.ExhaustionHelper;

@Mixin(FoodData.class)
public class FoodDataMixin implements ExhaustionHelper.ExhaustionManipulator
{
	@Shadow
	private float exhaustionLevel;

	@Override
	public void setExhaustion(float value)
	{
		this.exhaustionLevel = value;
	}

	@Override
	public float getExhaustion()
	{
		return this.exhaustionLevel;
	}
}
