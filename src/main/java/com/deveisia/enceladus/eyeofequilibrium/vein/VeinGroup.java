package com.deveisia.enceladus.eyeofequilibrium.vein;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record VeinGroup(ResourceLocation representative, List<ResourceLocation> members, int ownCount) {}
