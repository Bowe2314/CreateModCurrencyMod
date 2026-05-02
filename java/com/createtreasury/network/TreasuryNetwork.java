package com.createtreasury.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class TreasuryNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("createtreasury", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, CreateCompanyPacket.class,
                CreateCompanyPacket::encode, CreateCompanyPacket::decode, CreateCompanyPacket::handle);
        CHANNEL.registerMessage(nextId++, InvitePlayerPacket.class,
                InvitePlayerPacket::encode, InvitePlayerPacket::decode, InvitePlayerPacket::handle);
        CHANNEL.registerMessage(nextId++, AcceptInvitePacket.class,
                AcceptInvitePacket::encode, AcceptInvitePacket::decode, AcceptInvitePacket::handle);
        CHANNEL.registerMessage(nextId++, DeclineInvitePacket.class,
                DeclineInvitePacket::encode, DeclineInvitePacket::decode, DeclineInvitePacket::handle);
        CHANNEL.registerMessage(nextId++, SyncCompanyPacket.class,
                SyncCompanyPacket::encode, SyncCompanyPacket::decode, SyncCompanyPacket::handle);
        CHANNEL.registerMessage(nextId++, LeaveCompanyPacket.class,
                LeaveCompanyPacket::encode, LeaveCompanyPacket::decode, LeaveCompanyPacket::handle);
        CHANNEL.registerMessage(nextId++, KickMemberPacket.class,
                KickMemberPacket::encode, KickMemberPacket::decode, KickMemberPacket::handle);
        CHANNEL.registerMessage(nextId++, SetLinkerSelectionPacket.class,
                SetLinkerSelectionPacket::encode, SetLinkerSelectionPacket::decode, SetLinkerSelectionPacket::handle);
        CHANNEL.registerMessage(nextId++, LinkMintingPressPacket.class,
                LinkMintingPressPacket::encode, LinkMintingPressPacket::decode, LinkMintingPressPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenLinkerScreenPacket.class,
                OpenLinkerScreenPacket::encode, OpenLinkerScreenPacket::decode, OpenLinkerScreenPacket::handle);
        // Card system packets
        CHANNEL.registerMessage(nextId++, LinkCardPressPacket.class,
                LinkCardPressPacket::encode, LinkCardPressPacket::decode, LinkCardPressPacket::handle);
        CHANNEL.registerMessage(nextId++, ActivateCardPacket.class,
                ActivateCardPacket::encode, ActivateCardPacket::decode, ActivateCardPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenBankingScreenPacket.class,
                OpenBankingScreenPacket::encode, OpenBankingScreenPacket::decode, OpenBankingScreenPacket::handle);
        CHANNEL.registerMessage(nextId++, WithdrawPacket.class,
                WithdrawPacket::encode, WithdrawPacket::decode, WithdrawPacket::handle);
        CHANNEL.registerMessage(nextId++, DepositPacket.class,
                DepositPacket::encode, DepositPacket::decode, DepositPacket::handle);
        CHANNEL.registerMessage(nextId++, EngraveCardPacket.class,
                EngraveCardPacket::encode, EngraveCardPacket::decode, EngraveCardPacket::handle);
        CHANNEL.registerMessage(nextId++, LockCardPacket.class,
                LockCardPacket::encode, LockCardPacket::decode, LockCardPacket::handle);
        CHANNEL.registerMessage(nextId++, UnlockCardPacket.class,
                UnlockCardPacket::encode, UnlockCardPacket::decode, UnlockCardPacket::handle);
        CHANNEL.registerMessage(nextId++, LinkATMPacket.class,
                LinkATMPacket::encode, LinkATMPacket::decode, LinkATMPacket::handle);
        // Cash Register packets
        CHANNEL.registerMessage(nextId++, OpenCashRegisterPacket.class,
                OpenCashRegisterPacket::encode, OpenCashRegisterPacket::decode, OpenCashRegisterPacket::handle);
        CHANNEL.registerMessage(nextId++, CashRegisterSetSlotPacket.class,
                CashRegisterSetSlotPacket::encode, CashRegisterSetSlotPacket::decode, CashRegisterSetSlotPacket::handle);
        CHANNEL.registerMessage(nextId++, CashRegisterSetPricesPacket.class,
                CashRegisterSetPricesPacket::encode, CashRegisterSetPricesPacket::decode, CashRegisterSetPricesPacket::handle);
        CHANNEL.registerMessage(nextId++, CashRegisterBuyPacket.class,
                CashRegisterBuyPacket::encode, CashRegisterBuyPacket::decode, CashRegisterBuyPacket::handle);
        CHANNEL.registerMessage(nextId++, CashRegisterCollectPacket.class,
                CashRegisterCollectPacket::encode, CashRegisterCollectPacket::decode, CashRegisterCollectPacket::handle);
        CHANNEL.registerMessage(nextId++, LinkCashRegisterPacket.class,
                LinkCashRegisterPacket::encode, LinkCashRegisterPacket::decode, LinkCashRegisterPacket::handle);
    }
}
