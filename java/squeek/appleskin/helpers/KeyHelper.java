package squeek.appleskin.helpers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.InputMappings;
import org.lwjgl.glfw.GLFW;

public class KeyHelper
{
	public static boolean isCtrlKeyDown()
	{
		// prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
		boolean isCtrlKeyDown = InputMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
		if (!isCtrlKeyDown && Minecraft.IS_RUNNING_ON_MAC)
			isCtrlKeyDown = InputMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_SUPER) || InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SUPER);

		return isCtrlKeyDown;
	}

	public static boolean isShiftKeyDown()
	{
		return InputMappings.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || InputMappings.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}