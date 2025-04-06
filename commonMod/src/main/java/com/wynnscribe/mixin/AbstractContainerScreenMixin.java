package com.wynnscribe.mixin;

import com.wynnscribe.HoveredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin implements HoveredItem {

    @Shadow
    protected Slot hoveredSlot;

    @Override
    public ItemStack wynnscribe$getHoveredItem() {
        if(hoveredSlot != null) {
            return hoveredSlot.getItem();
        }
        return null;
    }
}
