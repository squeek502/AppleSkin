package squeek.appleskin.gui;

import com.mrcrayfish.configured.api.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.ModConfig;

import java.util.List;
import java.util.Set;

public class ConfiguredConfigProvider implements IModConfigProvider
{
	private static final ModConfig DEFAULTS = new ModConfig();

	@Override
	public Set<IModConfig> getConfigurationsForMod(ModContext modContext)
	{
		// Prefer cloth-config over configured
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) return Set.of();
		if (!modContext.modId().equals("appleskin")) return Set.of();
		return Set.of(new ConfiguredModConfig());
	}

	protected static class ConfiguredModConfig implements IModConfig
	{
		List<IConfigEntry> options;

		public ConfiguredModConfig()
		{
			this.options = List.of(
				new LeafEntry<Boolean>("showFoodValuesInTooltip"),
				new LeafEntry<Boolean>("showFoodValuesInTooltipAlways"),
				new LeafEntry<Boolean>("showSaturationHudOverlay"),
				new LeafEntry<Boolean>("showFoodValuesHudOverlay"),
				new LeafEntry<Boolean>("showFoodValuesHudOverlayWhenOffhand"),
				new LeafEntry<Boolean>("showFoodExhaustionHudUnderlay"),
				new LeafEntry<Boolean>("showFoodHealthHudOverlay"),
				new LeafEntry<Boolean>("showVanillaAnimationsOverlay"),
				new LeafEntry<String>("maxHudOverlayFlashAlpha")
			);
		}

		@Override
		public ConfigType getType()
		{
			return ConfigType.CLIENT;
		}

		@Override
		public boolean isChanged()
		{
			return true;
		}

		@Override
		public String getFileName()
		{
			return "appleskin.json5";
		}

		@Override
		public String getModId()
		{
			return "appleskin";
		}

		@Override
		public IConfigEntry createRootEntry()
		{
			return new RootEntry(this.options);
		}

		@Override
		public ActionResult update(IConfigEntry entry)
		{
			for (var option : entry.getChildren())
			{
				var configValue = option.getValue();
				assert (configValue != null);
				var value = fromT(configValue.get());
				try
				{
					var field = ModConfig.class.getDeclaredField(configValue.getName());
					field.set(ModConfig.INSTANCE, value);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}
			try
			{
				ModConfig.INSTANCE.save();
				return ActionResult.success();
			}
			catch (Exception e)
			{
				return ActionResult.fail(Component.nullToEmpty(e.getMessage()));
			}
		}

		@Override
		public ActionResult canPlayerEdit(@Nullable Player player)
		{
			return ActionResult.success();
		}

		@Override
		public @Nullable String getTranslationKey()
		{
			return "text.autoconfig.appleskin.title";
		}

		@Override
		public void startEditing()
		{
			for (var option : this.options)
			{
				var value = option.getValue();
				assert (value != null);
				value.cleanCache();
			}
		}
	}

	protected static class RootEntry implements IConfigEntry
	{
		List<IConfigEntry> children;

		public RootEntry(List<IConfigEntry> options)
		{
			this.children = options;
		}

		@Override
		public List<IConfigEntry> getChildren()
		{
			return this.children;
		}

		@Override
		public boolean isRoot()
		{
			return true;
		}

		@Override
		public boolean isLeaf()
		{
			return false;
		}

		@Override
		public @Nullable IConfigValue<?> getValue()
		{
			return null;
		}

		@Override
		public String getEntryName()
		{
			return "";
		}

		@Override
		public @Nullable Component getTooltip()
		{
			return null;
		}

		@Override
		public @Nullable String getTranslationKey()
		{
			return null;
		}
	}

	protected static class LeafEntry<T> implements IConfigEntry
	{
		private final String name;
		private final IConfigValue<T> value;

		protected LeafEntry(String name)
		{
			this.name = name;
			T value;
			try
			{
				value = asT(ModConfig.class.getDeclaredField(this.name).get(ModConfig.INSTANCE));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			T defaultValue;
			try
			{
				defaultValue = asT(ModConfig.class.getDeclaredField(this.name).get(DEFAULTS));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
			this.value = switch (value)
			{
				case String s -> new FloatVal<T>(this.name, value, defaultValue);
				default -> new SimpleValue<T>(this.name, value, defaultValue);
			};
		}

		@Override
		public List<IConfigEntry> getChildren()
		{
			return List.of();
		}

		@Override
		public boolean isRoot()
		{
			return false;
		}

		@Override
		public boolean isLeaf()
		{
			return true;
		}

		@Override
		public @Nullable IConfigValue<T> getValue()
		{
			return this.value;
		}

		@Override
		public String getEntryName()
		{
			return this.name;
		}

		@Override
		public @Nullable Component getTooltip()
		{
			return Component.translatable(this.getTranslationKey() + ".@Tooltip");
		}

		@Override
		public @Nullable String getTranslationKey()
		{
			return "text.autoconfig.appleskin.option." + this.name;
		}
	}

	protected static class SimpleValue<T> implements IConfigValue<T>
	{
		private final String name;
		private T value;
		private T initValue;
		private final T defaultValue;

		protected SimpleValue(String name, T initValue, T defaultValue)
		{
			this.name = name;
			this.value = initValue;
			this.initValue = initValue;
			this.defaultValue = defaultValue;
		}

		@Override
		public T get()
		{
			return this.value;
		}

		@Override
		public T getDefault()
		{
			return this.defaultValue;
		}

		@Override
		public void set(T value)
		{
			this.value = value;
		}

		@Override
		public boolean isValid(T value)
		{
			return true;
		}

		@Override
		public boolean isDefault()
		{
			return this.value.equals(this.defaultValue);
		}

		@Override
		public boolean isChanged()
		{
			return !this.value.equals(this.initValue);
		}

		@Override
		public void restore()
		{
			this.value = this.defaultValue;
		}

		@Override
		public @Nullable Component getComment()
		{
			return Component.translatable(this.getTranslationKey() + ".@Tooltip");
		}

		@Override
		public @Nullable String getTranslationKey()
		{
			return "text.autoconfig.appleskin.option." + this.name;
		}

		@Override
		public @Nullable Component getValidationHint()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return this.name;
		}

		@Override
		public void cleanCache()
		{
			try
			{
				this.value = asT(ModConfig.class.getDeclaredField(this.name).get(ModConfig.INSTANCE));
				this.initValue = this.value;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean requiresWorldRestart()
		{
			return false;
		}

		@Override
		public boolean requiresGameRestart()
		{
			return false;
		}
	}

	protected static class FloatVal<T> extends SimpleValue<T>
	{
		protected FloatVal(String name, T initValue, T defaultValue)
		{
			super(name, initValue, defaultValue);
		}

		@Override
		public boolean isValid(T value)
		{
			try
			{
				var floatVal = Float.parseFloat((String) value);
				return floatVal >= 0.0f && floatVal <= 1.0f;
			}
			catch (Exception e)
			{
				return false;
			}
		}

		@Override
		public @Nullable Component getValidationHint()
		{
			return Component.translatable("configured.validator.range_hint", "0.0", "1.0");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T asT(Object val)
	{
		if (val instanceof Float)
		{
			return (T) ((Float) val).toString();
		}
		else
		{
			return (T) val;
		}
	}

	private static <T> Object fromT(T val)
	{
		if (val instanceof String)
		{
			return Float.valueOf((String) val);
		}
		else
		{
			return val;
		}
	}
}
