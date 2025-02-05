package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderBuffers.class)
public class MixinRenderBuffers implements RenderBuffersExt, MemoryTrackingRenderBuffers, DrawCallTrackingRenderBuffers {
	@Unique
	private final FullyBufferedMultiBufferSource buffered = new FullyBufferedMultiBufferSource();
	@Unique
	private final OutlineBufferSource outlineBufferSource = new OutlineBufferSource(buffered);
	@Unique
	private int begins = 0;
	@Unique
	private int maxBegins = 0;
	@Shadow
	@Final
	private MultiBufferSource.BufferSource bufferSource;

	@Shadow
	@Final
	private MultiBufferSource.BufferSource crumblingBufferSource;

	@Shadow
	@Final
	private ChunkBufferBuilderPack fixedBufferPack;

	@Inject(method = "bufferSource", at = @At("HEAD"), cancellable = true)
	private void batchedentityrendering$replaceBufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
		if (shouldReplaceBufferSource()) {
			cir.setReturnValue(buffered);
		}
	}

	@Inject(method = "crumblingBufferSource", at = @At("HEAD"), cancellable = true)
	private void batchedentityrendering$replaceCrumblingBufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
		if (shouldReplaceBufferSource()) {
			cir.setReturnValue(buffered.getUnflushableWrapper());
		}
	}

	@Inject(method = "outlineBufferSource", at = @At("HEAD"), cancellable = true)
	private void batchedentityrendering$replaceOutlineBufferSource(CallbackInfoReturnable<OutlineBufferSource> provider) {
		if (shouldReplaceBufferSource()) {
			provider.setReturnValue(outlineBufferSource);
		}
	}

	private boolean shouldReplaceBufferSource() {
		return begins != 0;
	}

	@Override
	public void beginLevelRendering() {
		if (begins == 0) {
			buffered.assertWrapStackEmpty();
		}
		begins++;
		maxBegins = Math.max(maxBegins, begins);
	}

	@Override
	public void endLevelRendering() {
		begins--;
		if (begins == 0) {
			buffered.assertWrapStackEmpty();
		}
	}

	@Override
	public int getEntityBufferAllocatedSize() {
		return ((MemoryTrackingBuffer) buffered).getAllocatedSize();
	}

	@Override
	public int getMiscBufferAllocatedSize() {
		return ((MemoryTrackingBuffer) bufferSource).getAllocatedSize();
	}

	@Override
	public int getMaxBegins() {
		return maxBegins;
	}

	@Override
	public void freeAndDeleteBuffers() {
		freeBuffer(buffered);
		freeFixedBuffers();
		freeOutlineBuffer();
	}

	private void freeBuffer(MemoryTrackingBuffer buffer) {
		buffer.freeAndDeleteBuffer();
	}

	private void freeFixedBuffers() {
		((ChunkBufferBuilderPackAccessor) fixedBufferPack).getBuilders().values().forEach(bufferBuilder -> {
			if (bufferBuilder instanceof MemoryTrackingBuffer) {
				freeBuffer((MemoryTrackingBuffer) bufferBuilder);
			}
		});
	}

	private void freeOutlineBuffer() {
		((MemoryTrackingBuffer) ((OutlineBufferSourceAccessor) outlineBufferSource).getOutlineBufferSource()).freeAndDeleteBuffer();
	}

	@Override
	public int getDrawCalls() {
		return buffered.getDrawCalls();
	}

	@Override
	public int getRenderTypes() {
		return buffered.getRenderTypes();
	}

	@Override
	public void resetDrawCounts() {
		buffered.resetDrawCalls();
	}
}
