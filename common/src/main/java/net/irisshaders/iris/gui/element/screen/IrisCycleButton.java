package net.irisshaders.iris.gui.element.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gui.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class IrisCycleButton<T> extends AbstractButton {
	public static final BooleanSupplier DEFAULT_ALT_LIST_SELECTOR = () -> Minecraft.getInstance().hasAltDown();
	private static final List<Boolean> BOOLEAN_OPTIONS = ImmutableList.of(Boolean.TRUE, Boolean.FALSE);

	private final FloatSupplier alphaSupplier;
	private final Supplier<T> defaultValueSupplier;
	private final Component name;
	private int index;
	private T value;
	private final ValueListSupplier<T> values;
	private final Function<T, Component> valueStringifier;
	private final Function<IrisCycleButton<T>, MutableComponent> narrationProvider;
	private final OnValueChange<T> onValueChange;
	private final DisplayState displayState;
	private final OptionTooltipSupplier<T> tooltipSupplier;

	IrisCycleButton(int x, int y, int width, int height, Component message, Component name, int index,
					T value, Supplier<T> defaultValueSupplier, ValueListSupplier<T> values,
					Function<T, Component> valueStringifier, Function<IrisCycleButton<T>, MutableComponent> narrationProvider,
					OnValueChange<T> onValueChange, OptionTooltipSupplier<T> tooltipSupplier,
					DisplayState displayState, FloatSupplier alphaSupplier) {
		super(x, y, width, height, message);
		this.alphaSupplier = alphaSupplier;
		this.name = name;
		this.index = index;
		this.defaultValueSupplier = defaultValueSupplier;
		this.value = value;
		this.values = values;
		this.valueStringifier = valueStringifier;
		this.narrationProvider = narrationProvider;
		this.onValueChange = onValueChange;
		this.displayState = displayState;
		this.tooltipSupplier = tooltipSupplier;
		this.updateTooltip();
	}


	@Override
	public void onPress(InputWithModifiers inputWithModifiers) {
		this.cycleValue(1);
	}

	@Override
	public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();

		// Iris button rendering
		GlStateManager._enableBlend();
		GlStateManager._enableDepthTest();
		GuiUtil.bindIrisWidgetsTexture();
		GuiUtil.drawButton(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.isHoveredOrFocused(), !this.isActive());

		// Render text
		if (this.displayState != DisplayState.HIDE) {
			int textColor = this.active ? 16777215 : 10526880;
			this.renderString(guiGraphics, minecraft.font, textColor);
		}
	}

	@Override
	public float getAlpha() {
		return this.alphaSupplier.getAsFloat();
	}

	private void renderString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int color) {
		Component message = this.getMessage();
		int x = this.getX() + this.getWidth() / 2;
		int y = this.getY() + (this.getHeight() - 8) / 2;

		guiGraphics.drawCenteredString(font, message, x, y, color | Mth.ceil(this.alphaSupplier.getAsFloat() * 255.0F) << 24);
	}

	private void updateTooltip() {
		Tooltip tooltip = this.tooltipSupplier.apply(this.value);
		if (tooltip != null) {
			this.setTooltip(tooltip);
		}
	}

	private void cycleValue(int direction) {
		List<T> list = this.values.getSelectedList();
		this.index = Mth.positiveModulo(this.index + direction, list.size());
		T newValue = list.get(this.index);
		this.updateValue(newValue);
		this.onValueChange.onValueChange(this, newValue);
	}

	private T getCycledValue(int direction) {
		List<T> list = this.values.getSelectedList();
		return list.get(Mth.positiveModulo(this.index + direction, list.size()));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY > 0) {
			this.cycleValue(-1);
		} else if (scrollY < 0) {
			this.cycleValue(1);
		}
		return true;
	}

	public void setValue(T value) {
		List<T> list = this.values.getSelectedList();
		int newIndex = list.indexOf(value);
		if (newIndex != -1) {
			this.index = newIndex;
		}
		this.updateValue(value);
	}

	public void resetValue() {
		this.setValue(this.defaultValueSupplier.get());
	}

	private void updateValue(T value) {
		Component newMessage = this.createLabelForValue(value);
		this.setMessage(newMessage);
		this.value = value;
		this.updateTooltip();
	}

	private Component createLabelForValue(T value) {
		return this.displayState == DisplayState.VALUE ?
			this.valueStringifier.apply(value) :
			this.createFullName(value);
	}

	private MutableComponent createFullName(T value) {
		return CommonComponents.optionNameValue(this.name, this.valueStringifier.apply(value));
	}

	public T getValue() {
		return this.value;
	}

	@Override
	protected MutableComponent createNarrationMessage() {
		return this.narrationProvider.apply(this);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
		if (this.active) {
			T nextValue = this.getCycledValue(1);
			Component nextValueLabel = this.createLabelForValue(nextValue);
			if (this.isFocused()) {
				narrationElementOutput.add(NarratedElementType.USAGE,
					Component.translatable("narration.cycle_button.usage.focused", nextValueLabel));
			} else {
				narrationElementOutput.add(NarratedElementType.USAGE,
					Component.translatable("narration.cycle_button.usage.hovered", nextValueLabel));
			}
		}
	}

	public MutableComponent createDefaultNarrationMessage() {
		return wrapDefaultNarrationMessage(
			this.displayState == DisplayState.VALUE ?
				this.createFullName(this.value) :
				this.getMessage()
		);
	}

	public static <T> Builder<T> iris$builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier, FloatSupplier alphaSupplier) {
		return new Builder<>(valueStringifier, defaultValueSupplier, alphaSupplier);
	}

	public static <T> Builder<T> iris$builder(Function<T, Component> valueStringifier, T defaultValue, FloatSupplier alphaSupplier) {
		return new Builder<>(valueStringifier, () -> defaultValue, alphaSupplier);
	}

	public static Builder<Boolean> iris$booleanBuilder(Component trueText, Component falseText, boolean defaultValue, FloatSupplier alphaSupplier) {
		return new Builder<>(
			value -> value ? trueText : falseText,
			() -> defaultValue,
			alphaSupplier
		).withValues(BOOLEAN_OPTIONS);
	}

	public static Builder<Boolean> iris$onOffBuilder(boolean defaultValue, FloatSupplier alphaSupplier) {
		return iris$booleanBuilder(CommonComponents.OPTION_ON, CommonComponents.OPTION_OFF, defaultValue, alphaSupplier);
	}

	public static class Builder<T> {
		private final Supplier<T> defaultValueSupplier;
		private final Function<T, Component> valueStringifier;
		private final FloatSupplier alphaSupplier;
		private OptionTooltipSupplier<T> tooltipSupplier = value -> null;
		private Function<IrisCycleButton<T>, MutableComponent> narrationProvider = IrisCycleButton::createDefaultNarrationMessage;
		private ValueListSupplier<T> values = ValueListSupplier.create(ImmutableList.of());
		private DisplayState displayState = DisplayState.NAME_AND_VALUE;

		public Builder(Function<T, Component> valueStringifier, Supplier<T> defaultValueSupplier, FloatSupplier alphaSupplier) {
			this.valueStringifier = valueStringifier;
			this.defaultValueSupplier = defaultValueSupplier;
			this.alphaSupplier = alphaSupplier;
		}

		public Builder<T> withValues(Collection<T> values) {
			return this.withValues(ValueListSupplier.create(values));
		}

		@SafeVarargs
		public final Builder<T> withValues(T... values) {
			return this.withValues(ImmutableList.copyOf(values));
		}

		public Builder<T> withValues(List<T> defaultList, List<T> altList) {
			return this.withValues(ValueListSupplier.create(DEFAULT_ALT_LIST_SELECTOR, defaultList, altList));
		}

		public Builder<T> withValues(BooleanSupplier altListSelector, List<T> defaultList, List<T> altList) {
			return this.withValues(ValueListSupplier.create(altListSelector, defaultList, altList));
		}

		public Builder<T> withValues(ValueListSupplier<T> values) {
			this.values = values;
			return this;
		}

		public Builder<T> withTooltip(OptionTooltipSupplier<T> tooltipSupplier) {
			this.tooltipSupplier = tooltipSupplier;
			return this;
		}

		public Builder<T> withCustomNarration(Function<IrisCycleButton<T>, MutableComponent> narrationProvider) {
			this.narrationProvider = narrationProvider;
			return this;
		}

		public Builder<T> displayState(DisplayState displayState) {
			this.displayState = displayState;
			return this;
		}

		public Builder<T> displayOnlyValue() {
			return this.displayState(DisplayState.VALUE);
		}

		public IrisCycleButton<T> create(Component name, OnValueChange<T> onValueChange) {
			return this.create(0, 0, 150, 20, name, onValueChange);
		}

		public IrisCycleButton<T> create(int x, int y, int width, int height, Component name) {
			return this.create(x, y, width, height, name, (button, value) -> {});
		}

		public IrisCycleButton<T> create(int x, int y, int width, int height, Component name, OnValueChange<T> onValueChange) {
			List<T> defaultList = this.values.getDefaultList();
			if (defaultList.isEmpty()) {
				throw new IllegalStateException("No values for cycle button");
			}

			T defaultValue = this.defaultValueSupplier.get();
			int defaultIndex = defaultList.indexOf(defaultValue);
			if (defaultIndex == -1) {
				defaultIndex = 0;
				defaultValue = defaultList.get(0);
			}

			Component valueLabel = this.valueStringifier.apply(defaultValue);
			Component buttonMessage = this.displayState == DisplayState.VALUE ?
				valueLabel :
				CommonComponents.optionNameValue(name, valueLabel);

			return new IrisCycleButton<>(
				x, y, width, height, buttonMessage, name, defaultIndex, defaultValue,
				this.defaultValueSupplier, this.values, this.valueStringifier,
				this.narrationProvider, onValueChange, this.tooltipSupplier,
				this.displayState, this.alphaSupplier
			);
		}
	}

	public interface ValueListSupplier<T> {
		List<T> getSelectedList();
		List<T> getDefaultList();

		static <T> ValueListSupplier<T> create(Collection<T> values) {
			final List<T> list = ImmutableList.copyOf(values);
			return new ValueListSupplier<T>() {
				@Override
				public List<T> getSelectedList() {
					return list;
				}

				@Override
				public List<T> getDefaultList() {
					return list;
				}
			};
		}

		static <T> ValueListSupplier<T> create(final BooleanSupplier altListSelector, List<T> defaultList, List<T> altList) {
			final List<T> immutableDefaultList = ImmutableList.copyOf(defaultList);
			final List<T> immutableAltList = ImmutableList.copyOf(altList);

			return new ValueListSupplier<T>() {
				@Override
				public List<T> getSelectedList() {
					return altListSelector.getAsBoolean() ? immutableAltList : immutableDefaultList;
				}

				@Override
				public List<T> getDefaultList() {
					return immutableDefaultList;
				}
			};
		}
	}

	public enum DisplayState {
		NAME_AND_VALUE,
		VALUE,
		HIDE
	}

	@FunctionalInterface
	public interface OnValueChange<T> {
		void onValueChange(IrisCycleButton<T> button, T value);
	}

	@FunctionalInterface
	public interface OptionTooltipSupplier<T> {
		@Nullable Tooltip apply(T value);
	}
}
