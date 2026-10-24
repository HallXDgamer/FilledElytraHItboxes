package com.example.glidehitbox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Jab player elytra se glide kare, uska hitbox colour se filled dikhata hai.
 * Client-side only mod.
 */
public class FilledHitboxes implements ClientModInitializer {

    // Colour settings (0.0 - 1.0)
    private static final float RED = 1.0f;
    private static final float GREEN = 0.2f;
    private static final float BLUE = 0.2f;
    private static final float ALPHA = 0.35f;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity player = mc.player;

            // Sirf tab chale jab player glide kar raha ho
            if (player == null || !player.isGliding()) return;

            // Pehli person view mein apna hitbox dikhana ho to ye line hata do
            // if (mc.options.getPerspective().isFirstPerson()) return;

            MatrixStack matrices = context.matrixStack();
            VertexConsumerProvider consumers = context.consumers();
            if (matrices == null || consumers == null) return;

            Vec3d cam = context.camera().getPos();
            float delta = mc.getRenderTickCounter().getTickProgress(true);

            // Smooth position (frame ke beech interpolation)
            double x = MathHelper.lerp(delta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(delta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(delta, player.lastRenderZ, player.getZ());

            // Gliding pose ka asli hitbox, camera ke hisaab se shifted
            Box box = player.getDimensions(player.getPose())
                    .getBoxAt(x, y, z)
                    .offset(-cam.x, -cam.y, -cam.z);

            // Filled box
            VertexConsumer fill = consumers.getBuffer(RenderLayer.getDebugFilledBox());
            VertexRendering.drawFilledBox(
                    matrices, fill,
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    RED, GREEN, BLUE, ALPHA
            );

            // Outline (border) bhi, solid colour mein
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            VertexRendering.drawBox(matrices, lines, box, RED, GREEN, BLUE, 1.0f);
        });
    }
}
