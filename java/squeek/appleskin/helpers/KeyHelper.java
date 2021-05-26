package squeek.appleskin.helpers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyHelper
{
    public static boolean isCtrlKeyDown()
    {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        // prioritize CONTROL, but allow OPTION as well on Mac (note: GuiScreen's isCtrlKeyDown only checks for the OPTION key on Mac)
        boolean isCtrlKeyDown = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
        if (!isCtrlKeyDown && MinecraftClient.IS_SYSTEM_MAC)
            isCtrlKeyDown = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SUPER) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SUPER);

        return isCtrlKeyDown;
    }

    public static boolean isShiftKeyDown()
    {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}