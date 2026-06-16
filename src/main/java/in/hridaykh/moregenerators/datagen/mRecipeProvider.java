package in.hridaykh.moregenerators.datagen;

import com.simibubi.create.AllItems;
import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.collections.ModBlocks;
import in.hridaykh.moregenerators.collections.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class mRecipeProvider extends net.minecraft.data.recipes.RecipeProvider implements IConditionBuilder {

	public mRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void buildRecipes(RecipeOutput ro) {
		// ingredients
		shapelessRecipe(ro, ModItems.LED_FILAMENT, 2, ModItems.PHOSPHORUS, ModItems.SILICON);
		complexShapedRecipe(ro, ModItems.LED_BULB, 1, Map.of('G', Items.GLASS_PANE, 'F', ModItems.LED_FILAMENT, 'C', AllItems.COPPER_SHEET), " G ", "GFG", " C ");
		oreSmelting(ro, List.of(Items.QUARTZ), RecipeCategory.MISC, ModItems.SILICON, 0f, 100, "silicon");
		oreBlasting(ro, List.of(Items.QUARTZ), RecipeCategory.MISC, ModItems.SILICON, 0f, 100, "silicon");

		// misc
		shapedRecipe(ro, AllItems.COPPER_NUGGET, "##", ModdedItems.WIRE, 2);
		shapelessRecipe(ro, ModBlocks.LIGHT_RESISTOR, 1, ModdedItems.RESISTOR, Items.TORCH);

		// batteries
		shapelessRecipe(ro, ModBlocks.BUFF_POTATO_BATTERY, 1, AllItems.COPPER_SHEET, ModdedItems.ZINC_SHEET, Items.BAKED_POTATO);

		// solar
		shapelessRecipe(ro, ModBlocks.SOLAR_PANEL, 1, ModItems.SILICON, ModItems.PHOSPHORUS, Items.GLASS_PANE, AllItems.COPPER_NUGGET);
		shapelessRecipe(ro, ModBlocks.SOLAR_PANEL, 1, ModBlocks.ANGLED_SOLAR_PANEL);
		shapelessRecipe(ro, ModBlocks.ANGLED_SOLAR_PANEL, 1, ModBlocks.SOLAR_PANEL);
		shapelessRecipe(ro, ModBlocks.MODULAR_PANEL, 1, ModBlocks.SOLAR_PANEL, ModdedItems.WIRE);
	}

	private void shapelessRecipe(RecipeOutput ro, ItemLike out, int q, ItemLike... ins) {
		StringBuilder insName = new StringBuilder();
		var builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, out, q);
		boolean first = true;
		for (ItemLike in : ins) {
			String inName = in.asItem().getDescriptionId();
			builder.requires(in).unlockedBy("has_" + inName, has(in));
			if (first) {
				insName.append("_from_").append(inName);
				first = false;
			} else
				insName.append("_and_").append(inName);
		}
		builder.save(ro, out.asItem().getDescriptionId() + insName.toString());
	}

	private void shapedRecipe(RecipeOutput ro, ItemLike in, String pattern, ItemLike out, int q) {
		// Pad to 9 chars, replace non-spaces with 'I', and split every 3 characters
		String[] p = String.format("%-9s", pattern.replaceAll("_", "")).replaceAll("[^ ]", "I").split("(?<=\\G...)");
		String inName = in.asItem().getDescriptionId();
		String outName = out.asItem().getDescriptionId();
		ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out, q).pattern(p[0]).pattern(p[1]).pattern(p[2]).define('I', in).unlockedBy("has_" + inName, has(in))
				.save(ro, outName + "_from_" + inName);

	}

	private void complexShapedRecipe(RecipeOutput ro, ItemLike out, int q, Map<Character, ItemLike> inputDefinations, String... pattern) {
		if (pattern.length != 3)
			throw new IllegalArgumentException("shaped recipe pattern length should be 3");

		String outName = out.asItem().getDescriptionId();
		var b = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, out, q).pattern(pattern[0]).pattern(pattern[1]).pattern(pattern[2]);

		for (Map.Entry<Character, ItemLike> entry : inputDefinations.entrySet()) {
			b.define(entry.getKey(), entry.getValue());
			b.unlockedBy("has_" + entry.getValue().asItem().getDescriptionId(), has(entry.getValue()));
		}

		b.save(ro, "complex_" + outName);

	}

	protected static void oreSmelting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience,
			int cookingTime, String group) {
		oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, ingredients, category, result, experience, cookingTime, group,
				"_from_smelting");
	}

	protected static void oreBlasting(RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience,
			int cookingTime, String group) {
		oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, ingredients, category, result, experience, cookingTime, group,
				"_from_blasting");
	}

	protected static <T extends AbstractCookingRecipe> void oreCooking(RecipeOutput recipeOutput, RecipeSerializer<T> serializer,
			AbstractCookingRecipe.Factory<T> recipeFactory, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience,
			int cookingTime, String group, String suffix) {
		for (ItemLike itemlike : ingredients) {
			SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), category, result, experience, cookingTime, serializer, recipeFactory).group(group)
					.unlockedBy(getHasName(itemlike), has(itemlike))
					.save(recipeOutput, MoreGenerators.MOD_ID + ":" + getItemName(result) + suffix + "_" + getItemName(itemlike));
		}
	}
}
