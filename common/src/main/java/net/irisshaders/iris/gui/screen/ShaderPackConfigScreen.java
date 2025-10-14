package net.irisshaders.iris.gui.screen;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.ShaderPackConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.io.IOException;

public class ShaderPackConfigScreen extends Screen {
	private final Screen parent;
	private final ShaderPackConfig config;

	public ShaderPackConfigScreen(Screen parent) {
		super(Component.translatable("iris.shaderPackConfig.title"));
		this.parent = parent;
		this.config = ShaderPackConfig.get();
	}

	@Override
	protected void init() {
		super.init();

		int centerX = this.width / 2;
		int buttonWidth = 200;
		int buttonHeight = 20;

		// Shader Pack Version Selector
		this.addRenderableWidget(CycleButton.<ShaderPackConfig.ShaderPackVersion>builder(version -> Component.translatable(version.getTranslationKey()))
			.withValues(ShaderPackConfig.ShaderPackVersion.values())
			.withInitialValue(config.getShaderPackVersion())
			.create(centerX - buttonWidth / 2, 60, buttonWidth, buttonHeight,
				Component.translatable("iris.shaderPackConfig.version"),
				(button, value) -> {
					config.setShaderPackVersion(value);
				}));

		this.addRenderableWidget(Button.builder(Component.translatable("iris.shaderPackConfig.versionInfo"),
				button -> showVersionInfo())
			.pos(centerX - buttonWidth / 2, 90)
			.size(buttonWidth, buttonHeight)
			.build());

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK,
				button -> {
					try {
						Iris.reload();
					} catch (IOException e) {
						Iris.logger.error("Failed to reload shaders after changing shader pack version", e);
					}
					this.minecraft.setScreen(parent);
				})
			.pos(centerX - 100, this.height - 30)
			.size(200, buttonHeight)
			.build());
	}

	private void showVersionInfo() {
		MutableComponent version1Title = Component.literal("§e").append(Component.translatable("iris.shaderPackConfig.version1.title")).append("§r");
		Component version1Point1 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version1.point1").getString());
		Component version1Point2 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version1.point2").getString());
		Component version1Point3 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version1.point3").getString());
		Component version1Point4 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version1.point4").getString());

		Component version2Title = Component.literal("§e").append(Component.translatable("iris.shaderPackConfig.version2.title")).append("§r");
		Component version2Point1 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version2.point1").getString());
		Component version2Point2 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version2.point2").getString());
		Component version2Point3 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version2.point3").getString());
		Component version2Point4 = Component.literal("• " + Component.translatable("iris.shaderPackConfig.version2.point4").getString());

		Component message = version1Title
			.append("\n")
			.append(version1Point1)
			.append("\n")
			.append(version1Point2)
			.append("\n")
			.append(version1Point3)
			.append("\n")
			.append(version1Point4)
			.append("\n\n")
			.append(version2Title)
			.append("\n")
			.append(version2Point1)
			.append("\n")
			.append(version2Point2)
			.append("\n")
			.append(version2Point3)
			.append("\n")
			.append(version2Point4);

		this.minecraft.setScreen(new SimpleMessageScreen(this,
			Component.translatable("iris.shaderPackConfig.versionInfo.title"),
			message));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		this.renderBackground(guiGraphics, mouseX, mouseY, delta);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, delta);
	}

}
