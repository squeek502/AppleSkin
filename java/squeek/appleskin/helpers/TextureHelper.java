package squeek.appleskin.helpers;

import net.minecraft.resources.ResourceLocation;

public class TextureHelper
{
	public static final ResourceLocation MOD_ICONS = new ResourceLocation("appleskin", "textures/icons.png");
	public static final ResourceLocation HUNGER_OUTLINE_SPRITE = new ResourceLocation("appleskin", "tooltip_hunger_outline");

	// Hunger
	public static final ResourceLocation FOOD_EMPTY_HUNGER_TEXTURE = new ResourceLocation("hud/food_empty_hunger");
	public static final ResourceLocation FOOD_HALF_HUNGER_TEXTURE = new ResourceLocation("hud/food_half_hunger");
	public static final ResourceLocation FOOD_FULL_HUNGER_TEXTURE = new ResourceLocation("hud/food_full_hunger");
	public static final ResourceLocation FOOD_EMPTY_TEXTURE = new ResourceLocation("hud/food_empty");
	public static final ResourceLocation FOOD_HALF_TEXTURE = new ResourceLocation("hud/food_half");
	public static final ResourceLocation FOOD_FULL_TEXTURE = new ResourceLocation("hud/food_full");

	public enum FoodType
	{
		EMPTY,
		HALF,
		FULL,
	}

	public static ResourceLocation getFoodTexture(boolean isRotten, FoodType type)
	{
		return switch (type)
		{
			case EMPTY -> isRotten ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
			case HALF -> isRotten ? FOOD_HALF_HUNGER_TEXTURE : FOOD_HALF_TEXTURE;
			case FULL -> isRotten ? FOOD_FULL_HUNGER_TEXTURE : FOOD_FULL_TEXTURE;
		};
	}

	// Hearts
	public static final ResourceLocation HEART_CONTAINER = new ResourceLocation("hud/heart/container");
	public static final ResourceLocation HEART_HARDCORE_CONTAINER = new ResourceLocation("hud/heart/container_hardcore");
	public static final ResourceLocation HEART_FULL = new ResourceLocation("hud/heart/full");
	public static final ResourceLocation HEART_HARDCORE_FULL = new ResourceLocation("hud/heart/hardcore_full");
	public static final ResourceLocation HEART_HALF = new ResourceLocation("hud/heart/half");
	public static final ResourceLocation HEART_HARDCORE_HALF = new ResourceLocation("hud/heart/hardcore_half");

	public enum HeartType
	{
		CONTAINER,
		FULL,
		HALF,
	}

	public static ResourceLocation getHeartTexture(boolean hardcore, HeartType type)
	{
		return switch (type)
		{
			case CONTAINER -> hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
			case FULL -> hardcore ? HEART_HARDCORE_FULL : HEART_FULL;
			case HALF -> hardcore ? HEART_HARDCORE_HALF : HEART_HALF;
		};
	}
}
