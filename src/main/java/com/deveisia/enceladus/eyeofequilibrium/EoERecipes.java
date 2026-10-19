package com.deveisia.enceladus.eyeofequilibrium;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class EoERecipes {

    private EoERecipes() {}

    public static final int DURATION = 5 * 60 * 60 * 20;
    public static final int INPUT_SCALE = 100;
    public static final int OUTPUT_SCALE = 25;
    public static final String VISUAL_TIER_KEY = "eoe_visual_tier";

    private record Entry(String name, String fluid, int whiteDwarf, int blackDwarf, int universium,
                         int rawStellar, int voltage) {}

    private static final Entry[] ENTRIES = {
            new Entry("cosmic_neutronium", "cosmic_neutronium", 4, 4, 2, 144, GTValues.LuV),
            new Entry("infinity", "infinity", 12, 12, 6, 288, GTValues.ZPM),
            new Entry("metastable_oganesson", "metastable_oganesson", 36, 36, 9, 576, GTValues.UV),
            new Entry("rhugnor", "rhugnor", 72, 72, 18, 1152, GTValues.UHV),
            new Entry("hypogen", "hypogen", 144, 144, 36, 2304, GTValues.UHV),
            new Entry("transdimensional_residue", "transdimensional_residue", 288, 288, 72, 4608, GTValues.UEV),
            new Entry("spacetime", "spacetime", 576, 576, 144, 9216, GTValues.UEV),
            new Entry("shirabon", "shirabon", 1152, 1152, 288, 18432, GTValues.UIV),
            new Entry("mellion", "mellion", 2304, 2304, 576, 36864, GTValues.UXV),
            new Entry("redux", "redux", 4608, 4608, 1152, 73728, GTValues.OpV)
    };

    private static final int QUANTUM_ANOMALY = 2;
    private static final int FLUID_INPUT = 288;

    public static void init(Consumer<FinishedRecipe> provider) {
        if (EoERecipeTypes.EYE_OF_EQUILIBRIUM == null) return;
        Item anomaly = item("quantum_anomaly");
        if (anomaly == null) {
            EyeOfEquilibriumMod.LOGGER.error("[EyeOfEquilibrium] gtceu:quantum_anomaly is missing, recipes skipped");
            return;
        }
        Fluid whiteDwarf = fluid("white_dwarf_matter");
        Fluid blackDwarf = fluid("black_dwarf_matter");
        Fluid universium = fluid("universium");
        Fluid rawStellar = fluid("raw_stellar_matter");
        if (whiteDwarf == null || blackDwarf == null || universium == null || rawStellar == null) {
            EyeOfEquilibriumMod.LOGGER.error("[EyeOfEquilibrium] Output fluids are missing, recipes skipped");
            return;
        }

        for (int i = 0; i < ENTRIES.length; i++) {
            Entry e = ENTRIES[i];
            Fluid input = fluid(e.fluid());
            if (input == null) {
                EyeOfEquilibriumMod.LOGGER.warn("[EyeOfEquilibrium] gtceu:{} is missing, recipe {} skipped",
                        e.fluid(), e.name());
                continue;
            }
            GTRecipeBuilder builder = EoERecipeTypes.EYE_OF_EQUILIBRIUM
                    .recipeBuilder(EyeOfEquilibriumMod.id(e.name() + "_stellar_matter"))
                    .inputItems(new ItemStack(anomaly, QUANTUM_ANOMALY * INPUT_SCALE))
                    .inputFluids(new FluidStack(input, FLUID_INPUT * INPUT_SCALE))
                    .outputFluids(
                            new FluidStack(whiteDwarf, e.whiteDwarf() * OUTPUT_SCALE),
                            new FluidStack(blackDwarf, e.blackDwarf() * OUTPUT_SCALE),
                            new FluidStack(universium, e.universium() * OUTPUT_SCALE),
                            new FluidStack(rawStellar, e.rawStellar() * OUTPUT_SCALE))
                    .EUt(GTValues.VA[e.voltage()])
                    .duration(DURATION)
                    .addData(VISUAL_TIER_KEY, i);
            builder.save(provider);
        }
    }

    @Nullable
    private static Item item(String path) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtceu", path));
        return item == null || item == Items.AIR ? null : item;
    }

    @Nullable
    private static Fluid fluid(String path) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("gtceu", path));
        return fluid == null || fluid == Fluids.EMPTY ? null : fluid;
    }
}
