package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import squeek.appleskin.helpers.FoodHelper;

/*
 * TEST SCAFFOLDING - not a real feature yet.
 *
 * Predicts the saturation cost of a Lunge jab purely from client-local state (no server
 * sync involved), so it can be rendered side-by-side against the real, server-synced
 * saturation bar to see how well client-side prediction tracks reality before ever
 * trying this on a server that doesn't run AppleSkin. Remove once validated.
 */
public class LungePredictionHandler
{
	public static LungePredictionHandler INSTANCE;

	// -1 so the first tick always treats the real value as a fresh sync
	private float lastKnownRealSaturation = -1;
	private float predictedSaturation = 0;

	public static void init()
	{
		INSTANCE = new LungePredictionHandler();
		ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.onClientTick());
	}

	// called from MinecraftClientMixin right as a piercing weapon (spear) jab is dispatched,
	// whether or not it hits anything
	public void onSwingAttempt()
	{
		PlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null)
			return;

		World world = player.getEntityWorld();
		int level = getLungeLevel(player.getMainHandStack(), world);
		if (level <= 0)
			return;

		// mirrors the requirements on the vanilla `lunge` enchantment definition
		if (player.hasVehicle() || player.isGliding() || player.isTouchingWater())
			return;

		HungerManager hunger = player.getHungerManager();
		if (hunger.getFoodLevel() + predictedSaturation < 7)
			return;

		// mirrors the `minecraft:apply_exhaustion` effect on Lunge: 4 exhaustion per level,
		// and every 4 exhaustion immediately consumes 1 saturation point.
		// Subtracts from our own tracked shadow value, not the (possibly stale) real value,
		// so repeated lunges accumulate instead of each resetting from a stale baseline.
		float predicted = predictedSaturation;
		for (int i = 0; i < level; i++)
			predicted = Math.max(0, predicted - 1);

		predictedSaturation = predicted;
	}

	// called from ItemStackMixin right as an item finishes being consumed. Vanilla applies the
	// actual FoodComponent restore server-only (ConsumableComponent#finishConsumption gates it
	// behind !world.isClient()), so without this, eating never credits the shadow value at all.
	public void onFinishedEating(ItemStack stack, World world, PlayerEntity player)
	{
		if (!world.isClient())
			return;

		FoodHelper.QueriedFoodResult result = FoodHelper.query(stack, player);
		if (result == null)
			return;

		// mirrors HungerManager#addInternal: food level is clamped first, then saturation is
		// clamped against the *new* food level, not the old one
		int predictedFoodLevel = MathHelper.clamp(player.getHungerManager().getFoodLevel() + result.modifiedFoodComponent.nutrition(), 0, 20);
		predictedSaturation = MathHelper.clamp(predictedSaturation + result.modifiedFoodComponent.saturation(), 0, (float) predictedFoodLevel);
	}

	private static int getLungeLevel(ItemStack stack, World world)
	{
		Registry<Enchantment> enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
		RegistryEntry<Enchantment> lunge = enchantments.getEntry(enchantments.get(Enchantments.LUNGE));
		return EnchantmentHelper.getLevel(lunge, stack);
	}

	private void onClientTick()
	{
		PlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null)
			return;

		// only resync when the real value actually moves (e.g. a vanilla health-update packet
		// arrived) - otherwise keep the shadow value, since "real" may just be stale
		float real = player.getHungerManager().getSaturationLevel();
		if (real != lastKnownRealSaturation)
		{
			predictedSaturation = real;
			lastKnownRealSaturation = real;
		}
	}

	public float getDisplayedSaturation()
	{
		return predictedSaturation;
	}
}
