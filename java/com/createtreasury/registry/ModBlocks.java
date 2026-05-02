package com.createtreasury.registry;

import com.createtreasury.TreasuryMod;
import com.createtreasury.block.BrassATMBlock;
import com.createtreasury.block.BrassCashRegisterBlock;
import com.createtreasury.block.CardEngraverBlock;
import com.createtreasury.block.CardPressBlock;
import com.createtreasury.block.CompanyTerminalBlock;
import com.createtreasury.block.MintingPressBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TreasuryMod.MOD_ID);

    public static final RegistryObject<Block> COMPANY_TERMINAL = register("company_terminal",
            () -> new CompanyTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> BRASS_ATM = register("brass_atm",
            () -> new BrassATMBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CARD_ENGRAVER = register("card_engraver",
            () -> new CardEngraverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> CARD_PRESS = register("card_press",
            () -> new CardPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final RegistryObject<Block> BRASS_CASH_REGISTER = register("brass_cash_register",
            () -> new BrassCashRegisterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> MINTING_PRESS = register("minting_press",
            () -> new MintingPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    // The press is not a full cube disable face occlusion culling so
                    // adjacent block faces are rendered correctly when the press is
                    // placed against a wall.
                    .noOcclusion()));

    private static RegistryObject<Block> register(String name, Supplier<Block> supplier) {
        RegistryObject<Block> reg = BLOCKS.register(name, supplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(reg.get(), new Item.Properties()));
        return reg;
    }
}
