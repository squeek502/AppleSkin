package squeek.appleskin.mixin;

import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.hud.debug.DebugHudEntryVisibility;
import net.minecraft.client.gui.hud.debug.DebugProfileType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.DebugInfoHudEntry;

import java.util.HashMap;
import java.util.Map;

@Mixin(DebugHudEntries.class)
public abstract class DebugHudEntriesMixin {

    @Shadow
    @Final
    @Mutable
    public static Map<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> PROFILES;


    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void injectAppleSkinDebugHudEntry(CallbackInfo ci) {
        DebugHudEntries.register(DebugInfoHudEntry.ENTRY_ID, new DebugInfoHudEntry());
        final Map<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> profiles = new HashMap<>();
        for (Map.Entry<DebugProfileType, Map<Identifier, DebugHudEntryVisibility>> entry : PROFILES.entrySet()) {
            final Map<Identifier, DebugHudEntryVisibility> entries = new HashMap<>(entry.getValue());
            if (entry.getKey() == DebugProfileType.DEFAULT) {
                entries.put(DebugInfoHudEntry.ENTRY_ID, DebugHudEntryVisibility.IN_F3);
            }
            profiles.put(entry.getKey(), entries);
        }
        PROFILES = profiles;
    }
}