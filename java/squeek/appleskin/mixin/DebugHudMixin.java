package squeek.appleskin.mixin;

import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.DebugInfoHandler;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin
{
	@Inject(at = @At("RETURN"), method = "getLeftText")
	protected void getLeftText(CallbackInfoReturnable<List<String>> info)
	{
		if (DebugInfoHandler.INSTANCE != null)
			DebugInfoHandler.INSTANCE.onTextRender(info.getReturnValue());
	}
}
