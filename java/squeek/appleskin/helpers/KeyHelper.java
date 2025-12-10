package squeek.appleskin.helpers;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyHelper
{
	public static boolean isCtrlKeyDown()
	{
		var window = Minecraft.getInstance().getWindow();
		// prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
		boolean isCtrlKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
		if (!isCtrlKeyDown && Util.getPlatform() == Util.OS.OSX)
			isCtrlKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER);

		return isCtrlKeyDown;
	}

	public static boolean isShiftKeyDown()
	{
		var window = Minecraft.getInstance().getWindow();
		return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}