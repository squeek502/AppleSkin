package squeek.appleskin.helpers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class KeyHelper
{
	private KeyHelper() {
		throw new UnsupportedOperationException();
	}

	public static boolean isShiftKeyDown()
	{
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
}