package squeek.appleskin.helpers;

import net.minecraft.entity.player.PlayerEntity;

public class HungerHelper
{
	public interface ExhaustionManipulator
	{
		float getExhaustion();

		void setExhaustion(float exhaustion);
	}

	public static float getMaxExhaustion(PlayerEntity player)
	{
		return 4.0f;
	}

	public static float getExhaustion(PlayerEntity player)
	{
		return ((ExhaustionManipulator) player.getHungerManager()).getExhaustion();
	}

	public static void setExhaustion(PlayerEntity player, float exhaustion)
	{
		((ExhaustionManipulator) player.getHungerManager()).setExhaustion(exhaustion);
	}
}
