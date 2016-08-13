package squeek.appleskin.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;

public class GuiAppleCoreConfig extends GuiConfig {
    public GuiAppleCoreConfig(GuiScreen parentScreen) {
        super(parentScreen, new ConfigElement(ModConfig.config.getCategory(ModConfig.CATEGORY_CLIENT)).getChildElements(), ModInfo.MODID, false, false, GuiConfig.getAbridgedConfigPath(ModConfig.config.toString()));
    }
}