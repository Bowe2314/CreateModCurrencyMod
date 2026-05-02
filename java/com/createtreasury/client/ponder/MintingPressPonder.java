package com.createtreasury.client.ponder;

import com.createtreasury.block.MintingPressBlockEntity;
import com.createtreasury.registry.ModItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class MintingPressPonder {

    /**
     * Second ponder tab: "Linking to a Bank"
     * Reuses the minting_press.nbt schematic (same layout as mintCoins).
     * Shows how to use the Company Linker to link the press to a Bank,
     * and explains the config-gated behaviours (required link, max links, coin tagging).
     */
    public static void linkToBank(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("minting_press_link", "Linking a Minting Press to a Bank");
        scene.configureBasePlate(0, 0, 5);

        BlockPos depotPos  = new BlockPos(2, 1, 2);
        BlockPos pressPos  = new BlockPos(2, 3, 2);
        BlockPos shaftFree = new BlockPos(2, 3, 4);

        // ── 1. Show full setup already running ────────────────────────────────
        scene.showBasePlate();
        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        scene.idle(10);

        scene.world().modifyBlockEntityNBT(
                util.select().fromTo(2, 3, 2, 2, 3, 4),
                KineticBlockEntity.class,
                nbt -> nbt.putFloat("Speed", 64f),
                true);
        scene.idle(10);

        // ── 2. Introduce the Company Linker ───────────────────────────────────
        scene.overlay().showText(70)
                .text("The Company Linker lets you link a Minting Press to a Bank")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(pressPos));
        scene.idle(80);

        // ── 3. Right-click in air → open Bank selection ───────────────────────
        Vec3 openTarget = util.vector().blockSurface(shaftFree, Direction.SOUTH);
        scene.overlay().showControls(openTarget, Pointing.RIGHT, 60)
                .rightClick()
                .withItem(new ItemStack(ModItems.COMPANY_LINKER.get()));

        scene.overlay().showText(65)
                .text("Right-click with the Linker to open the Bank selection screen and choose your Bank")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(openTarget);
        scene.idle(75);

        // ── 4. Sneak + right-click the press → link it ────────────────────────
        Vec3 pressTop = util.vector().topOf(pressPos);
        scene.overlay().showControls(pressTop, Pointing.DOWN, 70)
                .whileSneaking()
                .rightClick()
                .withItem(new ItemStack(ModItems.COMPANY_LINKER.get()));

        scene.overlay().showText(70)
                .text("Then Sneak + Right-click the Minting Press to link it to the selected Bank")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(pressTop);
        scene.idle(80);

        // ── 5. Apply the link in the scene and show result ────────────────────
        scene.world().modifyBlockEntity(pressPos, MintingPressBlockEntity.class,
                be -> be.setLinkedBank("TreasuryBank"));
        scene.idle(10);

        scene.overlay().showText(70)
                .text("The press is now linked! Coins it mints will be stamped with the Bank's name")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(pressTop);
        scene.idle(80);

        // ── 6. Demo: mint a coin and show the MintedBy tag ───────────────────
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(item("create:zinc_ingot", 1)));
        scene.idle(10);

        scene.world().modifyBlockEntity(pressPos, MintingPressBlockEntity.class,
                be -> {
                    be.setSpeed(32f);
                    be.pressingBehaviour.start(PressingBehaviour.Mode.WORLD);
                });
        scene.idle(40);

        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(new ItemStack(ModItems.ZINC_COIN.get(), 4)));
        scene.idle(10);

        scene.overlay().showText(65)
                .text("Hover over the coins to see \"Minted by: TreasuryBank\" in the tooltip")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(depotPos));
        scene.idle(75);

        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(ItemStack.EMPTY));

        scene.markAsFinished();
    }

    private static ItemStack item(String id, int count) {
        var it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it != null ? new ItemStack(it, count) : ItemStack.EMPTY;
    }

    /**
     * Schematic: assets/createtreasury/ponder/minting_press.nbt  (5×5×5)
     *   y=0             : checkerboard floor (snow_block / white_concrete)
     *   (2,1,2)         : create:depot  y=2 is the free gap below the press
     *   (2,3,2)         : minting_press[horizontal_facing=south]
     *   (2,3,3),(2,3,4) : create:shaft[axis=z]
     */
    public static void mintCoins(SceneBuilder scene, SceneBuildingUtil util) {

        scene.title("minting_press", "Minting Press");
        scene.configureBasePlate(0, 0, 5);

        BlockPos depotPos = new BlockPos(2, 1, 2);
        BlockPos pressPos = new BlockPos(2, 3, 2);
        BlockPos shaftFree = new BlockPos(2, 3, 4);

        // ── 1. Checkerboard floor rises ───────────────────────────────────────
        scene.showBasePlate();
        scene.idle(20);

        // ── 2. Depot rises from floor ─────────────────────────────────────────
        scene.world().showSection(util.select().position(depotPos), Direction.UP);
        scene.idle(15);

        // ── 3. Press + shaft fall from above ──────────────────────────────────
        scene.world().showSection(
                util.select().fromTo(2, 3, 2, 2, 3, 4),
                Direction.DOWN);
        scene.idle(25);

        // ── 4. Set kinetic speed on press + shafts ────────────────────────────
        scene.world().modifyBlockEntityNBT(
                util.select().fromTo(2, 3, 2, 2, 3, 4),
                KineticBlockEntity.class,
                nbt -> nbt.putFloat("Speed", 64f),
                true);
        scene.idle(5);

        // ── text_1: rotational force ──────────────────────────────────────────
        scene.overlay().showText(60)
                .text("Connect any source of Rotational Force to the Shaft")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(shaftFree, Direction.SOUTH));
        scene.idle(70);

        // ── text_2: depot placement ───────────────────────────────────────────
        scene.overlay().showText(60)
                .text("Place a Depot directly below the press head")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(depotPos));
        scene.idle(70);

        // ── Recipe demonstrations ─────────────────────────────────────────────
        showMinting(scene, util, depotPos, pressPos,
                item("create:zinc_ingot", 1),
                new ItemStack(ModItems.ZINC_COIN.get(), 4),
                "Zinc Ingots are stamped into Zinc Coins (\u00d74)");

        showMinting(scene, util, depotPos, pressPos,
                item("create:brass_sheet", 1),
                new ItemStack(ModItems.BRASS_COIN.get(), 2),
                "Brass Sheets are stamped into Brass Coins (\u00d72)");

        showMinting(scene, util, depotPos, pressPos,
                item("create:andesite_alloy", 1),
                new ItemStack(ModItems.ANDESITE_COIN.get(), 4),
                "Andesite Alloy is stamped into Andesite Coins (\u00d74)");

        // Clear depot when done
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(ItemStack.EMPTY));

        scene.markAsFinished();
    }

    /**
     * Places the input item ON the depot (flat sprite, exactly how Create renders it),
     * animates the press stamp, then swaps to the output coins.
     * The depot is cleared at the start so the previous recipe's output is gone.
     */
    private static void showMinting(SceneBuilder scene, SceneBuildingUtil util,
                                    BlockPos depotPos, BlockPos pressPos,
                                    ItemStack input, ItemStack output,
                                    String text) {
        if (input.isEmpty() || output.isEmpty()) return;

        // Clear any previous item from the depot first
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(ItemStack.EMPTY));
        scene.idle(5);

        // Place input item flat on the depot Create renders this as a lying sprite
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(input));
        scene.idle(15);

        // Show label
        scene.overlay().showText(55)
                .text(text)
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(depotPos));

        // Trigger the press to stamp down
        scene.world().modifyBlockEntity(pressPos, MintingPressBlockEntity.class,
                be -> {
                    be.setSpeed(32f);
                    be.pressingBehaviour.start(PressingBehaviour.Mode.WORLD);
                });
        scene.idle(45);

        // Swap input for output coins (still lying flat on the depot)
        scene.world().modifyBlockEntity(depotPos, DepotBlockEntity.class,
                be -> be.setHeldItem(output));
        scene.idle(35);
    }
}
