package squeek.appleskin.helpers;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.entity.player.PlayerEntity;
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
			foodExhaustion = FoodStats.class.getDeclaredField(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.FIELD, "field_75126_c"));
			foodExhaustion.setAccessible(true);
		}
		catch (NoSuchFieldException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static float getMaxExhaustion(PlayerEntity player)
	{
		return 4.0f;
	}

	public static float getExhaustion(PlayerEntity player)
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

	public static void setExhaustion(PlayerEntity player, float exhaustion)
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
