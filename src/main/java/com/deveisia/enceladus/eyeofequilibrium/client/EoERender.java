package com.deveisia.enceladus.eyeofequilibrium.client;

import com.deveisia.enceladus.eyeofequilibrium.EyeOfEquilibriumMod;
import com.deveisia.enceladus.eyeofequilibrium.machine.EyeOfEquilibriumMachine;
import com.deveisia.enceladus.eyeofequilibrium.structure.EoEStructure;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.serialization.Codec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class EoERender extends DynamicRender<EyeOfEquilibriumMachine, EoERender> {

    public static final Codec<EoERender> CODEC = Codec.unit(EoERender::new);
    public static final DynamicRenderType<EyeOfEquilibriumMachine, EoERender> TYPE = new DynamicRenderType<>(CODEC);

    private static final ResourceLocation[] STAR_LAYERS = {
            EyeOfEquilibriumMod.id("textures/model/star_layer_0.png"),
            EyeOfEquilibriumMod.id("textures/model/star_layer_1.png"),
            EyeOfEquilibriumMod.id("textures/model/star_layer_2.png")
    };
    private static final ResourceLocation SPACE_LAYER = EyeOfEquilibriumMod.id("textures/model/space_layer.png");

    private static final float STAR_R = 255f / 255f, STAR_G = 102f / 255f, STAR_B = 13f / 255f;
    private static final float[] ROTATION_SPEEDS = { 1.5f, 1.2f, 1.6f };
    private static final float[] BASE_ROTATIONS = { 130f, -49f, 67f };
    private static final float[] LAYER_ALPHA = { 1.0f, 0.4f, 0.2f };
    private static final Vector3f[] LAYER_AXIS = {
            new Vector3f(0, 1, 1).normalize(),
            new Vector3f(1, 1, 0).normalize(),
            new Vector3f(1, 0, 1).normalize()
    };

    private static final float STAR_RESCALE = 0.2f;
    /** 천체 공전/자전 속도 배율 (기존 0.1 -> 0.3, 약 3배). 더 빠르게/느리게 하려면 이 값만 조정. */
    private static final float SPEED_SCALE = 0.3f;
    private static final float SHELL_SCALE = 0.01f * 17.5f * 74f;
    private static final float DEG_TO_RAD = (float) Math.PI / 180f;

    private enum Blend { NONE, ALPHA, ADDITIVE }

    public EoERender() {}

    @Override
    public DynamicRenderType<EyeOfEquilibriumMachine, EoERender> getType() {
        return TYPE;
    }

    private static boolean active(EyeOfEquilibriumMachine machine) {
        return machine.isFormed() && machine.isAnimating();
    }

    @Override
    public boolean shouldRender(EyeOfEquilibriumMachine machine, Vec3 cameraPos) {
        if (!active(machine)) return false;
        return Vec3.atCenterOf(machine.getCenterPos()).closerThan(cameraPos, getViewDistance());
    }

    @Override
    public boolean shouldRenderOffScreen(EyeOfEquilibriumMachine machine) {
        return active(machine);
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public AABB getRenderBoundingBox(EyeOfEquilibriumMachine machine) {
        AABB eye = new AABB(machine.getCenterPos()).inflate(SHELL_SCALE + 2);
        return eye.minmax(new AABB(machine.getPos()));
    }

    @Override
    public void render(EyeOfEquilibriumMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!active(machine) || !EoEShaders.ready()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        double time = (double) mc.level.getGameTime() + partialTick;
        EoEOrbits.State state = EoEOrbits.get(machine);

        Direction back = RelativeDirection.BACK.getRelative(machine.getFrontFacing(), machine.getUpwardsFacing(),
                machine.isFlipped());
        int d = EoEStructure.CENTER_DISTANCE;

        poseStack.pushPose();
        poseStack.translate(0.5 + back.getStepX() * d, 0.5 + back.getStepY() * d, 0.5 + back.getStepZ() * d);
        Matrix4f eye = new Matrix4f(poseStack.last().pose());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        try {
            renderOuterSpaceShell(eye);
            renderOrbits(eye, state, time);
            renderStar(eye, state.starSize(), time);
        } finally {
            GL11.glCullFace(GL11.GL_BACK);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            poseStack.popPose();
        }
    }

    private void renderOuterSpaceShell(Matrix4f eye) {
        ShaderInstance shader = EoEShaders.textured();
        shader.safeGetUniform("Tint").set(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, SPACE_LAYER);
        Matrix4f shell = new Matrix4f(eye).scale(-SHELL_SCALE, SHELL_SCALE, SHELL_SCALE);
        GL11.glCullFace(GL11.GL_FRONT);
        draw(EoEMeshes.sphere(), shader, shell, Blend.NONE);
    }

    private void renderOrbits(Matrix4f eye, EoEOrbits.State state, double time) {
        if (state.objects().isEmpty()) return;
        ShaderInstance shader = EoEShaders.orbit();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        GL11.glCullFace(GL11.GL_BACK);

        float starSize = (float) state.starSize();
        for (EoEOrbits.OrbitingObject obj : state.objects()) {
            VertexBuffer mesh = obj.planet() != null ? EoEMeshes.planet(obj.planet()) : EoEMeshes.block(obj.state());
            if (mesh == null) continue;

            float orbitAngle = (float) ((obj.orbitSpeed() * SPEED_SCALE * time) % 360f);
            float spinAngle = (float) ((obj.rotationSpeed() * SPEED_SCALE * time) % 360f);
            float distance = -0.2f - obj.distance() - STAR_RESCALE * starSize;

            Matrix4f model = new Matrix4f(eye)
                    .rotateZ(obj.zAngle() * DEG_TO_RAD)
                    .rotateX(obj.xAngle() * DEG_TO_RAD)
                    .rotateY(orbitAngle * DEG_TO_RAD)
                    .translate(distance, 0, 0)
                    .rotateY(spinAngle * DEG_TO_RAD)
                    .scale(obj.scale());
            draw(mesh, shader, model, Blend.NONE);
        }
    }

    private void renderStar(Matrix4f eye, double starRadius, double time) {
        ShaderInstance shader = EoEShaders.textured();
        Matrix4f starBase = new Matrix4f(eye).rotateY((float) Math.PI);
        GL11.glCullFace(GL11.GL_FRONT);

        for (int layer = 0; layer < 3; layer++) {
            float alpha = LAYER_ALPHA[layer];
            float rotation = (float) ((BASE_ROTATIONS[layer] + ROTATION_SPEEDS[layer] * time) % 360f);
            float scale = (float) (starRadius * Math.pow(0.95f, 2 - layer));
            Vector3f axis = LAYER_AXIS[layer];

            Matrix4f model = new Matrix4f(starBase)
                    .rotate(rotation * DEG_TO_RAD, axis.x, axis.y, axis.z)
                    .scale(scale);

            RenderSystem.setShaderTexture(0, STAR_LAYERS[layer]);
            shader.safeGetUniform("Tint").set(STAR_R, STAR_G, STAR_B, alpha);
            draw(EoEMeshes.sphere(), shader, model, alpha < 1.0f ? Blend.ADDITIVE : Blend.ALPHA);
        }
    }

    private static void draw(VertexBuffer mesh, ShaderInstance shader, Matrix4f modelView, Blend blend) {
        shader.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(modelView);
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        shader.apply();
        switch (blend) {
            case NONE -> RenderSystem.disableBlend();
            case ALPHA -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            case ADDITIVE -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            }
        }
        mesh.bind();
        mesh.draw();
        shader.clear();
        VertexBuffer.unbind();
    }
}
