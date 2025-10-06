package squeek.appleskin.client;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.*;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.ModConfig;
import squeek.appleskin.helpers.ExhaustionHelper;
import squeek.appleskin.helpers.FoodHelper;

import java.text.DecimalFormat;

public class DebugInfoHudEntry implements DebugHudEntry
{
	public static final Identifier ENTRY_ID = Identifier.of("appleskin", "food_stats");
	public static final Identifier SECTION_ID = Identifier.of("appleskin", "debug_info");

	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	@Override
	public void render(
			DebugHudLines lines,
			@Nullable World world,
			@Nullable WorldChunk clientChunk,
			@Nullable WorldChunk chunk
	) {
		if (world != null) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc == null || mc.player == null)
				return;

			HungerManager stats = mc.player.getHungerManager();
			if (stats == null) {
				return;
			}

			float curExhaustion = ExhaustionHelper.getExhaustion(mc.player);
			float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
			lines.addLineToSection(SECTION_ID, "hunger: " + stats.getFoodLevel() + ", sat: " + saturationDF.format(stats.getSaturationLevel()) + ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + exhaustionMaxDF.format(maxExhaustion));
		}
	}
}
