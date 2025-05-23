package net.irisshaders.iris.platform;

import com.mojang.logging.LogUtils;
import net.irisshaders.iris.gui.screen.ScreenHandler;
import net.irisshaders.iris.vertices.embeddium.terrain.IrisModelVertexFormats;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Mod(value = IrisForgeMod.MODID, dist = Dist.CLIENT)

public class IrisForgeMod {
	public static final String MODID = "iris";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static List<KeyMapping> KEYLIST = new ArrayList<>();
	public static final String FALSE = "false";
	private static final Properties CONFIG = new Properties();
	public static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("iris.properties");
	private static boolean ALLOW_FULL_FORMAT = false;
	private static boolean MOVE_LAST = false;
	private static boolean END_BATCH = false;
	private static boolean NO_SHADOW_STAGE = false;
	private static final String[] MODS = new String[]{
		"justdirethings",
		"supplementaries",
		"caxton",
		"twilightforest",
		"enchanted",
		"the_bumblezone",
		"forbidden_arcanus",
		"xycraft",
		"immersiveengineering",
		"computercraft",
		"creeperoverhaul",
		"arsnouveau",
		"mekanism",
		"eternal_starlight",
		"refurbished_furniture",
		"enderstorage",
		"extended_industrialization",
		"stellarview",
		"ping",
		"minecraft"
	};

	static {
		try {
			loadConfig();
		} catch (IOException e) {
			LOGGER.error("Failed to load EmbX config", e);
			throw new RuntimeException(e);
		}
	}

	public IrisForgeMod(IEventBus bus, ModContainer modContainer) {
		LOGGER.info("Loaded Iris (NeoForge) v{}", modContainer.getModInfo().getVersion());

		bus.addListener(this::registerKeys);
		modContainer.registerExtensionPoint(IConfigScreenFactory.class, ScreenHandler::registerConfigScreen);

		LOGGER.info("Initialized EmbX compatibility layer");
	}

	public void registerKeys(RegisterKeyMappingsEvent event) {
		KEYLIST.forEach(event::register);
		KEYLIST.clear();
	}

	public static ChunkVertexType getVertexFormat() {
		if (!ALLOW_FULL_FORMAT) {
			return IrisModelVertexFormats.MODEL_VERTEX_XHFP;
		}
		return Embeddium.options().performance.useCompactVertexFormat ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : IrisModelVertexFormats.MODEL_VERTEX_XSFP;
	}

	public static boolean allowsFullFormat() {
		return ALLOW_FULL_FORMAT;
	}

	public static boolean moveRenderLastStage() {
		return MOVE_LAST;
	}

	public static boolean endBatch() {
		return END_BATCH;
	}

	public static boolean noShadowStage() {
		return NO_SHADOW_STAGE;
	}

	public static Properties loadConfig() throws IOException {
		if (!Files.exists(CONFIG_PATH)) {
			makeConfig();
		}
		CONFIG.load(Files.newBufferedReader(CONFIG_PATH));
		ALLOW_FULL_FORMAT = !FALSE.equals(CONFIG.getProperty("FullVertexFormat"));
		MOVE_LAST = !FALSE.equals(CONFIG.getProperty("MoveRenderLastStage"));
		END_BATCH = !FALSE.equals(CONFIG.getProperty("EndBatch"));
		NO_SHADOW_STAGE = !FALSE.equals(CONFIG.get("NoShadowStage"));
		return CONFIG;
	}

	private static boolean parseBoolean(String value) {
		return value != null && !"false".equalsIgnoreCase(value);
	}

	private static void makeConfig() {
		try (var writer = Files.newBufferedWriter(CONFIG_PATH)) {
			writer.write("# Alternative Modded Shaders\n");
			writer.write("# ====================\n");
			writer.write("# Enable/disable alternative shaders (true/false)\n");
			for (String mod : MODS) {
				writer.write(mod + " = false\n");
			}

			writer.write("# Render Level Event\n");
			writer.write("MoveRenderLastStage = true\n");
			writer.write("EndBatch = true\n");
			writer.write("NoShadowStage = true\n");

			writer.write("# Misc Changes\n");
			writer.write("FullVertexFormat = true\n");
		} catch (IOException e) {
			LOGGER.error("Failed to create EmbX config", e);
		}
	}
}
