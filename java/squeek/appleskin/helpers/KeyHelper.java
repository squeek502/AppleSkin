package squeek.appleskin.helpers;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class KeyHelper
{
	public static boolean isCtrlKeyDown()
	{
		// prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
		boolean isCtrlKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
		if (!isCtrlKeyDown && Minecraft.IS_RUNNING_ON_MAC)
			isCtrlKeyDown = Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);

		return isCtrlKeyDown;
	}

	public static boolean isShiftKeyDown()
	{
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}
}