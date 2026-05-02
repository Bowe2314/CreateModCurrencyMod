package com.createtreasury.client.ponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class CompanyTerminalPonder {

    /**
     * Schematic: assets/createtreasury/ponder/company_terminal.nbt  (5×3×5)
     *   y=0        : checkerboard floor (snow_block / white_concrete)
     *   (2,1,2)    : company_terminal[facing=south]
     */
    public static void howItWorks(SceneBuilder scene, SceneBuildingUtil util) {

        scene.title("company_terminal", "Company Terminal");
        scene.configureBasePlate(0, 0, 5);

        BlockPos terminalPos = new BlockPos(2, 1, 2);

        // ── 1. Reveal floor and terminal ──────────────────────────────────────
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(terminalPos), Direction.DOWN);
        scene.idle(20);

        // ── text 1: what it is ────────────────────────────────────────────────
        scene.overlay().showText(65)
                .text("The Company Terminal lets players create and manage Companies and Banks")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(terminalPos));
        scene.idle(75);

        // ── text 2: right-click to open ───────────────────────────────────────
        scene.overlay().showControls(
                util.vector().blockSurface(terminalPos, Direction.SOUTH),
                Pointing.RIGHT, 55)
                .rightClick();

        scene.overlay().showText(60)
                .text("Right-click to open the terminal screen")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(terminalPos, Direction.SOUTH));
        scene.idle(70);

        // ── text 3: create / join ─────────────────────────────────────────────
        scene.overlay().showText(65)
                .text("From the screen you can found a new Company or Bank, invite other players, or accept invitations")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(terminalPos));
        scene.idle(75);

        // ── text 4: minting link ──────────────────────────────────────────────
        scene.overlay().showText(65)
                .text("Banks can be linked to Minting Presses using a Company Linker, allowing coins to be stamped with the bank's name")
                .attachKeyFrame()
                .placeNearTarget()
                .pointAt(util.vector().topOf(terminalPos));
        scene.idle(75);

        scene.markAsFinished();
    }
}
