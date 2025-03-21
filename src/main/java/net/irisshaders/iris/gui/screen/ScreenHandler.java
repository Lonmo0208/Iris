package net.irisshaders.iris.gui.screen;

import net.irisshaders.iris.gui.debug.DebugLoadFailedGridScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

public class ScreenHandler {
    // 需要这个类来避免Screen被提前加载，导致其他模组对Screen类的Mixin失效

    public static void openShaderPackScreen(){
        Minecraft.getInstance().setScreen(new ShaderPackScreen(null));
    }

    public static Screen registerConfigScreen(ModContainer modContainer, Screen screen) {
        return new ShaderPackScreen(screen);
    }

    public static void openDebugLoadFailedGridScreen(Component component,Exception e){
        Minecraft.getInstance().setScreen(new DebugLoadFailedGridScreen(Minecraft.getInstance().screen, component,e));
    }
}
