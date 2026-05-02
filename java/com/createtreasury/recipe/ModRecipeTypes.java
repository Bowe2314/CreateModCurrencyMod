package com.createtreasury.recipe;

import com.createtreasury.TreasuryMod;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeTypes implements IRecipeTypeInfo {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TreasuryMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, TreasuryMod.MOD_ID);

    // ---- entries ----

    public static final ModRecipeTypes MINTING      = new ModRecipeTypes("minting",       MintingRecipe::new);
    public static final ModRecipeTypes CARD_PRESSING = new ModRecipeTypes("card_pressing", CardPressRecipe::new);

    // ---- impl ----

    private final ResourceLocation id;
    private final RegistryObject<RecipeSerializer<?>> serializerObj;
    private final RegistryObject<RecipeType<?>> typeObj;

    @SuppressWarnings("unchecked")
    private <T extends PressingRecipe> ModRecipeTypes(String name, ProcessingRecipeBuilder.ProcessingRecipeFactory<T> factory) {
        id = new ResourceLocation(TreasuryMod.MOD_ID, name);
        serializerObj = SERIALIZERS.register(name, () -> new ProcessingRecipeSerializer<>(factory));
        typeObj       = TYPES.register(name, () -> new RecipeType<>() {
            @Override public String toString() { return id.toString(); }
        });
    }

    @Override public ResourceLocation   getId()         { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return serializerObj.get(); }
    @Override public RecipeType<?>       getType()       { return typeObj.get(); }
}
