package squeek.appleskin.helpers;

import net.minecraft.world.entity.player.Player;

public class ExhaustionHelper
{
	public interface ExhaustionManipulator
	{
		float getSaturationLevel();

		void setSaturation(float exhaustion);
	}

	public static float getSaturationLevel(Player player)
	{
		return ((ExhaustionManipulator) player.getFoodData()).getSaturationLevel();
	}

	public static void setSaturation(Player player, float exhaustion)
	{
		((ExhaustionManipulator) player.getFoodData()).setSaturation(exhaustion);
	}
}
