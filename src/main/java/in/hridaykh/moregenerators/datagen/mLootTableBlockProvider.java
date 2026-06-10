package in.hridaykh.moregenerators.datagen;

import in.hridaykh.moregenerators.collections.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class mLootTableBlockProvider extends BlockLootSubProvider {

	protected mLootTableBlockProvider(HolderLookup.Provider provider) {
		super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);

	}

	@Override
	protected void generate() {
		dropSelf(ModBlocks.LIGHT_RESISTOR.get());
		dropSelf(ModBlocks.SOLAR_PANEL.get());
		dropSelf(ModBlocks.ANGLED_SOLAR_PANEL.get());
		dropSelf(ModBlocks.BUFF_POTATO_BATTERY.get());
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return ModBlocks.BLOCKS.getEntries().stream().map(deferredHolder -> (Block) deferredHolder.get()).toList();
	}

}
