package squeek.appleskin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

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

	// how long to hold the predicted snapshot before letting it mirror the real value again
	private static final int GRACE_TICKS = 6;

	private float predictedSaturation = 0;
	private int graceTicksRemaining = 0;

	public static void init()
	{
		INSTANCE = new LungePredictionHandler();
		AttackEntityCallback.EVENT.register(INSTANCE::onAttackEntity);
		ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.onClientTick());
	}

	private ActionResult onAttackEntity(PlayerEntity player, World world, Hand hand, Entity target, EntityHitResult hitResult)
	{
		if (!world.isClient())
			return ActionResult.PASS;

		int level = getLungeLevel(player.getStackInHand(hand), world);
		if (level <= 0)
			return ActionResult.PASS;

		// mirrors the requirements on the vanilla `lunge` enchantment definition
		if (player.hasVehicle() || player.isGliding() || player.isTouchingWater())
			return ActionResult.PASS;

		HungerManager hunger = player.getHungerManager();
		if (hunger.getFoodLevel() + hunger.getSaturationLevel() < 7)
			return ActionResult.PASS;

		// mirrors the `minecraft:apply_exhaustion` effect on Lunge: 4 exhaustion per level,
		// and every 4 exhaustion immediately consumes 1 saturation point
		float predicted = hunger.getSaturationLevel();
		for (int i = 0; i < level; i++)
			predicted = Math.max(0, predicted - 1);

		predictedSaturation = predicted;
		graceTicksRemaining = GRACE_TICKS;

		return ActionResult.PASS;
	}

	private static int getLungeLevel(ItemStack stack, World world)
	{
		Registry<Enchantment> enchantments = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
		RegistryEntry<Enchantment> lunge = enchantments.getEntry(enchantments.get(Enchantments.LUNGE));
		return EnchantmentHelper.getLevel(lunge, stack);
	}

	private void onClientTick()
	{
		if (graceTicksRemaining > 0)
			graceTicksRemaining--;
	}

	// returns the predicted saturation while a prediction is still "fresh", otherwise the real value
	public float getDisplayedSaturation(float realSaturation)
	{
		return graceTicksRemaining > 0 ? predictedSaturation : realSaturation;
	}
}
