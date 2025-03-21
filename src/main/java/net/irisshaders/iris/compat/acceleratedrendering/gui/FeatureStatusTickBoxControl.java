package net.irisshaders.iris.compat.acceleratedrendering.gui;

import com.github.argon4w.acceleratedrendering.configs.FeatureStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.Rect2i;
import org.embeddedt.embeddium.api.math.Dim2i;
import org.embeddedt.embeddium.api.options.control.Control;
import org.embeddedt.embeddium.api.options.control.ControlElement;
import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.options.structure.OptionControlElement;

public class FeatureStatusTickBoxControl implements Control<FeatureStatus> {

    private final Option<FeatureStatus> option;

    public FeatureStatusTickBoxControl(Option<FeatureStatus> option) {
        this.option = option;
    }

    @Override
    public Option<FeatureStatus> getOption() {
        return option;
    }

    @Override
    public OptionControlElement<FeatureStatus> createElement(Dim2i dim2i) {
        return new TickBoxControlElement(this.option, dim2i);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    private static class TickBoxControlElement extends ControlElement<FeatureStatus> {
        private final Rect2i button;

        public TickBoxControlElement(Option<FeatureStatus> option, Dim2i dim) {
            super(option, dim);
            this.button = new Rect2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);
            int x = this.button.getX();
            int y = this.button.getY();
            int w = x + this.button.getWidth();
            int h = y + this.button.getHeight();
            boolean enabled = this.option.isAvailable();
            boolean ticked = this.option.getValue() == FeatureStatus.ENABLED;
            int color;
            if (enabled) {
                color = ticked ? -3179338 : -1;
            } else {
                color = -5592406;
            }

            if (ticked) {
                this.drawRect(drawContext, x + 2, y + 2, w - 2, h - 2, color);
            }

            this.drawBorder(drawContext, x, y, w, h, color);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                this.toggleControl();
                this.playClickSound();
                return true;
            } else {
                return false;
            }
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused()) {
                return false;
            } else if (CommonInputs.selected(keyCode)) {
                this.toggleControl();
                this.playClickSound();
                return true;
            } else {
                return false;
            }
        }

        public void toggleControl() {
            switch (this.option.getValue()) {
                case ENABLED:
                    this.option.setValue(FeatureStatus.DISABLED);
                    break;
                case DISABLED:
                    this.option.setValue(FeatureStatus.ENABLED);
            }
        }
    }
}
