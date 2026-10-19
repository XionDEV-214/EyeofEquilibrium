package com.deveisia.enceladus.eyeofequilibrium.machine;

import com.deveisia.enceladus.eyeofequilibrium.EoERecipes;
import com.deveisia.enceladus.eyeofequilibrium.structure.EoEPatterns;
import com.deveisia.enceladus.eyeofequilibrium.structure.EoEStructure;
import com.deveisia.enceladus.eyeofequilibrium.ui.AstralArraySlotWidget;
import com.deveisia.enceladus.eyeofequilibrium.ui.VeinSelectorFancyConfigurator;
import com.deveisia.enceladus.eyeofequilibrium.vein.VeinUtil;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EyeOfEquilibriumMachine extends WorkableElectricMultiblockMachine
                                     implements IInteractedMachine, IDropSaveMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EyeOfEquilibriumMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final double ASTRAL_BONUS_PER_ITEM = 0.00001;
    public static final long ORE_PER_VEIN = 512_000L;
    public static final double ORE_BONUS_PER_TIER = 0.10;
    private static final String ASTRAL_TAG = "eoe_astral_arrays";
    private static final int POWER_OFF_GRACE_CHECKS = 3;

    @Persisted
    @DescSynced
    private boolean animating = false;

    @Persisted
    @DescSynced
    private int visualTier = -1;

    @Persisted
    @DescSynced
    private String selectedVeins = "";

    @Persisted
    @DescSynced
    private long astralArrayCount = 0;

    private int unpoweredChecks = 0;
    @Nullable
    private TickableSubscription tickSubscription;

    public EyeOfEquilibriumMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public boolean isAnimating() {
        return animating;
    }

    public int getVisualTier() {
        return visualTier;
    }

    public String getSelectedVeinsRaw() {
        return selectedVeins;
    }

    public long getAstralArrayCount() {
        return astralArrayCount;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (!isRemote() && tickSubscription == null) {
            tickSubscription = subscribeServerTick(this::tickServer);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (tickSubscription != null) {
            unsubscribe(tickSubscription);
            tickSubscription = null;
        }
        unpoweredChecks = 0;
        if (animating || visualTier != -1) {
            animating = false;
            visualTier = -1;
            markDirty();
        }
    }

    private void tickServer() {
        if (isRemote() || getOffsetTimer() % 20 != 0 || !isFormed()) return;

        boolean powered = hasPower() || getRecipeLogic().isWorking();
        if (powered) {
            unpoweredChecks = 0;
            if (!animating) {
                animating = true;
                markDirty();
            }
        } else if (animating && ++unpoweredChecks >= POWER_OFF_GRACE_CHECKS) {
            unpoweredChecks = 0;
            animating = false;
            markDirty();
        }

        int tier = currentRecipeVisualTier();
        if (tier != visualTier) {
            visualTier = tier;
            markDirty();
        }
    }

    private boolean hasPower() {
        EnergyContainerList containers = getEnergyContainer();
        return containers.getEnergyStored() > 0;
    }

    private int currentRecipeVisualTier() {
        if (!getRecipeLogic().isWorking()) return -1;
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        if (recipe == null || recipe.data == null || !recipe.data.contains(EoERecipes.VISUAL_TIER_KEY)) return -1;
        return recipe.data.getInt(EoERecipes.VISUAL_TIER_KEY);
    }

    public BlockPos getCenterPos() {
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), isFlipped());
        return getPos().relative(back, EoEStructure.CENTER_DISTANCE);
    }

    public double tierAverage(String prefix) {
        if (!isFormed()) return 0;
        PatternMatchContext ctx = getMultiblockState().getMatchContext();
        int count = ctx.getInt(prefix + "_count");
        return count > 0 ? (double) ctx.getInt(prefix + "_sum") / count : 0;
    }

    public double spacetimeTier() {
        return tierAverage(EoEPatterns.SPACETIME);
    }

    public double timeTier() {
        return tierAverage(EoEPatterns.TIME);
    }

    public double stabilityTier() {
        return tierAverage(EoEPatterns.STABILITY);
    }

    public double oreBonusMultiplier() {
        return 1 + ORE_BONUS_PER_TIER * (spacetimeTier() + timeTier());
    }

    public double structureOutputMultiplier() {
        return (1 + 0.25 * spacetimeTier()) * (1 + 0.5 * stabilityTier());
    }

    public double inputMultiplier() {
        return (1 - 0.1 * timeTier()) * (1 + 0.5 * stabilityTier());
    }

    public double durationMultiplier() {
        return 1 + 0.25 * stabilityTier();
    }

    public double astralMultiplier() {
        return 1 + astralArrayCount * ASTRAL_BONUS_PER_ITEM;
    }

    public double totalOutputMultiplier() {
        return structureOutputMultiplier() * astralMultiplier();
    }

    public Set<ResourceLocation> getSelectedVeinSet() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        if (selectedVeins.isEmpty()) return out;
        for (String s : selectedVeins.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) out.add(id);
        }
        return out;
    }

    public boolean isVeinSelected(ResourceLocation id) {
        return getSelectedVeinSet().contains(id);
    }

    public void toggleVein(ResourceLocation id) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        if (!set.remove(id)) set.add(id);
        applySelection(set);
    }

    public int countSelected(Collection<ResourceLocation> ids) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        int n = 0;
        for (ResourceLocation id : ids) if (set.contains(id)) n++;
        return n;
    }

    public void toggleGroup(List<ResourceLocation> members, ResourceLocation representative) {
        Set<ResourceLocation> set = getSelectedVeinSet();
        if (members.stream().anyMatch(set::contains)) set.removeAll(members);
        else set.add(representative);
        applySelection(set);
    }

    public void clearVeins() {
        applySelection(new LinkedHashSet<>());
    }

    private void applySelection(Set<ResourceLocation> set) {
        List<String> parts = new ArrayList<>();
        for (ResourceLocation id : set) parts.add(id.toString());
        this.selectedVeins = String.join(",", parts);
        if (!isRemote()) {
            markDirty();
            getRecipeLogic().markLastRecipeDirty();
            getRecipeLogic().updateTickSubscription();
        }
    }

    public static boolean isAstralArray(ItemStack stack) {
        Item item = AstralArray.item();
        return item != null && !stack.isEmpty() && stack.is(item);
    }

    public long insertAstralArrays(ItemStack stack, int amount) {
        if (!isAstralArray(stack) || amount <= 0) return 0;
        int moved = Math.min(amount, stack.getCount());
        long space = Long.MAX_VALUE - astralArrayCount;
        if (space < moved) moved = (int) space;
        if (moved <= 0) return 0;
        stack.shrink(moved);
        astralArrayCount += moved;
        markDirty();
        return moved;
    }

    public ItemStack extractAstralArrays(int amount) {
        Item item = AstralArray.item();
        if (item == null || astralArrayCount <= 0 || amount <= 0) return ItemStack.EMPTY;
        int taken = (int) Math.min(amount, astralArrayCount);
        astralArrayCount -= taken;
        markDirty();
        return new ItemStack(item, taken);
    }

    public void handleAstralSlotClick(Player player, int button, boolean shift) {
        if (isRemote()) return;
        if (shift) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (isAstralArray(stack)) {
                    insertAstralArrays(stack, stack.getCount());
                    if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
                }
            }
            inventory.setChanged();
            return;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            if (isAstralArray(carried)) {
                insertAstralArrays(carried, button == 1 ? 1 : carried.getCount());
                player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            }
            return;
        }
        Item item = AstralArray.item();
        if (item == null) return;
        int max = new ItemStack(item).getMaxStackSize();
        ItemStack taken = extractAstralArrays(button == 1 ? 1 : max);
        if (!taken.isEmpty()) player.containerMenu.setCarried(taken);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!isAstralArray(held)) return InteractionResult.PASS;
        if (!world.isClientSide) {
            insertAstralArrays(held, held.getCount());
            player.displayClientMessage(Component.translatable("eyeofequilibrium.astral_array.stored",
                    FormattingUtil.formatNumbers(astralArrayCount)), true);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Override
    public boolean savePickClone() {
        return false;
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        if (astralArrayCount > 0) tag.putLong(ASTRAL_TAG, astralArrayCount);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        if (tag.contains(ASTRAL_TAG)) {
            astralArrayCount = Math.max(0, tag.getLong(ASTRAL_TAG));
            markDirty();
        }
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(getScreenTexture())
                .addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()))
                .addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                        .textSupplier(this.getLevel().isClientSide ? null : this::addDisplayText)
                        .setMaxWidthLimit(150)
                        .clickHandler(this::handleDisplayClick)));
        group.addWidget(new AstralArraySlotWidget(4 + 182 - 24, 4 + 117 - 24, this));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new VeinSelectorFancyConfigurator(this));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        textList.add(Component.translatable("eyeofequilibrium.multiblock.spacetime_tier",
                format(spacetimeTier())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.time_tier",
                format(timeTier())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.stability_tier",
                format(stabilityTier())).withStyle(ChatFormatting.GRAY));

        textList.add(Component.translatable("eyeofequilibrium.multiblock.ore_bonus",
                format(oreBonusMultiplier())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.input",
                format(inputMultiplier())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.output",
                format(structureOutputMultiplier())).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.duration",
                format(durationMultiplier())).withStyle(ChatFormatting.GRAY));

        textList.add(Component.translatable("eyeofequilibrium.multiblock.astral_arrays",
                FormattingUtil.formatNumbers(astralArrayCount),
                String.format("%.3f", (astralMultiplier() - 1) * 100)).withStyle(ChatFormatting.LIGHT_PURPLE));
        textList.add(Component.translatable("eyeofequilibrium.multiblock.total_output",
                format(totalOutputMultiplier())).withStyle(ChatFormatting.AQUA));

        int veins = 0;
        for (ResourceLocation id : getSelectedVeinSet()) {
            if (!VeinUtil.materials(VeinUtil.get(id, isRemote())).isEmpty()) veins++;
        }
        if (veins == 0) {
            textList.add(Component.translatable("eyeofequilibrium.multiblock.no_selection")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            textList.add(Component.translatable("eyeofequilibrium.multiblock.selected_veins", veins)
                    .withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("eyeofequilibrium.multiblock.ore_per_cycle",
                    FormattingUtil.formatNumbers(ORE_PER_VEIN)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }
}
