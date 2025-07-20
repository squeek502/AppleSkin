package squeek.appleskin.helpers;

import net.minecraft.util.Identifier;
import squeek.appleskin.AppleSkin;

public final class TextureHelper
{
	private TextureHelper() {
		throw new UnsupportedOperationException();
	}

	public static final Identifier MOD_ICONS = Identifier.of(AppleSkin.MOD_ID, "textures/icons.png");
	public static final Identifier HUNGER_OUTLINE_SPRITE = Identifier.of(AppleSkin.MOD_ID, "tooltip_hunger_outline");

	// Hunger
	public static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
	public static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
	public static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
	public static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
	public static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
	public static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");

	public enum FoodType
	{
		EMPTY,
		HALF,
		FULL,
	}

	public static Identifier getFoodTexture(boolean isRotten, FoodType type)
	{
		return switch (type)
		{
			case EMPTY -> isRotten ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
			case HALF -> isRotten ? FOOD_HALF_HUNGER_TEXTURE : FOOD_HALF_TEXTURE;
			case FULL -> isRotten ? FOOD_FULL_HUNGER_TEXTURE : FOOD_FULL_TEXTURE;
		};
	}

	// Hearts
	public static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
	public static final Identifier HEART_HARDCORE_CONTAINER = Identifier.ofVanilla("hud/heart/container_hardcore");
	public static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
	public static final Identifier HEART_HARDCORE_FULL = Identifier.ofVanilla("hud/heart/hardcore_full");
	public static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
	public static final Identifier HEART_HARDCORE_HALF = Identifier.ofVanilla("hud/heart/hardcore_half");

	public enum HeartType
	{
		CONTAINER,
		FULL,
		HALF,
	}

	public static Identifier getHeartTexture(boolean hardcore, HeartType type)
	{
		return switch (type)
		{
			case CONTAINER -> hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
			case FULL -> hardcore ? HEART_HARDCORE_FULL : HEART_FULL;
			case HALF -> hardcore ? HEART_HARDCORE_HALF : HEART_HALF;
		};
	}
}
