package net.irisshaders.iris.platform;

import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.concurrent.CopyOnWriteArrayList;

@Mod("iris")
public class IrisForgeMod {
	private static final CopyOnWriteArrayList<KeyMapping> KEYLIST = new CopyOnWriteArrayList<>();

	public IrisForgeMod() {
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> {
			return new ShaderPackScreen(screen);
		}));
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeys);
	}

	public static void registerKeyBinding(KeyMapping keyMapping) {
		KEYLIST.add(keyMapping);
	}

	public void registerKeys(RegisterKeyMappingsEvent event) {
		KEYLIST.forEach(event::register);
		KEYLIST.clear();
	}
}
