package net.irisshaders.iris.compat.acceleratedrendering.gui;

import com.github.argon4w.acceleratedrendering.AcceleratedRenderingModEntry;
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
}
