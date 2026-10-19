package com.deveisia.enceladus.eyeofequilibrium.client;

import com.deveisia.enceladus.eyeofequilibrium.machine.EyeOfEquilibriumMachine;
import com.deveisia.enceladus.eyeofequilibrium.vein.VeinUtil;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.client.ClientProxy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class EoEOrbits {

    private EoEOrbits() {}

    public static final int IDLE_TIER = 9;
    public static final double IDLE_STAR_SIZE = 1.0;
    private static final float MAX_ANGLE = 30f;
    private static final float MIN_DISTANCE = 2.0f;
    private static final float MAX_DISTANCE = 11.0f;   // 우주 껍질(반지름 약 12.95) 안쪽에 머물도록

    /** planet != null 이면 GTNH 행성 텍스처 큐브, 아니면 state(광맥 원석 블록)를 그린다. */
    public record OrbitingObject(@Nullable String planet, @Nullable BlockState state, float distance, float rotationSpeed, float orbitSpeed,
                                 float xAngle, float zAngle, float scale) {}

    public record State(String signature, int tier, double starSize, List<OrbitingObject> objects) {}

    private static final Map<EyeOfEquilibriumMachine, State> CACHE = new WeakHashMap<>();

    /**
     * GTNH 가 Eye of Harmony 에서 쓰는 행성 목록 (gtneioreplugin 의 차원 코드).
     * GTNH 블랙리스트(Ow, ED, EA, VA)는 제외했고, 텍스처는 textures/planet/<코드>_<면>.png 로 들어 있다.
     */
    private static final List<String> PLANETS = List.of(
            "An", "As", "BC", "BE", "BF", "CB", "Ca", "Ce",
            "DD", "De", "En", "Eu", "Ga", "Ha", "Ho", "Io",
            "KB", "MB", "MM", "Ma", "Me", "Mh", "Mi", "Mo",
            "Ne", "Np", "Ob", "Ph", "Pl", "Pr", "Ra", "Rb",
            "Se", "TE", "TF", "Ti", "Tr", "VB", "Ve");

    public static State get(EyeOfEquilibriumMachine machine) {
        int recipeTier = machine.getVisualTier();
        String veins = machine.getSelectedVeinsRaw();
        String signature = recipeTier + "|" + veins + "|" + ClientProxy.CLIENT_ORE_VEINS.size();
        State cached = CACHE.get(machine);
        if (cached != null && cached.signature().equals(signature)) return cached;

        int tier = recipeTier < 0 ? IDLE_TIER : recipeTier;
        double starSize = recipeTier < 0 ? IDLE_STAR_SIZE : 0.4 + Math.min(recipeTier, 8) / 8.0;
        long seed = machine.getPos().asLong() * 31L + Objects.hash(veins, ClientProxy.CLIENT_ORE_VEINS.size());
        State state = new State(signature, tier, starSize, generate(machine, new Random(seed)));
        CACHE.put(machine, state);
        return state;
    }

    private static List<OrbitingObject> generate(EyeOfEquilibriumMachine machine, Random rand) {
        // 레시피 티어와 상관없이 항상 모든 행성을 렌더링한다.
        List<String> planets = new ArrayList<>(PLANETS);

        // 선택된 광맥의 원석 블록도 (있다면) 추가 천체로 함께 돈다.
        Set<Block> veinBlocks = new LinkedHashSet<>();
        for (ResourceLocation id : machine.getSelectedVeinSet()) {
            for (Material material : VeinUtil.materials(VeinUtil.get(id, true))) {
                Block block = ChemicalHelper.getBlock(TagPrefix.rawOreBlock, material);
                if (block != null && block != Blocks.AIR) veinBlocks.add(block);
            }
        }

        // 행성과 광맥 블록을 섞어서 궤도 순서를 정한다.
        List<Object> bodies = new ArrayList<>(planets);
        bodies.addAll(veinBlocks);
        Collections.shuffle(bodies, rand);

        int n = bodies.size();
        List<OrbitingObject> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Object body = bodies.get(i);
            float xAngle = range(rand, -MAX_ANGLE, MAX_ANGLE);
            float zAngle = range(rand, -MAX_ANGLE, MAX_ANGLE);
            // 천체가 많아도 우주 껍질 밖으로 나가지 않도록 궤도 반경을 [MIN, MAX] 에 균등 배치
            float t = n > 1 ? (float) i / (n - 1) : 0f;
            float distance = MIN_DISTANCE + t * (MAX_DISTANCE - MIN_DISTANCE) + range(rand, -0.1f, 0.1f);
            float scale = range(rand, 0.2f, 0.7f);
            float rotationSpeed = range(rand, 0.5f, 1.5f);
            float orbitSpeed = range(rand, 0.5f, 1.5f);
            if (body instanceof String dim) {
                out.add(new OrbitingObject(dim, null, distance, rotationSpeed, orbitSpeed, xAngle, zAngle, scale));
            } else {
                out.add(new OrbitingObject(null, ((Block) body).defaultBlockState(), distance, rotationSpeed,
                        orbitSpeed, xAngle, zAngle, scale));
            }
        }
        return out;
    }

    private static float range(Random rand, float a, float b) {
        return rand.nextFloat() * (b - a) + a;
    }
}
