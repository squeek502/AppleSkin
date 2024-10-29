package squeek.appleskin.helpers;

import net.minecraft.util.math.MathHelper;

public class ColorHelper
{
	public static int argbFromRGBA(float r, float g, float b, float a)
	{
		return (MathHelper.floor(a * 255.0) << 24) |
			(MathHelper.floor(r * 255.0) << 16) |
			(MathHelper.floor(g * 255.0) << 8) |
			MathHelper.floor(b * 255.0);
	}
}
