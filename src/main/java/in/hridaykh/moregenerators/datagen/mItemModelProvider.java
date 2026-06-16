package in.hridaykh.moregenerators.datagen;

import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.collections.ModBlocks;
import in.hridaykh.moregenerators.collections.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class mItemModelProvider extends net.neoforged.neoforge.client.model.generators.ItemModelProvider {

	public mItemModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
		super(output, MoreGenerators.MOD_ID, exFileHelper);
	}

	@Override
	protected void registerModels() {
		basicItem(ModItems.LED_FILAMENT.get());

		basicItem(ModItems.PHOSPHORUS.get());
		basicItem(ModItems.SILICON.get());

		this.withExistingParent(ModBlocks.SOLAR_PANEL.getId().getPath(), modLoc("block/solar_panel"));
		this.withExistingParent(ModBlocks.MODULAR_PANEL.getId().getPath(), modLoc("block/modular_panel"));
		this.withExistingParent(ModBlocks.ANGLED_SOLAR_PANEL.getId().getPath(), modLoc("block/angled_solar_panel"));

		this.withExistingParent(ModItems.LED_BULB.getId().getPath(), modLoc("block/lamps/light_bulb"));
		this.withExistingParent(ModBlocks.LIGHT_RESISTOR.getId().getPath(), modLoc("block/resistor_v_lit"));
	}

}
