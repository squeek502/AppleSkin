package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;
import squeek.appleskin.helpers.TextureHelper;
import squeek.appleskin.util.IntPoint;

import java.util.Random;
import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class HUDOverlayHandler
{
    private static float unclampedFlashAlpha = 0f;
    private static float flashAlpha = 0f;
    private static byte alphaDir = 1;
    protected static int foodIconsOffset;
    public static final Vector<IntPoint> healthBarOffsets = new Vector<>();
    public static final Vector<IntPoint> foodBarOffsets = new Vector<>();
    private static final Random random = new Random();

    public static void init()
    {
        MinecraftForge.EVENT_BUS.register(new HUDOverlayHandler());
    }

    static ResourceLocation FOOD_LEVEL_ELEMENT = new ResourceLocation("minecraft", "food_level");
    static ResourceLocation PLAYER_HEALTH_ELEMENT = new ResourceLocation("minecraft", "player_health");

    @SubscribeEvent
    public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
    {
        if (event.getOverlay() == GuiOverlayManager.findOverlay(FOOD_LEVEL_ELEMENT))
        {
            Minecraft mc = Minecraft.getInstance();
            ForgeGui gui = (ForgeGui) mc.gui;
            boolean isMounted = mc.player.getVehicle() instanceof LivingEntity;
            if (!isMounted && !mc.options.hideGui && gui.shouldDrawSurvivalElements())
            {
                renderExhaustion(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight());
            }
        }
    }

    @SubscribeEvent
    public void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event)
    {
        if (event.getOverlay() == GuiOverlayManager.findOverlay(FOOD_LEVEL_ELEMENT))
        {
            Minecraft mc = Minecraft.getInstance();
            ForgeGui gui = (ForgeGui) mc.gui;
            boolean isMounted = mc.player.getVehicle() instanceof LivingEntity;
            if (!isMounted && !mc.options.hideGui && gui.shouldDrawSurvivalElements())
            {
                renderFoodOrHealthOverlay(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight(), RenderOverlayType.FOOD);
            }
        }
        else if (event.getOverlay() == GuiOverlayManager.findOverlay(PLAYER_HEALTH_ELEMENT))
        {
            Minecraft mc = Minecraft.getInstance();
            ForgeGui gui = (ForgeGui) mc.gui;
            if (!mc.options.hideGui && gui.shouldDrawSurvivalElements())
            {
                renderFoodOrHealthOverlay(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight(), RenderOverlayType.HEALTH);
            }
        }
    }

    public static void renderExhaustion(ForgeGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight)
    {
        foodIconsOffset = gui.rightHeight;
        if (!ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        assert player != null;
        int right = mc.getWindow().getGuiScaledWidth() / 2 + 91;
        int top = mc.getWindow().getGuiScaledHeight() - foodIconsOffset;
        float exhaustion = player.getFoodData().getExhaustionLevel();
        HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top, guiGraphics);
        MinecraftForge.EVENT_BUS.post(renderEvent);
        if (!renderEvent.isCanceled())
            drawExhaustionOverlay(renderEvent, mc, 1f);
    }

    enum RenderOverlayType
    {
        HEALTH,
        FOOD,
    }

    public static void renderFoodOrHealthOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight, RenderOverlayType type)
    {
        if (!shouldRenderAnyOverlays())
            return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        assert player != null;
        FoodData stats = player.getFoodData();
        int top = mc.getWindow().getGuiScaledHeight() - foodIconsOffset;
        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int right = mc.getWindow().getGuiScaledWidth() / 2 + 91;
        if (type == RenderOverlayType.HEALTH)
            generateHealthBarOffsets(top, left, right, mc.gui.getGuiTicks(), player);
        if (type == RenderOverlayType.FOOD)
            generateHungerBarOffsets(top, left, right, mc.gui.getGuiTicks(), player);
        HUDOverlayEvent.Saturation saturationRenderEvent = null;
        if (type == RenderOverlayType.FOOD)
        {
            saturationRenderEvent = new HUDOverlayEvent.Saturation(stats.getSaturationLevel(), right, top, guiGraphics);
            if (!ModConfig.SHOW_SATURATION_OVERLAY.get())
                saturationRenderEvent.setCanceled(true);
            if (!saturationRenderEvent.isCanceled())
                MinecraftForge.EVENT_BUS.post(saturationRenderEvent);
            if (!saturationRenderEvent.isCanceled())
                drawSaturationOverlay(saturationRenderEvent, mc, 0, 1f);
        }
        ItemStack heldItem = player.getMainHandItem();
        if (ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get() && !FoodHelper.canConsume(heldItem, player))
            heldItem = player.getOffhandItem();
        boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && FoodHelper.canConsume(heldItem, player);
        if (!shouldRenderHeldItemValues)
        {
            resetFlash();
            return;
        }
        FoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(heldItem, player);
        FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, heldItem, FoodHelper.getDefaultFoodValues(heldItem, player), modifiedFoodValues);
        MinecraftForge.EVENT_BUS.post(foodValuesEvent);
        modifiedFoodValues = foodValuesEvent.modifiedFoodValues;
        if (type == RenderOverlayType.HEALTH)
        {
            if (healthBarOffsets.size() == 0)
                return;
            if (!shouldShowEstimatedHealth(heldItem, modifiedFoodValues))
                return;
            float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(heldItem, modifiedFoodValues, player);
            float currentHealth = player.getHealth();
            float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());
            HUDOverlayEvent.HealthRestored healthRenderEvent = null;
            if (currentHealth < modifiedHealth)
                healthRenderEvent = new HUDOverlayEvent.HealthRestored(modifiedHealth, heldItem, modifiedFoodValues, left, top, guiGraphics);
            if (healthRenderEvent != null)
                MinecraftForge.EVENT_BUS.post(healthRenderEvent);
            if (healthRenderEvent != null && !healthRenderEvent.isCanceled())
                drawHealthOverlay(healthRenderEvent, mc, flashAlpha);
        }
        else if (type == RenderOverlayType.FOOD)
        {
            if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get())
                return;
            HUDOverlayEvent.HungerRestored renderRenderEvent = new HUDOverlayEvent.HungerRestored(stats.getFoodLevel(), heldItem, modifiedFoodValues, right, top, guiGraphics);
            MinecraftForge.EVENT_BUS.post(renderRenderEvent);
            if (renderRenderEvent.isCanceled())
                return;
            int foodHunger = modifiedFoodValues.hunger;
            float foodSaturationIncrement = modifiedFoodValues.getSaturationIncrement();
            drawHungerOverlay(renderRenderEvent, mc, foodHunger, flashAlpha, FoodHelper.isRotten(heldItem, player));
            assert saturationRenderEvent != null;
            if (!saturationRenderEvent.isCanceled())
            {
                int newFoodValue = stats.getFoodLevel() + foodHunger;
                float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;
                float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodSaturationIncrement;
                drawSaturationOverlay(saturationRenderEvent, mc, saturationGained, flashAlpha);
            }
        }
    }

    public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha)
    {
        if (saturationLevel + saturationGained < 0) return;
        enableAlpha(alpha);
        float modifiedSaturation = saturationLevel + saturationGained;
        int iconSize = 9;
        int currentLayer = (int)(modifiedSaturation / 20);
        float layerSaturation = modifiedSaturation % 20;
        if (layerSaturation == 0 && modifiedSaturation > 0) layerSaturation = 20;
        int startSaturationBar = 0;
        int endSaturationBar = 10;
        if (saturationGained != 0)
        {
            float currentSaturationInLayer = saturationLevel % 20;
            if (currentSaturationInLayer == 0 && saturationLevel > 0) currentSaturationInLayer = 20;
            startSaturationBar = (int)(currentSaturationInLayer / 2.0F);
        }
        for (int i = startSaturationBar; i < endSaturationBar; ++i)
        {
            IntPoint offset = foodBarOffsets.get(i);
            if (offset == null) continue;
            int x = right + offset.x;
            int y = top + offset.y;
            float effectiveSaturationOfBar = (layerSaturation / 2.0F) - i;
            int u = 0;
            if (effectiveSaturationOfBar >= 1) u = 3 * iconSize;
            else if (effectiveSaturationOfBar >= 0.5) u = 2 * iconSize;
            else if (effectiveSaturationOfBar > 0.25) u = 1 * iconSize;
            int currentColor = getLayerColor(currentLayer, alpha);
            if (currentLayer > 0)
            {
                int previousColor = getLayerColor(currentLayer - 1, alpha);
                RenderSystem.setShaderColor(((previousColor >> 16) & 0xFF) / 255f, ((previousColor >> 8) & 0xFF) / 255f, (previousColor & 0xFF) / 255f, ((previousColor >> 24) & 0xFF) / 255f);
                guiGraphics.blit(TextureHelper.MOD_ICONS, x, y, 3 * iconSize, 0, iconSize, iconSize);
            }
            RenderSystem.setShaderColor(((currentColor >> 16) & 0xFF) / 255f, ((currentColor >> 8) & 0xFF) / 255f, (currentColor & 0xFF) / 255f, ((currentColor >> 24) & 0xFF) / 255f);
            guiGraphics.blit(TextureHelper.MOD_ICONS, x, y, u, 0, iconSize, iconSize);
        }
        RenderSystem.setShaderColor(1f,1f,1f,1f);
        RenderSystem.setShaderTexture(0, TextureHelper.MC_ICONS);
        disableAlpha(alpha);
    }

    private static int hsvToRgb(float h, float s, float v)
    {
        int r,g,b;
        int i = (int)(h*6);
        float f = h*6-i;
        float p = v*(1-s);
        float q = v*(1-f*s);
        float t = v*(1-(1-f)*s);
        switch(i%6){case 0:r=Math.round(255*v);g=Math.round(255*t);b=Math.round(255*p);break;case 1:r=Math.round(255*q);g=Math.round(255*v);b=Math.round(255*p);break;case 2:r=Math.round(255*p);g=Math.round(255*v);b=Math.round(255*t);break;case 3:r=Math.round(255*p);g=Math.round(255*q);b=Math.round(255*v);break;case 4:r=Math.round(255*t);g=Math.round(255*p);b=Math.round(255*v);break;case 5:r=Math.round(255*v);g=Math.round(255*p);b=Math.round(255*q);break;default:r=g=b=0;break;}
        return (r<<16)|(g<<8)|b;
    }

    private static int getLayerColor(int layer, float alpha)
    {
        int alphaValue = (int)(alpha*255)<<24;
        final int COLOR_COUNT=20;
        final float HUE_START=60f/360f;
        final float HUE_END=360f/360f;
        int colorIndex = layer % COLOR_COUNT;
        float hueStep = (HUE_END-HUE_START)/COLOR_COUNT;
        float hue = HUE_START + colorIndex*hueStep;
        int loopCount = layer / COLOR_COUNT;
        float baseValue = 1.0f;
        float valueIncrement = 0.15f*loopCount;
        float value = Math.min(1.0f, baseValue+valueIncrement);
        if(value>=1.0f && loopCount>0){value=0.6f;hue=HUE_START;}
        float saturation=1.0f;
        int rgb=hsvToRgb(hue,saturation,value);
        return alphaValue|rgb;
    }

    public static void drawHungerOverlay(int hungerRestored, int foodLevel, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha, boolean useRottenTextures)
    {
        if(hungerRestored<=0)return;
        enableAlpha(alpha);
        int modifiedFood = Math.max(0,Math.min(20,foodLevel+hungerRestored));
        int startFoodBars = Math.max(0,foodLevel/2);
        int endFoodBars = (int)Math.ceil(modifiedFood/2.0F);
        int iconStartOffset=16;
        int iconSize=9;
        for(int i=startFoodBars;i<endFoodBars;++i)
        {
            IntPoint offset = foodBarOffsets.get(i);
            if(offset==null)continue;
            int x = right+offset.x;
            int y = top+offset.y;
            int v=3*iconSize;
            int u=iconStartOffset+4*iconSize;
            int ub=iconStartOffset+1*iconSize;
            if(useRottenTextures){u+=4*iconSize;ub+=12*iconSize;}
            if(i*2+1==modifiedFood)u+=1*iconSize;
            RenderSystem.setShaderColor(1f,1f,1f,alpha*0.25f);
            guiGraphics.blit(TextureHelper.MC_ICONS,x,y,ub,v,iconSize,iconSize);
            RenderSystem.setShaderColor(1f,1f,1f,alpha);
            guiGraphics.blit(TextureHelper.MC_ICONS,x,y,u,v,iconSize,iconSize);
        }
        disableAlpha(alpha);
    }

    public static void drawHealthOverlay(float health,float modifiedHealth,Minecraft mc,GuiGraphics guiGraphics,int right,int top,float alpha)
    {
        if(modifiedHealth<=health)return;
        enableAlpha(alpha);
        int fixedModifiedHealth=(int)Math.ceil(modifiedHealth);
        boolean isHardcore=mc.player.level()!=null && mc.player.level().getLevelData().isHardcore();
        int startHealthBars=(int)Math.max(0,(Math.ceil(health)/2.0F));
        int endHealthBars=(int)Math.max(0,Math.ceil(modifiedHealth/2.0F));
        int iconStartOffset=16;
        int iconSize=9;
        for(int i=startHealthBars;i<endHealthBars;++i)
        {
            IntPoint offset=healthBarOffsets.get(i);
            if(offset==null)continue;
            int x=right+offset.x;
            int y=top+offset.y;
            int v=0;
            int u=iconStartOffset+4*iconSize;
            int ub=iconStartOffset+1*iconSize;
            if(i*2+1==fixedModifiedHealth)u+=1*iconSize;
            if(isHardcore)v=5*iconSize;
            RenderSystem.setShaderColor(1f,1f,1f,alpha*0.25f);
            guiGraphics.blit(TextureHelper.MC_ICONS,x,y,ub,v,iconSize,iconSize);
            RenderSystem.setShaderColor(1f,1f,1f,alpha);
            guiGraphics.blit(TextureHelper.MC_ICONS,x,y,u,v,iconSize,iconSize);
        }
        disableAlpha(alpha);
    }

    public static void drawExhaustionOverlay(float exhaustion,Minecraft mc,GuiGraphics guiGraphics,int right,int top,float alpha)
    {
        float maxExhaustion=HungerHelper.getMaxExhaustion(mc.player);
        float ratio=Math.min(1,Math.max(0,exhaustion/maxExhaustion));
        int width=(int)(ratio*81);
        int height=9;
        enableAlpha(.75f);
        guiGraphics.blit(TextureHelper.MOD_ICONS,right-width,top,81-width,18,width,height);
        disableAlpha(.75f);
        RenderSystem.setShaderTexture(0,TextureHelper.MC_ICONS);
    }

    public static void enableAlpha(float alpha)
    {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f,1f,1f,alpha);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void disableAlpha(float alpha)
    {
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f,1f,1f,1f);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase!=TickEvent.Phase.END)return;
        unclampedFlashAlpha+=alphaDir*0.125f;
        if(unclampedFlashAlpha>=1.5f)alphaDir=-1;
        else if(unclampedFlashAlpha<=-0.5f)alphaDir=1;
        flashAlpha=Math.max(0f,Math.min(1f,unclampedFlashAlpha))*Math.max(0f,Math.min(1f,ModConfig.MAX_HUD_OVERLAY_FLASH_ALPHA.get().floatValue()));
    }

    public static void resetFlash()
    {
        unclampedFlashAlpha=flashAlpha=0f;
        alphaDir=1;
    }

    private static void drawSaturationOverlay(HUDOverlayEvent.Saturation event,Minecraft mc,float saturationGained,float alpha)
    {
        drawSaturationOverlay(saturationGained,event.saturationLevel,mc,event.guiGraphics,event.x,event.y,alpha);
    }

    private static void drawHungerOverlay(HUDOverlayEvent.HungerRestored event,Minecraft mc,int hunger,float alpha,boolean useRottenTextures)
    {
        drawHungerOverlay(hunger,event.currentFoodLevel,mc,event.guiGraphics,event.x,event.y,alpha,useRottenTextures);
    }

    private static void drawHealthOverlay(HUDOverlayEvent.HealthRestored event,Minecraft mc,float alpha)
    {
        drawHealthOverlay(mc.player.getHealth(),event.modifiedHealth,mc,event.guiGraphics,event.x,event.y,alpha);
    }

    private static void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event,Minecraft mc,float alpha)
    {
        drawExhaustionOverlay(event.exhaustion,mc,event.guiGraphics,event.x,event.y,alpha);
    }

    private static boolean shouldRenderAnyOverlays()
    {
        return ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() || ModConfig.SHOW_SATURATION_OVERLAY.get() || ModConfig.SHOW_FOOD_HEALTH_HUD_OVERLAY.get();
    }

    private static boolean shouldShowEstimatedHealth(ItemStack hoveredStack,FoodValues modifiedFoodValues)
    {
        if(!ModConfig.SHOW_FOOD_HEALTH_HUD_OVERLAY.get())return false;
        Minecraft mc=Minecraft.getInstance();
        Player player=mc.player;
        FoodData stats=player.getFoodData();
        if(player.level().getDifficulty()==Difficulty.PEACEFUL)return false;
        if(stats.getFoodLevel()>=18)return false;
        if(player.hasEffect(MobEffects.POISON))return false;
        if(player.hasEffect(MobEffects.WITHER))return false;
        if(player.hasEffect(MobEffects.REGENERATION))return false;
        return true;
    }

    private static void generateHealthBarOffsets(int top,int left,int right,int ticks,Player player)
    {
        random.setSeed((long)(ticks*312871L));
        final int preferHealthBars=10;
        final float maxHealth=player.getMaxHealth();
        final float absorptionHealth=(float)Math.ceil(player.getAbsorptionAmount());
        int healthBars=(int)Math.ceil((maxHealth+absorptionHealth)/2.0F);
        if(healthBars<0 || healthBars>1000){healthBarOffsets.setSize(0);return;}
        int healthRows=(int)Math.ceil((float)healthBars/10.0F);
        int healthRowHeight=Math.max(10-(healthRows-2),3);
        boolean shouldAnimatedHealth=false;
        if(ModConfig.SHOW_VANILLA_ANIMATION_OVERLAY.get())
        {
            shouldAnimatedHealth=Math.ceil(player.getHealth())<=4;
        }
        if(healthBarOffsets.size()!=healthBars)healthBarOffsets.setSize(healthBars);
        for(int i=healthBars-1;i>=0;--i)
        {
            int row=(int)Math.ceil((float)(i+1)/(float)preferHealthBars)-1;
            int x=left+i%preferHealthBars*8;
            int y=top-row*healthRowHeight;
            if(shouldAnimatedHealth)y+=random.nextInt(2);
            IntPoint point=healthBarOffsets.get(i);
            if(point==null){point=new IntPoint();healthBarOffsets.set(i,point);}
            point.x=x-left;
            point.y=y-top;
        }
    }

    private static void generateHungerBarOffsets(int top,int left,int right,int ticks,Player player)
    {
        final int preferFoodBars=10;
        boolean shouldAnimatedFood=false;
        if(ModConfig.SHOW_VANILLA_ANIMATION_OVERLAY.get())
        {
            FoodData stats=player.getFoodData();
            float saturationLevel=stats.getSaturationLevel();
            int foodLevel=stats.getFoodLevel();
            shouldAnimatedFood=saturationLevel<=0.0F && ticks%(foodLevel*3+1)==0;
        }
        if(foodBarOffsets.size()!=preferFoodBars)foodBarOffsets.setSize(preferFoodBars);
        for(int i=0;i<preferFoodBars;++i)
        {
            int x=right-i*8-9;
            int y=top;
            if(shouldAnimatedFood)y+=random.nextInt(3)-1;
            IntPoint point=foodBarOffsets.get(i);
            if(point==null){point=new IntPoint();foodBarOffsets.set(i,point);}
            point.x=x-right;
            point.y=y-top;
        }
    }
}
