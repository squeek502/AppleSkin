package squeek.appleskin.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.FoodStats;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import squeek.appleskin.AppleSkin;

import java.lang.reflect.Field;

public class HungerHelper
{
	static final Field foodExhaustion = ReflectionHelper.findField(FoodStats.class, "foodExhaustionLevel", "field_75126_c", "c");

	public static float getMaxExhaustion(EntityPlayer player)
	{
		if (AppleSkin.hasAppleCore)
			return AppleCoreHelper.getMaxExhaustion(player);

		return 4.0f;
	}

	public static float getExhaustion(EntityPlayer player)
	{
		if (AppleSkin.hasAppleCore)
			return AppleCoreHelper.getExhaustion(player);

		try
		{
			return foodExhaustion.getFloat(player.getFoodStats());
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void setExhaustion(EntityPlayer player, float exhaustion)
	{
		if (AppleSkin.hasAppleCore)
		{
			AppleCoreHelper.setExhaustion(player, exhaustion);
			return;
		}

		try
		{
			foodExhaustion.setFloat(player.getFoodStats(), exhaustion);
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
}
