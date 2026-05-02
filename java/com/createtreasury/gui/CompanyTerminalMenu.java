package com.createtreasury.gui;

import com.createtreasury.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CompanyTerminalMenu extends AbstractContainerMenu {

    public CompanyTerminalMenu(int containerId, Inventory inv, FriendlyByteBuf buf) {
        this(containerId, inv);
    }

    public CompanyTerminalMenu(int containerId, Inventory inv) {
        super(ModMenuTypes.COMPANY_TERMINAL.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
