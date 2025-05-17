package net.irisshaders.iris.compat.acceleratedrendering.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;

public class ARModInfo {

    private static final String AR_MOD_NAME = FMLLoader.getLoadingModList().getModFileById( "acceleratedrendering").getMods().getFirst().getDisplayName();

    private static final String AR_MOD_VERSION = FMLLoader.getLoadingModList().getModFileById( "acceleratedrendering").versionString();

    public static String getArModName(){
        return AR_MOD_NAME;
    }

    public static String getArModVersion(){
        return AR_MOD_VERSION;
    }

    public static String getFormattedArModVersion() {
        ChatFormatting color;
        String version = getArModVersion();

        if (!FMLEnvironment.production) {
            color = ChatFormatting.GOLD;
            version = version + " (Development Environment)";
        } else if (version.endsWith("-dirty") || version.contains("unknown") || version.endsWith("-nogit")) {
            color = ChatFormatting.RED;
        } else if (version.contains("+rev.")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else {
            color = ChatFormatting.GREEN;
        }

        return color + version;
    }

    public static ResourceLocation location(String path){
        return ResourceLocation.fromNamespaceAndPath( "acceleratedrendering", path);
    }
}
