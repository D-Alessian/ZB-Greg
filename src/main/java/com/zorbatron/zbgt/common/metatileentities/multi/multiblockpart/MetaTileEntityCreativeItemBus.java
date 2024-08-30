package com.zorbatron.zbgt.common.metatileentities.multi.multiblockpart;

import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.NotNull;

import com.zorbatron.zbgt.api.capability.impl.InfiniteItemStackHandler;
import com.zorbatron.zbgt.client.widgets.PhantomSlotNoTextWidget;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.IGhostSlotConfigurable;
import gregtech.api.capability.impl.GhostCircuitItemStackHandler;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.GhostCircuitSlotWidget;
import gregtech.api.gui.widgets.SlotWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.util.Position;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockNotifiablePart;

public class MetaTileEntityCreativeItemBus extends MetaTileEntityMultiblockNotifiablePart implements
                                           IMultiblockAbilityPart<IItemHandlerModifiable>, IGhostSlotConfigurable {

    private InfiniteItemStackHandler infiniteItemStackHandler;
    private GhostCircuitItemStackHandler circuitItemStackHandler;
    private ItemHandlerList actualImportItems;

    public MetaTileEntityCreativeItemBus(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GTValues.MAX, false);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityCreativeItemBus(metaTileEntityId);
    }

    @Override
    protected void initializeInventory() {
        this.infiniteItemStackHandler = new InfiniteItemStackHandler(this, 16, false);
        this.circuitItemStackHandler = new GhostCircuitItemStackHandler(this);
        this.actualImportItems = new ItemHandlerList(
                Arrays.asList(this.infiniteItemStackHandler, this.circuitItemStackHandler));

        super.initializeInventory();
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, 18 + 18 * 4 + 94)
                .label(6, 6, getMetaFullName());

        WidgetGroup slots = new WidgetGroup(new Position(52, 18));

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int index = y * 4 + x;

                slots.addWidget(new PhantomSlotNoTextWidget(infiniteItemStackHandler, index, x * 18, y * 18)
                        .setClearSlotOnRightClick(true).setBackgroundTexture(GuiTextures.SLOT));
            }
        }

        SlotWidget circuitSlot = new GhostCircuitSlotWidget(circuitItemStackHandler, 0, 151, 72)
                .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY);
        builder.widget(circuitSlot.setConsumer(this::getCircuitSlotTooltip));

        return builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 7, 102)
                .widget(slots)
                .build(getHolder(), entityPlayer);
    }

    protected void getCircuitSlotTooltip(@NotNull SlotWidget widget) {
        String configString;
        if (circuitItemStackHandler.getCircuitValue() == GhostCircuitItemStackHandler.NO_CONFIG) {
            configString = new TextComponentTranslation("gregtech.gui.configurator_slot.no_value").getFormattedText();
        } else {
            configString = String.valueOf(circuitItemStackHandler.getCircuitValue());
        }

        widget.setTooltipText("gregtech.gui.configurator_slot.tooltip", configString);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);

        Textures.ITEM_HATCH_INPUT_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return this.actualImportItems;
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return this.infiniteItemStackHandler;
    }

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return MultiblockAbility.IMPORT_ITEMS;
    }

    @Override
    public void registerAbilities(List<IItemHandlerModifiable> abilityList) {
        abilityList.add(this.importItems);
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controllerBase) {
        super.addToMultiBlock(controllerBase);

        this.infiniteItemStackHandler.addNotifiableMetaTileEntity(controllerBase);
        this.infiniteItemStackHandler.addToNotifiedList(this, controllerBase, false);

        this.circuitItemStackHandler.addNotifiableMetaTileEntity(controllerBase);
        this.circuitItemStackHandler.addToNotifiedList(this, controllerBase, false);
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controllerBase) {
        super.removeFromMultiBlock(controllerBase);

        this.infiniteItemStackHandler.removeNotifiableMetaTileEntity(controllerBase);

        this.circuitItemStackHandler.removeNotifiableMetaTileEntity(controllerBase);
    }

    @Override
    public boolean hasGhostCircuitInventory() {
        return true;
    }

    @Override
    public void setGhostCircuitConfig(int config) {
        if (this.circuitItemStackHandler.getCircuitValue() == config) {
            return;
        }
        this.circuitItemStackHandler.setCircuitValue(config);
        if (!getWorld().isRemote) {
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.circuitItemStackHandler.write(data);

        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);

        this.circuitItemStackHandler.read(data);
    }
}
