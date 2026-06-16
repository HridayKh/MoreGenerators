package in.hridaykh.moregenerators.datagen;

import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.collections.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class mBlockStateProvider extends BlockStateProvider {

	public mBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, MoreGenerators.MOD_ID, exFileHelper);
	}

	@Override
	protected void registerStatesAndModels() {
		customModel(ModBlocks.MODULAR_PANEL);
		customModel(ModBlocks.BUFF_POTATO_BATTERY);
	}

	private void customModel(DeferredBlock<?> block) {
		var model = models().getExistingFile(modLoc("block/" + block.getId().getPath()));
		simpleBlock(block.get(), model);
	}
}