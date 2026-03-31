package squeek.appleskin.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.helpers.ExhaustionHelper;
import squeek.appleskin.helpers.FoodHelper;

import java.text.DecimalFormat;

public class DebugInfoHudEntry implements DebugScreenEntry
{
	public static final Identifier ENTRY_ID = Identifier.fromNamespaceAndPath("appleskin", "food_stats");
	public static final Identifier SECTION_ID = Identifier.fromNamespaceAndPath("appleskin", "debug_info");

	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	@Override
	public void display(
		DebugScreenDisplayer lines,
		@Nullable Level world,
		@Nullable LevelChunk clientChunk,
		@Nullable LevelChunk chunk
	)
	{
		if (world != null)
		{
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.player == null)
				return;

			FoodData stats = mc.player.getFoodData();
			if (stats == null)
			{
				return;
			}

			float curExhaustion = ExhaustionHelper.getExhaustion(mc.player);
			float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
			lines.addToGroup(SECTION_ID, "hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
		}
	}
}
