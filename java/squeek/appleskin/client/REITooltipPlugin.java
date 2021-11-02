package squeek.appleskin.client;

import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;

import java.util.ArrayList;
import java.util.List;

public class REITooltipPlugin implements REIClientPlugin
{
	// Because AppleSkin adds its custom tooltip as an OrderedText, REI treats it
	// as a string and it gets transformed into a plain StyledText before it gets
	// converted into a TooltipComponent, meaning AppleSkin's custom TooltipComponent
	// is bypassed and our custom rendering doesn't happen.
	// We need to do the conversion before that, so we register a
	// tooltip transformer with REI, find our FoodOverlayTextComponent and
	// convert it into AppleSkin's custom TooltipComponent implementation and add *that*
	// directly to REI's tooltip, so that it doesn't get converted by REI afterwards.
	//
	// This has the potential to mess with the ordering of the AppleSkin tooltip between
	// vanilla tooltips and REI tooltips, but that seems fine.

	@Override
	public void registerEntryRenderers(EntryRendererRegistry registry)
	{
		registry.transformTooltip(VanillaEntryTypes.ITEM, (itemstack, mouse, tooltip) -> {
			if (tooltip == null)
				return null;

			List<Tooltip.Entry> foodComponents = new ArrayList<Tooltip.Entry>();
			for (final Tooltip.Entry entry : tooltip.entries())
			{
				if (entry.isText() && entry.getAsText() instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
					foodComponents.add(entry);
			}
			for (Tooltip.Entry entry : foodComponents)
			{
				// remove the text version
				tooltip.entries().remove(entry);
				// add the TooltipComponent version
				tooltip.add(((TooltipOverlayHandler.FoodOverlayTextComponent) entry.getAsText()).foodOverlay);
			}
			return tooltip;
		});
	}
}
