package squeek.appleskin.helpers;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public class KeyHelper
{
	public static boolean isCtrlKeyDown()
	{
		Window window = Minecraft.getInstance().getWindow();
		// prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
		boolean isCtrlKeyDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
		if (!isCtrlKeyDown && Util.getPlatform() == Util.OS.OSX)
			isCtrlKeyDown = InputConstants.isKeyDown(InputConstants.KEY_LGUI) || InputConstants.isKeyDown(InputConstants.KEY_RGUI);

		return isCtrlKeyDown;
	}

	public static boolean isShiftKeyDown()
	{
		Window window = Minecraft.getInstance().getWindow();
		return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
	}
}