package squeek.appleskin.helpers;

import net.minecraft.world.entity.player.Player;

public class ExhaustionHelper
{
	public interface ExhaustionManipulator
	{
		float getExhaustion();

		void setExhaustion(float exhaustion);
	}

	public static float getExhaustion(Player player)
	{
		return ((ExhaustionManipulator) player.getFoodData()).getExhaustion();
	}

	public static void setExhaustion(Player player, float exhaustion)
	{
		((ExhaustionManipulator) player.getFoodData()).setExhaustion(exhaustion);
	}
}
