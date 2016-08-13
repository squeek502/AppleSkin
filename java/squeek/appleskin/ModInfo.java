package squeek.appleskin;

import java.util.Locale;

public final class ModInfo
{
	public static final String MODID = "appleskin";
	public static final String VERSION = "${version}";
	public static final String MODID_LOWER = ModInfo.MODID.toLowerCase(Locale.ROOT);
	public static final String GUI_FACTORY_CLASS = "squeek.appleskin.client.gui.GuiFactory";
}