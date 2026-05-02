package com.createtreasury.recipe;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A pressing recipe that is only processed by the Card Press, not the regular
 * Mechanical Press or the Minting Press. Same JSON format as PressingRecipe.
 */
public class CardPressRecipe extends PressingRecipe {

    public CardPressRecipe(ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CARD_PRESSING.getType();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CARD_PRESSING.getSerializer();
    }
}
