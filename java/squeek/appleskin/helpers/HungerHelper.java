package squeek.appleskin.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.FoodStats;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

public class HungerHelper
{
	protected static final Field foodExhaustion;

	static
	{
		try
		{
			foodExhaustion = FoodStats.class.getDeclaredField(ObfuscationReflectionHelper.remapName("field_75126_c"));
			foodExhaustion.setAccessible(true);
		}
		catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static float getMaxExhaustion(EntityPlayer player)
	{
		return 4.0f;
	}

	public static float getExhaustion(EntityPlayer player)
	{
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
