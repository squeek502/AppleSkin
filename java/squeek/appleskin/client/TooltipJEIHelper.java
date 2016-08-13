package squeek.appleskin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Mouse;
import squeek.appleskin.AppleSkin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class TooltipJEIHelper
{
	public static int toolTipOffsetX;
	public static int toolTipOffsetY;

	private static Method jeiGetRuntime = null;
	private static Class<?> JeiRuntime = null;
	private static Method jeiGetItemListOverlay = null;
	private static Method jeiGetRecipesGui = null;
	private static Class<?> RecipesGui = null;
	private static Method jeiGetFocusUnderMouse = null;
	private static Method jeiRecipesGetFocusUnderMouse = null;
	private static Field jeiFocus_value = null;
	private static Field jeiRecipeLayouts = null;
	private static Field jeiRecipesGui_guiLeft = null;
	private static Field jeiRecipesGui_guiTop = null;
	private static Field jeiRecipeLayoutPosX = null;
	private static Field jeiRecipeLayoutPosY = null;
	private static Method jeiRecipeLayout_getFocusUnderMouse = null;
	private static boolean jeiLoaded = false;

	static
	{
		try
		{
			jeiLoaded = Loader.isModLoaded("JEI");
			if (jeiLoaded)
			{
				Class<?> jeiInternal = Class.forName("mezz.jei.Internal");
				jeiGetRuntime = jeiInternal.getDeclaredMethod("getRuntime");
				JeiRuntime = Class.forName("mezz.jei.JeiRuntime");
				jeiGetItemListOverlay = JeiRuntime.getDeclaredMethod("getItemListOverlay");
				jeiGetRecipesGui = JeiRuntime.getDeclaredMethod("getRecipesGui");

				Class<?> ItemListOverlay = Class.forName("mezz.jei.gui.ItemListOverlay");
				jeiGetFocusUnderMouse = ItemListOverlay.getDeclaredMethod("getFocusUnderMouse", int.class, int.class);

				RecipesGui = Class.forName("mezz.jei.gui.RecipesGui");
				jeiRecipesGetFocusUnderMouse = RecipesGui.getDeclaredMethod("getFocusUnderMouse", int.class, int.class);

				jeiRecipeLayouts = RecipesGui.getDeclaredField("recipeLayouts");
				jeiRecipeLayouts.setAccessible(true);
				jeiRecipesGui_guiLeft = RecipesGui.getDeclaredField("guiLeft");
				jeiRecipesGui_guiLeft.setAccessible(true);
				jeiRecipesGui_guiTop = RecipesGui.getDeclaredField("guiTop");
				jeiRecipesGui_guiTop.setAccessible(true);
				Class<?> RecipeLayout = Class.forName("mezz.jei.gui.RecipeLayout");
				jeiRecipeLayoutPosX = RecipeLayout.getDeclaredField("posX");
				jeiRecipeLayoutPosX.setAccessible(true);
				jeiRecipeLayoutPosY = RecipeLayout.getDeclaredField("posY");
				jeiRecipeLayoutPosY.setAccessible(true);
				jeiRecipeLayout_getFocusUnderMouse = RecipeLayout.getDeclaredMethod("getFocusUnderMouse", int.class, int.class);

				Class<?> Focus = Class.forName("mezz.jei.gui.Focus");
				jeiFocus_value = Focus.getDeclaredField("value");
				jeiFocus_value.setAccessible(true);
			}
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			AppleSkin.Log.error("Unable to integrate the food values tooltip overlay with JEI: ");
			e.printStackTrace();
		}
	}

	public static ItemStack getHoveredStack()
	{
		toolTipOffsetX = 0;
		toolTipOffsetY = 0;

		Minecraft mc = Minecraft.getMinecraft();
		GuiScreen curScreen = mc.currentScreen;

		ScaledResolution scale = new ScaledResolution(mc);

		boolean isJEIRecipesGui = RecipesGui != null && RecipesGui.isInstance(curScreen);

		int mouseX = Mouse.getX() * scale.getScaledWidth() / mc.displayWidth;
		int mouseY = scale.getScaledHeight() - Mouse.getY() * scale.getScaledHeight() / mc.displayHeight;
		ItemStack hoveredStack = null;

		// get the hovered stack from the active container
		try
		{
			// try JEI recipe handler
			if (jeiFocus_value != null)
			{
				Object jeiRuntime = jeiGetRuntime.invoke(null);

				// try to get the hovered stack from the current recipe if possible
				Object recipesGui = isJEIRecipesGui ? curScreen : jeiGetRecipesGui.invoke(jeiRuntime);
				if (isJEIRecipesGui)
				{
					Object recipesFocus = jeiRecipesGetFocusUnderMouse.invoke(curScreen, mouseX, mouseY);
					if (recipesFocus != null && jeiFocus_value.get(recipesFocus) instanceof ItemStack)
						hoveredStack = (ItemStack) jeiFocus_value.get(recipesFocus);
				}

				// next try to get the hovered stack from the right-hand item list
				if (hoveredStack == null)
				{
					Object itemList = jeiGetItemListOverlay.invoke(jeiRuntime);
					Object listFocus = jeiGetFocusUnderMouse.invoke(itemList, mouseX, mouseY);
					if (listFocus != null && jeiFocus_value.get(listFocus) instanceof ItemStack)
						hoveredStack = (ItemStack) jeiFocus_value.get(listFocus);
				}
				else
				{
					// ::gross code alert::
					// when the hoveredStack is in the RecipesGui,
					// tooltips are drawn using a translated Gl matrix, so
					// we need to turn the relative x/y coords back into absolute ones
					// unfortunately, we have to recalculate which RecipeLayout is hovered
					// in order to do this
					Object hoveredLayout = null;
					int guiLeft = jeiRecipesGui_guiLeft.getInt(recipesGui);
					int guiTop = jeiRecipesGui_guiTop.getInt(recipesGui);
					int recipeMouseX = mouseX - guiLeft;
					int recipeMouseY = mouseY - guiTop;
					List<Object> recipeLayouts = (List<Object>) jeiRecipeLayouts.get(recipesGui);
					for (Object recipeLayout : recipeLayouts)
					{
						if (jeiRecipeLayout_getFocusUnderMouse.invoke(recipeLayout, recipeMouseX, recipeMouseY) != null)
						{
							hoveredLayout = recipeLayout;
							break;
						}
					}

					if (hoveredLayout != null)
					{
						toolTipOffsetX = jeiRecipeLayoutPosX.getInt(hoveredLayout) + guiLeft;
						toolTipOffsetY = jeiRecipeLayoutPosY.getInt(hoveredLayout) + guiTop;
					}
				}
			}
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return hoveredStack;
	}
}