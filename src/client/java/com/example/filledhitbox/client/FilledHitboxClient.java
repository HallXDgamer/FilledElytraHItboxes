package com.example.filledhitbox.client;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/**
 * Elytra glide ke time player ka hitbox colour se filled dikhata hai.
 * Colour upar constants mein set hai.
 * Minecraft 1.21.11, Mojang mappings.
 */
public class FilledHitboxClient implements ClientModInitializer {

	// Colour (0.0 - 1.0), yahan badal sakte ho
	private static final float RED = 1.0f;
	private static final float GREEN = 0.2f;
	private static final float BLUE = 0.2f;
	private static final float ALPHA = 0.35f;

	// true karoge to apna khud ka hitbox bhi dikhega
	private static final boolean SHOW_SELF = false;

	private static final RenderPipeline FILLED_BOX = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("filled-hitbox", "pipeline/glide_hitbox_filled"))
					.build()
	);

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private BufferBuilder buffer;
	private MappableRingBuffer vertexBuffer;

	@Override
	public void onInitializeClient() {
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::onRender);
	}

	private void onRender(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;

		float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		PoseStack matrices = context.matrices();
		Vec3 camera = context.worldState().cameraRenderState.pos;

		buffer = null;

		for (AbstractClientPlayer target : mc.level.players()) {
			if (target == mc.player && !SHOW_SELF) continue;
			if (!target.isFallFlying()) continue;

			// Smooth position (frame ke beech interpolation)
			double dx = Mth.lerp(delta, target.xOld, target.getX()) - target.getX();
			double dy = Mth.lerp(delta, target.yOld, target.getY()) - target.getY();
			double dz = Mth.lerp(delta, target.zOld, target.getZ()) - target.getZ();

			// Player ka asli (gliding pose wala) hitbox
			AABB box = target.getBoundingBox().move(dx, dy, dz);

			if (buffer == null) {
				buffer = new BufferBuilder(ALLOCATOR, FILLED_BOX.getVertexFormatMode(), FILLED_BOX.getVertexFormat());
			}

			matrices.pushPose();
			matrices.translate(-camera.x, -camera.y, -camera.z);
			addFilledBox(matrices.last().pose(), buffer,
					(float) box.minX, (float) box.minY, (float) box.minZ,
					(float) box.maxX, (float) box.maxY, (float) box.maxZ,
					RED, GREEN, BLUE, ALPHA);
			matrices.popPose();
		}

		// Koi bhi player glide nahi kar raha to kuch draw nahi hota
		if (buffer != null) {
			draw(mc);
		}
	}

	private void addFilledBox(Matrix4fc m, BufferBuilder b,
			float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
			float r, float g, float bl, float a) {
		// Front
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		// Back
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		// Left
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		// Right
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		// Top
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		// Bottom
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
	}

	private void draw(Minecraft client) {
		MeshData built = buffer.buildOrThrow();
		MeshData.DrawState drawState = built.drawState();
		VertexFormat format = drawState.format();

		// Upload vertices
		int vertexBufferSize = drawState.vertexCount() * format.getVertexSize();
		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}
			vertexBuffer = new MappableRingBuffer(() -> "filled-hitbox vertex buffer",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
		}

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView view = encoder.mapBuffer(
				vertexBuffer.currentBuffer().slice(0, built.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(built.vertexBuffer(), view.data());
		}
		GpuBuffer vertices = vertexBuffer.currentBuffer();

		// Indices (quads)
		built.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
		GpuBuffer indices = FILLED_BOX.getVertexFormat().uploadImmediateIndexBuffer(built.indexBuffer());
		VertexFormat.IndexType indexType = built.drawState().indexType();

		// Draw
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "filled-hitbox render pass",
						client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
						client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(FILLED_BOX);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.setVertexBuffer(0, vertices);
			pass.setIndexBuffer(indices, indexType);
			pass.drawIndexed(0, 0, drawState.indexCount(), 1);
		}

		built.close();
		vertexBuffer.rotate();
		buffer = null;
	}
}
