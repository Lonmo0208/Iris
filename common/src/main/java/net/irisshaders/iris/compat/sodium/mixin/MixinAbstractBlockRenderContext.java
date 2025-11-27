package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.irisshaders.iris.compat.general.IrisModSupport;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(AbstractBlockRenderContext.class)
public class MixinAbstractBlockRenderContext {
	@Shadow
	protected BlockPos pos;

	@Shadow
	protected BlockAndTintGetter level;

}
