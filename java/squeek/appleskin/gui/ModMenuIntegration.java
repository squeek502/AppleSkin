package squeek.appleskin.gui;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import squeek.appleskin.ModConfig;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi
{
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory()
	{
		return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
	}
}
