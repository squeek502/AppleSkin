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

* Adds hunger/saturation/exhaustion info to the debug overlay (F3)
* Syncs the value of saturation and exhaustion to the client.

---

### Building AppleSkin
1. Clone the repository
2. Open a command line and execute ```gradlew build```

Note: To give the build a version number, use ```gradlew build -Pversion=<version>``` instead (example: ```gradlew build -Pversion=1.0.0```).
