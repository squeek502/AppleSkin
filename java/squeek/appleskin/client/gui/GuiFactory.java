package squeek.appleskin.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.DefaultGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;

@SideOnly(Side.CLIENT)
public class GuiFactory extends DefaultGuiFactory
{
	public GuiFactory()
	{
		super(ModInfo.MODID, GuiConfig.getAbridgedConfigPath(ModConfig.config.toString()));
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen)
	{
		return new GuiConfig(parentScreen, new ConfigElement(ModConfig.config.getCategory(ModConfig.CATEGORY_CLIENT)).getChildElements(), ModInfo.MODID, false, false, GuiConfig.getAbridgedConfigPath(ModConfig.config.toString()));
	}
}