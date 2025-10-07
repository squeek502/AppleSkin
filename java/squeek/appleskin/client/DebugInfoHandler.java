package squeek.appleskin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.helpers.HungerHelper;

import java.text.DecimalFormat;

public class DebugInfoHandler
{
	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	public static final ResourceLocation SECTION_ID = ResourceLocation.fromNamespaceAndPath("appleskin", "debug_info");

	public static void init()
	{
		DebugScreenEntries.register(FoodStatsDebugEntry.ENTRY_ID, new FoodStatsDebugEntry());
	}

	public static class FoodStatsDebugEntry implements DebugScreenEntry
	{
		public static final ResourceLocation ENTRY_ID = ResourceLocation.fromNamespaceAndPath("appleskin", "food_stats");

		@Override
		public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk)
		{
			if (level != null)
			{
				Minecraft mc = Minecraft.getInstance();
				if (mc.player == null)
				{
					return;
				}

				FoodData stats = mc.player.getFoodData();
				float curExhaustion = stats.exhaustionLevel;
				float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
				displayer.addToGroup(SECTION_ID, "hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
			}
		}
	}
}
