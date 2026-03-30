package squeek.appleskin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.*;
import net.minecraft.world.food.FoodData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
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
		@NonNull DebugScreenDisplayer displayer,
		@Nullable Level world,
		@Nullable LevelChunk clientChunk,
		@Nullable LevelChunk chunk
	) {
		if (world != null) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null)
				return;

			FoodData stats = mc.player.getFoodData();

			float curExhaustion = ExhaustionHelper.getSaturationLevel(mc.player);
			float maxExhaustion = FoodHelper.MAX_EXHAUSTION;

			displayer.addToGroup(SECTION_ID, "hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
		}
	}
}
