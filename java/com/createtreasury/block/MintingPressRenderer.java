package com.createtreasury.block;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import com.createtreasury.client.TreasuryClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class MintingPressRenderer extends KineticBlockEntityRenderer<MintingPressBlockEntity> {

    public MintingPressRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(MintingPressBlockEntity be) {
        return true;
    }

    @Override
    protected void renderSafe(MintingPressBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        // The block is opaque so Minecraft stores light=0 at the block's own position.
        // Sample from the block above (which is in air) to get the real ambient light.
        int correctedLight = LightTexture.pack(
                be.getLevel().getBrightness(LightLayer.BLOCK, be.getBlockPos().above()),
                be.getLevel().getBrightness(LightLayer.SKY,   be.getBlockPos().above()));

        // Shaft
        BlockState state = getRenderedBlockState(be);
        RenderType type  = getRenderType(be, state);
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), correctedLight);

        // Head
        BlockState blockState = be.getBlockState();
        PressingBehaviour pressingBehaviour = be.getPressingBehaviour();
        float renderedHeadOffset = pressingBehaviour.getRenderedHeadOffset(partialTicks)
                * pressingBehaviour.mode.headOffset;

        SuperByteBuffer headRender = CachedBuffers.partialFacing(TreasuryClientEvents.MINTING_PRESS_HEAD,
                blockState, blockState.getValue(HORIZONTAL_FACING));

        headRender.translate(0, -renderedHeadOffset, 0)
                .light(correctedLight)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected BlockState getRenderedBlockState(MintingPressBlockEntity be) {
        return shaft(getRotationAxisOf(be));
    }
}
