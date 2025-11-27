package net.irisshaders.iris.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.irisshaders.iris.gui.element.screen.IrisButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SimpleMessageScreen extends Screen {
	private final Screen parent;
	private final List<AbstractWidget> infoWidgets = new ArrayList<>();

	public SimpleMessageScreen(Screen parent) {
		super(Component.translatable("iris.shaderPackConfig.versionInfo.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		super.init();

		int centerX = this.width / 2;
		int buttonWidth = 300;
		int startY = 40;
		int buttonHeight = 20;
		int spacing = 5;

		addInfoButton(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight,
			Component.translatable("iris.shaderPackConfig.version1.title").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
		addInfoButton(centerX - buttonWidth / 2, startY + buttonHeight + spacing, buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version1.point1")));
		addInfoButton(centerX - buttonWidth / 2, startY + 2 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version1.point2")));
		addInfoButton(centerX - buttonWidth / 2, startY + 3 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version1.point3")));
		addInfoButton(centerX - buttonWidth / 2, startY + 4 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version1.point4")));
		addInfoButton(centerX - buttonWidth / 2, startY + 6 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.translatable("iris.shaderPackConfig.version2.title").withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.BOLD));
		addInfoButton(centerX - buttonWidth / 2, startY + 7 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version2.point1")));
		addInfoButton(centerX - buttonWidth / 2, startY + 8 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version2.point2")));
		addInfoButton(centerX - buttonWidth / 2, startY + 9 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version2.point3")));
		addInfoButton(centerX - buttonWidth / 2, startY + 10 * (buttonHeight + spacing), buttonWidth, buttonHeight,
			Component.literal("• ").append(Component.translatable("iris.shaderPackConfig.version2.point4")));

		this.addRenderableWidget(IrisButton.iris$builder(CommonComponents.GUI_BACK,
				button -> this.minecraft.setScreen(parent), () -> 1.0F)
			.bounds(centerX - 100, this.height - 30, 200, 20)
			.build());
	}

	private void addInfoButton(int x, int y, int width, int height, Component text) {
		IrisButton button = IrisButton.iris$builder(text, btn -> {}, () -> 1.0F)
			.pos(x, y)
			.size(width, height)
			.build();
		this.addRenderableWidget(button);
		this.infoWidgets.add(button);
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
		super.render(guiGraphics, mouseX, mouseY, delta);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
	}

	public List<AbstractWidget> getInfoWidgets() {
		return infoWidgets;
	}
}
