package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.irisshaders.iris.Iris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class HorizonRenderer {
	private static final float TOP = 16.0F;
	private static final float BOTTOM = -16.0F;

	private static final float[] OCTAGON_X = new float[9];
	private static final float[] OCTAGON_Z = new float[9];

	static {
		for (int i = 0; i <= 8; i++) {
			float angle = (float) (-i * Math.PI / 4.0);
			OCTAGON_X[i] = (float) Math.cos(angle);
			OCTAGON_Z[i] = (float) Math.sin(angle);
		}
	}

	private final ExecutorService asyncExecutor;
	private final AtomicReference<VertexBuffer> bufferRef = new AtomicReference<>();
	private volatile int currentRenderDistance;
	private volatile Future<?> pendingRebuild = null;
	private final AtomicBoolean destroyed = new AtomicBoolean(false);
	private volatile VertexBuffer fallbackBuffer = null;

	private volatile int lastBuiltRadius = -1;

	private final AtomicBoolean isRebuilding = new AtomicBoolean(false);

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

	public HorizonRenderer() {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		int poolSize = Math.max(1, Math.min(availableProcessors, 2));

		if (poolSize > 1 && availableProcessors <= 4) {
			poolSize = 1;
		}

		this.asyncExecutor = Executors.newFixedThreadPool(poolSize, r -> {
			Thread thread = new Thread(r, "Iris-Horizon-Renderer-" + THREAD_COUNTER.getAndIncrement());
			thread.setDaemon(true);
			thread.setPriority(Thread.MIN_PRIORITY);
			return thread;
		});

		currentRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
		lastBuiltRadius = currentRenderDistance * 16;
		rebuildBufferSync();
	}

	private void rebuildBufferSync() {
		if (destroyed.get()) {
			return;
		}

		try {
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
			buildHorizon(currentRenderDistance * 16, buffer);
			MeshData meshData = buffer.build();

			if (meshData != null) {
				VertexBuffer newBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
				newBuffer.bind();
				newBuffer.upload(meshData);
				VertexBuffer.unbind();

				VertexBuffer oldFallback = fallbackBuffer;
				fallbackBuffer = newBuffer;
				VertexBuffer oldBuffer = bufferRef.getAndSet(newBuffer);

				closeBufferAsync(oldBuffer);
				closeBufferAsync(oldFallback);

				meshData.close();
				lastBuiltRadius = currentRenderDistance * 16;
			}

			tesselator.clear();
		} catch (Exception e) {
			Iris.logger.error("Failed to build horizon buffer synchronously: ", e);
		}
	}

	private void rebuildBufferAsync() {
		if (destroyed.get()) {
			return;
		}

		final int radius = currentRenderDistance * 16;

		if (radius == lastBuiltRadius) {
			return;
		}

		if (!isRebuilding.compareAndSet(false, true)) {
			return;
		}

		try {
			lastBuiltRadius = radius;

			if (asyncExecutor.isShutdown() || asyncExecutor.isTerminated()) {
				return;
			}

			if (pendingRebuild != null) {
				pendingRebuild.cancel(false);
			}

			pendingRebuild = asyncExecutor.submit(() -> {
				try {
					Tesselator tesselator = new Tesselator();
					BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
					buildHorizon(radius, buffer);
					MeshData meshData = buffer.build();
					tesselator.clear();

					if (!destroyed.get() && radius == currentRenderDistance * 16) {
						Minecraft.getInstance().execute(() -> {
							if (!destroyed.get() && radius == currentRenderDistance * 16) {
								uploadToGPUAsync(meshData);
							} else if (meshData != null) {
								meshData.close();
							}
						});
					} else if (meshData != null) {
						meshData.close();
					}
				} catch (Exception e) {
					Iris.logger.warn("Failed to build horizon mesh asynchronously: ", e);
				} finally {
					isRebuilding.set(false);
				}
			});
		} catch (Exception e) {
			isRebuilding.set(false);
			Iris.logger.warn("Failed to submit horizon rebuild task: ", e);
		}
	}

	private void uploadToGPUAsync(MeshData meshData) {
		if (meshData == null) return;

		try {
			VertexBuffer newBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
			newBuffer.bind();
			newBuffer.upload(meshData);
			VertexBuffer.unbind();

			VertexBuffer oldBuffer = bufferRef.getAndSet(newBuffer);
			closeBufferAsync(oldBuffer);

			meshData.close();
		} catch (Exception e) {
			try {
				meshData.close();
			} catch (Exception ignored) {
			}
			Iris.logger.warn("Failed to upload horizon buffer asynchronously: ", e);
		}
	}

	private void buildHorizon(int radius, VertexConsumer consumer) {
		if (radius > 256) {
			radius = 256;
		}

		consumer.addVertex(0.0F, BOTTOM, 0.0F);

		for (int i = 0; i <= 8; i++) {
			float x = radius * OCTAGON_X[i];
			float z = radius * OCTAGON_Z[i];
			consumer.addVertex(x, TOP, z);
		}
	}

	public void renderHorizon(Matrix4fc modelView, Matrix4fc projection, ShaderInstance shader) {
		if (destroyed.get()) {
			return;
		}

		int newRenderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
		if (currentRenderDistance != newRenderDistance) {
			currentRenderDistance = newRenderDistance;
			rebuildBufferAsync();
		}

		VertexBuffer currentBuffer = bufferRef.get();
		if (currentBuffer == null) {
			currentBuffer = fallbackBuffer;
			if (currentBuffer == null) {
				return;
			}
		}

		try {
			currentBuffer.bind();
			currentBuffer.drawWithShader(new Matrix4f(modelView), new Matrix4f(projection), shader);
			VertexBuffer.unbind();
		} catch (Exception e) {
			Iris.logger.warn("Failed to render horizon: ", e);
		}
	}

	public void destroy() {
		destroyed.set(true);

		if (pendingRebuild != null) {
			pendingRebuild.cancel(true);
		}

		VertexBuffer buffer = bufferRef.getAndSet(null);
		closeBufferSync(buffer);

		if (fallbackBuffer != null) {
			closeBufferSync(fallbackBuffer);
			fallbackBuffer = null;
		}

		if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
			try {
				asyncExecutor.shutdownNow();
			} catch (Exception ignored) {
			}
		}
	}

	private void closeBufferSync(VertexBuffer buffer) {
		if (buffer != null) {
			try {
				buffer.close();
			} catch (Exception ignored) {
			}
		}
	}

	private void closeBufferAsync(VertexBuffer buffer) {
		if (buffer != null && !asyncExecutor.isShutdown() && !asyncExecutor.isTerminated()) {
			asyncExecutor.submit(() -> {
				try {
					buffer.close();
				} catch (Exception ignored) {
				}
			});
		}
	}
}
