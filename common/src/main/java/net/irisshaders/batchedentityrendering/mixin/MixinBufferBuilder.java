package net.irisshaders.batchedentityrendering.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

/**
 * Mixin for BufferBuilder to implement MemoryTrackingBuffer.
 * Provides methods to track memory usage and manage buffer lifecycle.
 */
@Mixin(BufferBuilder.class)
public class MixinBufferBuilder implements MemoryTrackingBuffer {

    @Shadow
    private ByteBuffer buffer;

    /**
     * Returns the total allocated size of the buffer in bytes.
     * @return The capacity of the buffer.
     */
    @Override
    public int getAllocatedSize() {
        return buffer != null ? buffer.capacity() : 0;
    }

    /**
     * Returns the number of bytes currently used by the buffer.
     * @return The current position of the buffer, indicating the used size.
     */
    @Override
    public int getUsedSize() {
        return buffer != null ? buffer.position() : 0;
    }

    /**
     * Frees the native memory allocated for the buffer and sets the buffer to null.
     * This method should be called when the buffer is no longer needed to release resources.
     */
    @Override
    public void freeAndDeleteBuffer() {
        if (buffer != null) {
            // Free the native memory
            MemoryUtil.getAllocator(false).free(MemoryUtil.memAddress(buffer));
            // Set buffer to null to prevent reuse
            buffer = null;
        }
    }
}
