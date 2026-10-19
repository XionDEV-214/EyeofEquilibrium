package com.deveisia.enceladus.eyeofequilibrium.client;

import com.deveisia.enceladus.eyeofequilibrium.EyeOfEquilibriumMod;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class EoEMeshes {

    private EoEMeshes() {}

    private static final int SPHERE_SLICES = 64;
    private static final int SPHERE_STACKS = 64;

    @Nullable private static VertexBuffer sphere;
    private static final Map<BlockState, VertexBuffer> BLOCKS = new HashMap<>();
    private static final Set<BlockState> FAILED = new HashSet<>();
    private static final Map<String, VertexBuffer> PLANETS = new HashMap<>();

    // GTNH(gtneioreplugin) 행성 블록의 면 순서: 0 bottom, 1 top, 2 back, 3 front, 4 left, 5 right
    private static final String[] PLANET_FACES = { "bottom", "top", "back", "front", "left", "right" };

    public static void release() {
        if (sphere != null) {
            sphere.close();
            sphere = null;
        }
        for (VertexBuffer vb : BLOCKS.values()) vb.close();
        BLOCKS.clear();
        FAILED.clear();
        for (VertexBuffer vb : PLANETS.values()) vb.close();
        PLANETS.clear();
    }

    public static VertexBuffer sphere() {
        if (sphere == null) {
            BufferBuilder bb = new BufferBuilder(SPHERE_SLICES * SPHERE_STACKS * 6 * 20);
            bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            for (int i = 0; i < SPHERE_STACKS; i++) {
                double v0 = (double) i / SPHERE_STACKS;
                double v1 = (double) (i + 1) / SPHERE_STACKS;
                double phi0 = Math.PI / 2.0 - i * Math.PI / SPHERE_STACKS;
                double phi1 = Math.PI / 2.0 - (i + 1) * Math.PI / SPHERE_STACKS;
                double y0 = Math.sin(phi0), y1 = Math.sin(phi1);
                double r0 = Math.cos(phi0), r1 = Math.cos(phi1);
                for (int j = 0; j < SPHERE_SLICES; j++) {
                    float uu0 = (float) (1.0 - (double) j / SPHERE_SLICES);
                    float uu1 = (float) (1.0 - (double) (j + 1) / SPHERE_SLICES);
                    double th0 = j * 2.0 * Math.PI / SPHERE_SLICES;
                    double th1 = (j + 1) * 2.0 * Math.PI / SPHERE_SLICES;
                    double x00 = r0 * Math.cos(th0), z00 = r0 * Math.sin(th0);
                    double x10 = r1 * Math.cos(th0), z10 = r1 * Math.sin(th0);
                    double x11 = r1 * Math.cos(th1), z11 = r1 * Math.sin(th1);
                    double x01 = r0 * Math.cos(th1), z01 = r0 * Math.sin(th1);

                    vertex(bb, x00, y0, z00, uu0, (float) v0);
                    vertex(bb, x10, y1, z10, uu0, (float) v1);
                    vertex(bb, x11, y1, z11, uu1, (float) v1);

                    vertex(bb, x00, y0, z00, uu0, (float) v0);
                    vertex(bb, x11, y1, z11, uu1, (float) v1);
                    vertex(bb, x01, y0, z01, uu1, (float) v0);
                }
            }
            sphere = upload(bb);
        }
        return sphere;
    }

    private static void vertex(BufferBuilder bb, double x, double y, double z, float u, float v) {
        bb.vertex(x, y, z).uv(u, v).endVertex();
    }


    /** GTNH 전용 행성 텍스처(면마다 다른 텍스처)를 입힌 단위 큐브. 중심이 원점. */
    public static VertexBuffer planet(String dimension) {
        return PLANETS.computeIfAbsent(dimension, EoEMeshes::buildPlanet);
    }

    private static VertexBuffer buildPlanet(String dim) {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite[] sp = new TextureAtlasSprite[6];
        for (int i = 0; i < 6; i++) {
            sp[i] = atlas.getSprite(EyeOfEquilibriumMod.id("planet/" + dim.toLowerCase(java.util.Locale.ROOT) + "_" + PLANET_FACES[i]));
        }
        final float a = -0.5f, b = 0.5f;
        // 면별 정점 (바닐라 FaceInfo와 같은 반시계 순서): DOWN, UP, NORTH, SOUTH, WEST, EAST
        float[][] pos = {
                { a, a, b,  a, a, a,  b, a, a,  b, a, b },   // 0 bottom
                { a, b, a,  a, b, b,  b, b, b,  b, b, a },   // 1 top
                { b, b, a,  b, a, a,  a, a, a,  a, b, a },   // 2 back  (north)
                { a, b, b,  a, a, b,  b, a, b,  b, b, b },   // 3 front (south)
                { a, b, a,  a, a, a,  a, a, b,  a, b, b },   // 4 left  (west)
                { b, b, b,  b, a, b,  b, a, a,  b, b, a }    // 5 right (east)
        };
        BufferBuilder bb = new BufferBuilder(6 * 4 * 20);
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int f = 0; f < 6; f++) {
            TextureAtlasSprite t = sp[f];
            float[] u = { t.getU0(), t.getU0(), t.getU1(), t.getU1() };
            float[] v = { t.getV0(), t.getV1(), t.getV1(), t.getV0() };
            for (int i = 0; i < 4; i++) {
                bb.vertex(pos[f][i * 3], pos[f][i * 3 + 1], pos[f][i * 3 + 2]).uv(u[i], v[i]).endVertex();
            }
        }
        return upload(bb);
    }

    @Nullable
    public static VertexBuffer block(BlockState state) {
        VertexBuffer cached = BLOCKS.get(state);
        if (cached != null || FAILED.contains(state)) return cached;
        try {
            VertexBuffer built = buildBlock(state);
            BLOCKS.put(state, built);
            return built;
        } catch (Exception e) {
            FAILED.add(state);
            EyeOfEquilibriumMod.LOGGER.error("[EyeOfEquilibrium] Failed to bake orbit mesh for {}", state, e);
            return null;
        }
    }

    private static VertexBuffer buildBlock(BlockState state) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            random.setSeed(42L);
            quads.addAll(model.getQuads(state, dir, random, ModelData.EMPTY, null));
        }
        random.setSeed(42L);
        quads.addAll(model.getQuads(state, null, random, ModelData.EMPTY, null));

        BufferBuilder bb = new BufferBuilder(Math.max(256, quads.size() * 4 * 20));
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (BakedQuad q : quads) {
            int[] v = q.getVertices();
            for (int i = 0; i < 4; i++) {
                int o = i * 8;
                float x = Float.intBitsToFloat(v[o]) - 0.5f;
                float y = Float.intBitsToFloat(v[o + 1]) - 0.5f;
                float z = Float.intBitsToFloat(v[o + 2]) - 0.5f;
                float u = Float.intBitsToFloat(v[o + 4]);
                float w = Float.intBitsToFloat(v[o + 5]);
                bb.vertex(x, y, z).uv(u, w).endVertex();
            }
        }
        return upload(bb);
    }

    private static VertexBuffer upload(BufferBuilder bb) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(bb.end());
        VertexBuffer.unbind();
        return vb;
    }
}
