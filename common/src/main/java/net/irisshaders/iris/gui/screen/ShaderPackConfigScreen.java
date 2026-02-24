package net.irisshaders.iris.gui.screen;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.ShaderPackConfig;
import net.irisshaders.iris.gui.element.screen.IrisButton;
import net.irisshaders.iris.gui.element.screen.IrisCycleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

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

		this.addRenderableWidget(IrisCycleButton.<ShaderPackConfig.ShaderPackVersion>iris$builder(
				version -> Component.translatable(version.getTranslationKey()),
				config::getShaderPackVersion,
				() -> 1.0f)
			.withValues(ShaderPackConfig.ShaderPackVersion.values())
			.create(centerX - buttonWidth / 2, 60, buttonWidth, buttonHeight,
				Component.translatable("iris.shaderPackConfig.version"),
				(button, value) -> {
					config.setShaderPackVersion(value);
				}));

		this.addRenderableWidget(IrisButton.iris$builder(Component.translatable("iris.shaderPackConfig.versionInfo"),
				button -> showVersionInfo(), () -> 1.0F)
			.pos(centerX - buttonWidth / 2, 90)
			.size(buttonWidth, buttonHeight)
			.build());

		this.addRenderableWidget(IrisButton.iris$builder(CommonComponents.GUI_BACK,
				button -> {
					try {
						Iris.reload();
					} catch (IOException e) {
						Iris.logger.error("Failed to reload shaders after changing shader pack version", e);
					}
					this.minecraft.setScreen(parent);
				}, () -> 1.0F)
			.pos(centerX - 100, this.height - 30)
			.size(200, buttonHeight)
			.build());
	}

	private void showVersionInfo() {
		this.minecraft.setScreen(new SimpleMessageScreen(this));
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
		super.render(guiGraphics, mouseX, mouseY, delta);
	}
}
