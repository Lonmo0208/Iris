package net.irisshaders.iris.compat.acceleratedrendering.gui;

import com.github.argon4w.acceleratedrendering.AcceleratedRenderingModEntry;
import net.minecraft.ChatFormatting;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;

public class AcceleratedRenderingModInfo {

    private static final String AR_MOD_NAME = FMLLoader.getLoadingModList().getModFileById(AcceleratedRenderingModEntry.MODID).getMods().getFirst().getDisplayName();

    private static final String AR_MOD_VERSION = FMLLoader.getLoadingModList().getModFileById(AcceleratedRenderingModEntry.MODID).versionString();

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
}
