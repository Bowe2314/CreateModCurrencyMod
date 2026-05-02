package com.createtreasury.recipe;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A pressing-style recipe that is only processed by the Minting Press,
 * not the regular Mechanical Press.
 *
 * Extends PressingRecipe for structural compatibility (same JSON format,
 * same processing logic) but overrides getType() / getSerializer() so
 * the recipe manager stores these under the "createtreasury:minting" type
 * and the Mechanical Press's AllRecipeTypes.PRESSING lookup never finds them.
 */
public class MintingRecipe extends PressingRecipe {

    public MintingRecipe(ProcessingRecipeParams params) {
        super(params);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MINTING.getType();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MINTING.getSerializer();
    }
}
