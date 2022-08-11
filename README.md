<img src="https://www.ryanliptak.com/images/appleskin.png" width="32" /> [AppleSkin](https://minecraft.curseforge.com/projects/appleskin)
===========

Minecraft mod that adds various food-related HUD improvements formerly provided by [AppleCore](https://github.com/squeek502/AppleCore) (basically, AppleCore without the core).

### Features

* Adds food value information to tooltips:

![](https://i.imgur.com/YksBaUx.png)

* Adds a visualization of saturation and exhaustion to the HUD:

![](https://i.imgur.com/tmImVqo.gif)

* Adds a visualization of potential hunger/saturation restored while holding food:

![](https://i.imgur.com/aHf1QxQ.gif)

* Adds a visualization of potential health restored while holding food:

![](https://i.imgur.com/jUOKFUl.gif)

* Adds hunger/saturation/exhaustion info to the debug overlay (F3)
* Syncs the value of saturation and exhaustion to the client.

---

### Building AppleSkin
1. Clone the repository
2. Open a command line and execute ```gradlew build```

Note: To give the build a version number, use ```gradlew build -Pversion=<version>``` instead (example: ```gradlew build -Pversion=1.0.0```).

---

### For Mod Developers

> Note: These instructions are Forge-specific. For Fabric, see the instructions in the relevant `-fabric` branch.

If followed, the directions below will make it so that your mod's Maven dependencies won't include AppleSkin at all, and your mod will load fine with or without AppleSkin installed.

To compile against the AppleSkin API, include the following in your `build.gradle`:

```groovy
repositories {
	maven { url "https://maven.ryanliptak.com/" }
}
```

and add this to your `dependencies` block:

```groovy
compileOnly fg.deobf("squeek.appleskin:appleskin-forge:<version>:api")
```

where `<version>` is replaced by the appropriate version found here:

https://maven.ryanliptak.com/squeek/appleskin/appleskin-forge

Once you're compiling against the AppleSkin API, you can create an event handler and only register it when `appleskin` is loaded. Here's an example implementation:

In your `@Mod` annotated class:

```java
private void clientInit(final FMLClientSetupEvent event) {
    if (ModList.get().isLoaded("appleskin")) {
        MinecraftForge.EVENT_BUS.register(new AppleSkinEventHandler());
    }
}
```

and the `AppleSkinEventHandler` class:

```java
public class AppleSkinEventHandler
{
	@SubscribeEvent
	public void onPreTooltipEvent(TooltipOverlayEvent.Pre event) {
		// hide the tooltip for regular apples
		if (event.itemStack.getItem() == Items.APPLE) {
			event.setCanceled(true);
		}
	}
}
```

(see the `squeek.appleskin.api.event` package for all the possible events that can be registered)

---

Note: if you want to test with the full AppleSkin mod in your development environment, you can also add the following to your `dependencies`:

```groovy
runtimeOnly fg.deobf("squeek.appleskin:appleskin-forge:<version>")
```

while replacing `<version>` as mentioned above.