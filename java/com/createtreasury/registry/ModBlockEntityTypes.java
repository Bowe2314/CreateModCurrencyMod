package com.createtreasury.registry;

import com.createtreasury.TreasuryMod;
import com.createtreasury.block.BrassATMBlockEntity;
import com.createtreasury.block.BrassCashRegisterBlockEntity;
import com.createtreasury.block.CardPressBlockEntity;
import com.createtreasury.block.MintingPressBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TreasuryMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<BrassATMBlockEntity>> BRASS_ATM =
            BLOCK_ENTITIES.register("brass_atm", () ->
                    BlockEntityType.Builder
                            .of(BrassATMBlockEntity::new, ModBlocks.BRASS_ATM.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<BrassCashRegisterBlockEntity>> CASH_REGISTER =
            BLOCK_ENTITIES.register("brass_cash_register", () ->
                    BlockEntityType.Builder
                            .of(BrassCashRegisterBlockEntity::new, ModBlocks.BRASS_CASH_REGISTER.get())
                            .build(null));

    @SuppressWarnings("unchecked")
    public static final RegistryObject<BlockEntityType<CardPressBlockEntity>> CARD_PRESS =
            BLOCK_ENTITIES.register("card_press", () -> {
                BlockEntityType<CardPressBlockEntity>[] holder = new BlockEntityType[1];
                BlockEntityType<CardPressBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new CardPressBlockEntity(holder[0], pos, state),
                                ModBlocks.CARD_PRESS.get())
                        .build(null);
                holder[0] = type;
                return type;
            });

    @SuppressWarnings("unchecked")
    public static final RegistryObject<BlockEntityType<MintingPressBlockEntity>> MINTING_PRESS =
            BLOCK_ENTITIES.register("minting_press", () -> {
                BlockEntityType<MintingPressBlockEntity>[] holder = new BlockEntityType[1];
                BlockEntityType<MintingPressBlockEntity> type = BlockEntityType.Builder
                        .of((pos, state) -> new MintingPressBlockEntity(holder[0], pos, state),
                                ModBlocks.MINTING_PRESS.get())
                        .build(null);
                holder[0] = type;
                return type;
            });
}
