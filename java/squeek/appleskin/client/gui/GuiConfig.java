package squeek.appleskin.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;

public class GuiConfig extends net.minecraftforge.fml.client.config.GuiConfig
{
	public GuiConfig(GuiScreen parentScreen)
	{
		super(parentScreen, new ConfigElement(ModConfig.config.getCategory(ModConfig.CATEGORY_CLIENT)).getChildElements(), ModInfo.MODID, false, false, net.minecraftforge.fml.client.config.GuiConfig.getAbridgedConfigPath(ModConfig.config.toString()));
	}
}