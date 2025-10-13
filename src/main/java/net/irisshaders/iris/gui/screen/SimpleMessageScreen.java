package net.irisshaders.iris.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SimpleMessageScreen extends Screen {
    private final Screen parent;
    private final Component messageTemp;
    private MultiLineLabel message;

    public SimpleMessageScreen(Screen parent, Component title, Component message) {
        super(title);
        this.parent = parent;
        this.messageTemp = message;
    }

    @Override
    protected void init() {
        super.init();
        this.message = MultiLineLabel.create(this.font, messageTemp, this.width - 50);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, 
                button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        message.renderCentered(guiGraphics, this.width / 2, 40, 9, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}