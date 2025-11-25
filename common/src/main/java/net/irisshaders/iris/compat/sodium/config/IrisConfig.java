package net.irisshaders.iris.compat.sodium.config;

import java.io.IOException;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.Range;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.gui.screen.ShaderPackConfigScreen;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

public class IrisConfig implements ConfigEntryPoint {
	public static final Identifier MONO = Identifier.fromNamespaceAndPath("iris", "textures/gui/config-icon-mono.png");
	public static final Identifier COLOR = Identifier.fromNamespaceAndPath("iris", "textures/gui/iris-logo.png");

	public IrisConfig() {
	}

	public void registerConfigLate(ConfigBuilder builder) {
		builder.registerOwnModOptions()
			.setName("Iris")
			.setIcon(MONO)
			.setColorTheme(builder.createColorTheme().setBaseThemeRGB(-698654))
			.addPage(builder.createExternalPage()
				.setName(Component.translatable("options.iris.shaderPackSelection.title"))
				.setScreenProvider((i) -> Minecraft.getInstance().setScreen(new ShaderPackScreen(i))))
			.addPage(builder.createOptionPage()
				.setName(Component.literal("Settings"))
				.addOptionGroup(builder.createOptionGroup()
					.addOption(builder.createExternalButtonOption(Identifier.fromNamespaceAndPath("iris", "settings"))
						.setTooltip(Component.translatable("options.iris.shaderPackList.sodium_tooltip"))
						.setName(Component.translatable("options.iris.shaderPackList"))
						.setScreenProvider((i) -> Minecraft.getInstance().setScreen(new ShaderPackScreen(i))))
					.addOption(builder.createExternalButtonOption(Identifier.fromNamespaceAndPath("iris", "shaderPackConfig"))
						.setTooltip(Component.translatable("options.iris.shaderPackConfig.sodium_tooltip"))
						.setName(Component.translatable("iris.shaderPackConfig.title"))
						.setScreenProvider((i) -> Minecraft.getInstance().setScreen(new ShaderPackConfigScreen(i)))))
				.addOptionGroup(builder.createOptionGroup()
					.addOption(builder.createEnumOption(Identifier.fromNamespaceAndPath("iris", "colorSpace"), ColorSpace.class)
						.setBinding((i) -> IrisVideoSettings.colorSpace = i, () -> IrisVideoSettings.colorSpace)
						.setName(Component.translatable("options.iris.colorSpace"))
						.setDefaultValue(ColorSpace.SRGB)
						.setTooltip(Component.translatable("options.iris.colorSpace.sodium_tooltip"))
						.setStorageHandler(() -> {
							try {
								Iris.getIrisConfig().save();
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						})
						.setElementNameProvider(ColorSpace::getName))
					.addOption(builder.createIntegerOption(Identifier.fromNamespaceAndPath("iris", "shadowDistance"))
						.setDefaultValue(32)
						.setBinding((value) -> IrisVideoSettings.shadowDistance = value,
							() -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance))
						.setName(Component.translatable("options.iris.shadowDistance"))
						.setTooltip((i) -> !IrisVideoSettings.isShadowDistanceSliderEnabled()
							? Component.translatable("options.iris.shadowDistance.disabled")
							: Component.translatable("options.iris.shadowDistance.sodium_tooltip"))
						.setValueFormatter(ControlValueFormatterImpls.quantityOrDisabled(
							(i) -> Component.translatable("options.chunks", i),
							Component.literal("None")))
						.setEnabledProvider((i) -> IrisVideoSettings.isShadowDistanceSliderEnabled(),
							new Identifier[]{ConfigState.UPDATE_ON_REBUILD})
						.setStorageHandler(() -> {
							try {
								Iris.getIrisConfig().save();
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						})
						.setRange(new Range(0, 32, 1))
						.setImpact(OptionImpact.HIGH))));
	}
}
