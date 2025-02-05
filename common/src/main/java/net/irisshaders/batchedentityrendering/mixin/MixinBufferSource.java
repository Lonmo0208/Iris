package net.irisshaders.batchedentityrendering.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinBufferSource implements MemoryTrackingBuffer {
	@Shadow
	@Final
	protected BufferBuilder builder;

	@Shadow
	@Final
	protected Map<RenderType, BufferBuilder> fixedBuffers;

	private void processFixedBuffers(Processor processor) {
		for (BufferBuilder bufferBuilder : fixedBuffers.values()) {
			processor.process((MemoryTrackingBuffer) bufferBuilder);
		}
	}

	@Override
	public int getAllocatedSize() {
		AtomicInteger allocatedSize = new AtomicInteger(((MemoryTrackingBuffer) builder).getAllocatedSize());
		processFixedBuffers(b -> allocatedSize.addAndGet(b.getAllocatedSize()));
		return allocatedSize.get();
	}

	@Override
	public int getUsedSize() {
		AtomicInteger usedSize = new AtomicInteger(((MemoryTrackingBuffer) builder).getUsedSize());
		processFixedBuffers(b -> usedSize.addAndGet(b.getUsedSize()));
		return usedSize.get();
	}

	@Override
	public void freeAndDeleteBuffer() {
		((MemoryTrackingBuffer) builder).freeAndDeleteBuffer();
		processFixedBuffers(MemoryTrackingBuffer::freeAndDeleteBuffer);
	}

	@FunctionalInterface
	private interface Processor {
		void process(MemoryTrackingBuffer buffer);
	}
}
