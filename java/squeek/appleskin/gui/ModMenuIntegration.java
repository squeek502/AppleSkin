package squeek.appleskin.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi
{
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory()
	{
		if (FabricLoader.getInstance().isModLoaded("cloth-config"))
		{
			return parent -> AutoConfig.getConfigScreen(AutoConfigIntegration.class, parent).get();
		}
		return screen -> null;
	}
}
