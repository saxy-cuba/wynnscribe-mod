package com.wynnscribe;

import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.Nullable;

interface HoveredItem {
    @Nullable
    fun `wynnscribe$getHoveredItem`(): ItemStack
}
