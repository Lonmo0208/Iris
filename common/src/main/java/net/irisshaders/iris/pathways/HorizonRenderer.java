package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Renders the sky horizon. Vanilla Minecraft simply uses the "clear color" for its horizon, and then draws a plane
 * above the player. This class extends the sky rendering so that an inverted octagonal cone is drawn around the player instead,
 * allowing shaders to perform more advanced sky rendering.
 * <p>
 * However, the horizon rendering is designed so that when sky shaders are not being used, it looks almost exactly the
 * same as vanilla sky rendering, except a few almost entirely imperceptible differences where the walls
 * of the inverted octagonal cone intersect the top plane.
 */
public class HorizonRenderer {
	/**
	 * The Y coordinate of the top skybox plane. Acts as the upper bound for the horizon cone, since the cone lies
	 * between the bottom and top skybox planes.
	 */
	private static final float TOP = 16.0F;

	/**
	 * The Y coordinate of the bottom skybox plane. Acts as the lower bound for the horizon cone, since the cone lies
	 * between the bottom and top skybox planes.
	 */
	private static final float BOTTOM = -16.0F;

	private final ExecutorService asyncExecutor;
	private final AtomicReference<GpuBuffer> bufferRef = new AtomicReference<>();
	private volatile int currentRenderDistance;
	private volatile int indexCount = -1;

	private volatile Future<?> pendingRebuild = null;
	private final AtomicBoolean destroyed = new AtomicBoolean(false);
	private volatile GpuBuffer fallbackBuffer = null;

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

	public HorizonRenderer() {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		int poolSize = Math.max(2, Math.min(availableProcessors, 8));

		this.asyncExecutor = Executors.newFixedThreadPool(poolSize, r -> {
			Thread thread = new Thread(r, "Iris-Horizon-Renderer-" + THREAD_COUNTER.getAndIncrement());
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY + 1);
			return thread;
		});

		currentRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
		buildAndUploadBufferSync(currentRenderDistance * 16);
	}

	private void buildAndUploadBufferSync(int radius) {
		if (destroyed.get()) {
			return;
		}

		try {
			BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
			buildHorizon(radius, buffer);
			MeshData meshData = buffer.buildOrThrow();

			GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
				() -> "Horizon",
				GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
				meshData.vertexBuffer()
			);

			this.indexCount = meshData.drawState().indexCount();

			GpuBuffer oldFallback = fallbackBuffer;
			fallbackBuffer = newBuffer;

			GpuBuffer oldBuffer = bufferRef.getAndSet(newBuffer);

			closeBufferAsync(oldBuffer);
			closeBufferAsync(oldFallback);

			meshData.close();
			Tesselator.getInstance().clear();
		} catch (Exception e) {
			Iris.logger.error("Failed to build horizon buffer synchronously: ", e);
		}
	}

	private void rebuildBufferAsync() {
		if (destroyed.get()) {
			return;
		}

		final int radius = currentRenderDistance * 16;

		buildAndUploadBufferSync(radius);

		if (asyncExecutor.isShutdown() || asyncExecutor.isTerminated()) {
			return;
		}

		try {
			if (pendingRebuild != null && !pendingRebuild.isDone()) {
				pendingRebuild.cancel(true);
			}

			pendingRebuild = asyncExecutor.submit(() -> {
				if (destroyed.get()) {
					return;
				}

				try {
					Tesselator tesselator = new Tesselator();
					BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
					buildHorizon(radius, buffer);
					MeshData meshData = buffer.buildOrThrow();
					tesselator.clear();

					Minecraft.getInstance().execute(() -> {
						if (!destroyed.get()) {
							uploadToGPUAsync(meshData);
						}
					});
				} catch (Exception e) {
					Iris.logger.warn("Failed to build horizon mesh asynchronously: ", e);
				}
			});
		} catch (Exception e) {
			Iris.logger.warn("Failed to submit horizon rebuild task: ", e);
		}
	}

	private void uploadToGPUAsync(MeshData meshData) {
		try {
			GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
				() -> "Horizon-Async",
				GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
				meshData.vertexBuffer()
			);

			int newIndexCount = meshData.drawState().indexCount();

			GpuBuffer oldBuffer = bufferRef.getAndSet(newBuffer);
			this.indexCount = newIndexCount;

			closeBufferAsync(oldBuffer);

			meshData.close();
		} catch (Exception e) {
			try {
				meshData.close();
			} catch (Exception ex) {
			}
			Iris.logger.warn("Failed to upload horizon buffer asynchronously: ", e);
		}
	}

	private void closeBufferAsync(GpuBuffer buffer) {
		if (buffer != null && !asyncExecutor.isShutdown() && !asyncExecutor.isTerminated()) {
			asyncExecutor.submit(() -> {
				try {
					buffer.close();
				} catch (Exception e) {
				}
			});
		}
	}

	private void buildHorizon(int radius, VertexConsumer consumer) {
		if (radius > 256) {
			radius = 256;
		}

		consumer.addVertex(0.0F, BOTTOM, 0.0F);

		for (int i = 0; i <= 8; i++) {
			float angle = (float) (-i * Math.PI / 4.0);
			float x = (float) (radius * Math.cos(angle));
			float z = (float) (radius * Math.sin(angle));
			consumer.addVertex(x, TOP, z);
		}
	}

	public void renderHorizon(Matrix4fc modelView, Matrix4fc projection, Vector4f fogColor) {
		if (destroyed.get()) {
			return;
		}

		int newRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();

		if (currentRenderDistance != newRenderDistance) {
			currentRenderDistance = newRenderDistance;
			rebuildBufferAsync();
		}

		GpuBuffer currentBuffer = bufferRef.get();
		if (currentBuffer == null) {
			currentBuffer = fallbackBuffer;
			if (currentBuffer == null) {
				return;
			}
		}

		if (indexCount <= 0) {
			return;
		}

		try {
			RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLE_FAN);
			GpuBuffer indexBuffer = indices.getBuffer(indexCount);
			GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().writeTransform(modelView, fogColor, new Vector3f(), new Matrix4f());

			try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sky",
				Minecraft.getInstance().getMainRenderTarget().getColorTextureView(),
				OptionalInt.empty(),
				Minecraft.getInstance().getMainRenderTarget().getDepthTextureView(),
				OptionalDouble.empty())) {

				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", gpuBufferSlice);
				pass.setVertexBuffer(0, currentBuffer);
				pass.setIndexBuffer(indexBuffer, indices.type());
				pass.setPipeline(RenderPipelines.SKY);
				pass.drawIndexed(0, 0, indexCount, 1);
			}
		} catch (Exception e) {
			Iris.logger.warn("Failed to render horizon: ", e);
		}
	}

	public void destroy() {
		destroyed.set(true);

		if (pendingRebuild != null && !pendingRebuild.isDone()) {
			pendingRebuild.cancel(true);
		}

		GpuBuffer buffer = bufferRef.getAndSet(null);
		closeBufferSync(buffer);

		if (fallbackBuffer != null) {
			closeBufferSync(fallbackBuffer);
			fallbackBuffer = null;
		}

		if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
			try {
				asyncExecutor.shutdownNow();
			} catch (Exception e) {
			}
		}
	}

	private void closeBufferSync(GpuBuffer buffer) {
		if (buffer != null) {
			try {
				buffer.close();
			} catch (Exception e) {
			}
		}
	}
}
