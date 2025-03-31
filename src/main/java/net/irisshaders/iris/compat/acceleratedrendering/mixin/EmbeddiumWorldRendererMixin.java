package net.irisshaders.iris.compat.acceleratedrendering.mixin;

import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.buffers.SimpleCrumblingBufferSource;
import com.github.argon4w.acceleratedrendering.features.blocks.AcceleratedBlockEntityRenderingFeature;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.SortedSet;

@Mixin(EmbeddiumWorldRenderer.class)
public class EmbeddiumWorldRendererMixin {


    @WrapOperation(
            method = {"renderBlockEntity"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
            )}
    )
    private static void wrapRenderBlockEntity(BlockEntityRenderDispatcher instance, BlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, Operation<Void> original, @Local(argsOnly = true) Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions) {
        if (!AcceleratedBlockEntityRenderingFeature.isEnabled()) {
            original.call(instance, pBlockEntity, pPartialTick, pPoseStack, pBufferSource);
        } else if (!AcceleratedBlockEntityRenderingFeature.shouldUseAcceleratedPipeline()) {
            original.call(instance, pBlockEntity, pPartialTick, pPoseStack, pBufferSource);
        } else {
            BlockPos blockPos = pBlockEntity.getBlockPos();
            MultiBufferSource bufferSource = CoreBuffers.CORE;
            SortedSet<BlockDestructionProgress> destructionProgresses = (SortedSet)blockBreakingProgressions.get(blockPos.asLong());
            if (destructionProgresses != null && !destructionProgresses.isEmpty()) {
                int progress = ((BlockDestructionProgress)destructionProgresses.last()).getProgress();
                if (progress >= 0) {
                    bufferSource = new SimpleCrumblingBufferSource(bufferSource, (RenderType) ModelBakery.DESTROY_TYPES.get(progress), pPoseStack, 1.0F);
                }

                original.call(instance, pBlockEntity, pPartialTick, pPoseStack, bufferSource);
            } else {
                original.call(instance, pBlockEntity, pPartialTick, pPoseStack, bufferSource);
            }
        }
    }

}
