package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatterImpls;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.features.FeatureFlags;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(SodiumConfigBuilder.class)
public class MixinSodiumOptions {
	@WrapOperation(method = "buildQualityPage", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/config/structure/BooleanOptionBuilder;setName(Lnet/minecraft/network/chat/Component;)Lnet/caffeinemc/mods/sodium/api/config/structure/BooleanOptionBuilder;"))
	private BooleanOptionBuilder iris$blockFabulous(BooleanOptionBuilder instance, Component component, Operation<BooleanOptionBuilder> original) {
		if (component.getContents() instanceof TranslatableContents contents && (contents.getKey().contains("improved"))) {
			return original.call(instance.setEnabledProvider(i -> Iris.getCurrentPack().isEmpty(), ConfigState.UPDATE_ON_REBUILD), component);
		}

		return original.call(instance, component);
	}


	@WrapOperation(method = "buildQualityPage", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/config/structure/EnumOptionBuilder;setName(Lnet/minecraft/network/chat/Component;)Lnet/caffeinemc/mods/sodium/api/config/structure/EnumOptionBuilder;"))
	private <E extends Enum<E>> EnumOptionBuilder<E> iris$blockRGSSx(EnumOptionBuilder<E> instance, Component component, Operation<EnumOptionBuilder<E>> original) {
		Class<E> enumClass = ((EnumOptionBuilderImplAccessor<E>) instance).getEnumClass();

		if (enumClass.equals(TextureFilteringMethod.class)) {
			return original.call(instance.setEnabledProvider(i -> Iris.getCurrentPack().isEmpty() || Iris.getCurrentPack().get().hasFeature(FeatureFlags.TEXTURE_FILTERING), ConfigState.UPDATE_ON_REBUILD), component);
		}

		return original.call(instance, component);
	}


	@WrapOperation(method = "buildQualityPage", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;setEnabledProvider(Ljava/util/function/Function;[Lnet/minecraft/resources/Identifier;)Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;"))
	private IntegerOptionBuilder iris$blockRGSS3(IntegerOptionBuilder instance, Function<ConfigState, Boolean> configStateBooleanFunction, Identifier[] identifiers, Operation<IntegerOptionBuilder> original) {

		if (((IntegerOptionBuilderImplAccessor) instance).getValueFormatter() == ControlValueFormatterImpls.anisotropyBit()) {
			return original.call(instance, (Function<ConfigState, Boolean>) state -> {
				if (Iris.getCurrentPack().isPresent()) {
					return configStateBooleanFunction.apply(state) && Iris.getCurrentPack().get().hasFeature(FeatureFlags.TEXTURE_FILTERING);
				} else {
					return configStateBooleanFunction.apply(state);
				}
			}, ArrayUtils.add(identifiers, ConfigState.UPDATE_ON_REBUILD));
		}

		return original.call(instance, configStateBooleanFunction, identifiers);
	}

	@WrapOperation(method = "buildQualityPage", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/config/structure/BooleanOptionBuilder;setTooltip(Lnet/minecraft/network/chat/Component;)Lnet/caffeinemc/mods/sodium/api/config/structure/BooleanOptionBuilder;"))
	private BooleanOptionBuilder iris$blockFabulous2(BooleanOptionBuilder instance, Component component, Operation<BooleanOptionBuilder> original) {
		if (component.getContents() instanceof TranslatableContents contents && contents.getKey().contains("improved")) {
			return instance.setTooltip(i -> {
				if (Iris.getCurrentPack().isPresent()) {
					return Component.literal("This option is not relevant when a shader pack is active.");
				} else {
					return component;
				}
			});
		}

		return original.call(instance, component);
	}

	@WrapOperation(method = "buildQualityPage", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/config/structure/EnumOptionBuilder;setTooltip(Ljava/util/function/Function;)Lnet/caffeinemc/mods/sodium/api/config/structure/EnumOptionBuilder;"))
	private <E extends Enum<E>> EnumOptionBuilder<E> iris$blockRGSS2(EnumOptionBuilder<E> instance, Function<E, Component> eComponentFunction, Operation<EnumOptionBuilder<E>> original) {
		Class<E> enumClass = ((EnumOptionBuilderImplAccessor<E>) instance).getEnumClass();

		if (enumClass.equals(TextureFilteringMethod.class)) {
			Function<E, Component> newFunction = e -> {
				if (Iris.getCurrentPack().isPresent() && !Iris.getCurrentPack().get().hasFeature(FeatureFlags.TEXTURE_FILTERING)) {
					return Component.literal("Your currently active shader pack does not support this.");
				} else {
					return eComponentFunction.apply(e);
				}
			};

			return original.call(instance, newFunction);
		}

		return original.call(instance, eComponentFunction);
	}
}
