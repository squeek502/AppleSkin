<img src="http://www.ryanliptak.com/images/appleskin.png" width="32" /> [AppleSkin](http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/2222837-applecore)
===========

Minecraft mod that adds various food-related HUD improvements formerly provided by [AppleCore](https://github.com/squeek502/AppleCore) (basically, AppleCore without the core).

### Features

* Adds food value information to tooltips: 

![](http://i.imgur.com/furoAAi.png)
* Adds a visualization of saturation and exhaustion to the HUD 

![](http://zippy.gfycat.com/ShimmeringYearlyCicada.gif)
* Adds a visualization of potential hunger/saturation restored while holding food

![](http://zippy.gfycat.com/PowerfulDeafeningHarvestmen.gif)
* Adds hunger/saturation/exhaustion info to the debug overlay (F3)
* Syncs the value of saturation and exhaustion to the client

---

### Building AppleSkin
1. Clone the repository
2. Open a command line and execute ```gradlew build```

Note: To give the build a version number, use ```gradlew build -Pversion=<version>``` instead (example: ```gradlew build -Pversion=1.0.0```)
