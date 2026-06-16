package in.hridaykh.moregenerators.collections;

import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.content.battery.buff.BuffPotatoBatteryBlock;
import in.hridaykh.moregenerators.content.resistor.Resistor;
import in.hridaykh.moregenerators.content.solar.ModularPanelBlock;
import in.hridaykh.moregenerators.content.solar.SolarBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MoreGenerators.MOD_ID);

	public static final DeferredBlock<Resistor> LIGHT_RESISTOR = registerBlockWithItem("resistor", () -> new Resistor(BlockBehaviour.Properties.of().instabreak()));

	public static final DeferredBlock<SolarBlock> SOLAR_PANEL = registerBlockWithItem("solar_panel",
			() -> new SolarBlock(BlockBehaviour.Properties.of().instabreak()));

	public static final DeferredBlock<SolarBlock> ANGLED_SOLAR_PANEL = registerBlockWithItem("angled_solar_panel",
			() -> new SolarBlock(BlockBehaviour.Properties.of().instabreak().noOcclusion()));

	public static final DeferredBlock<BuffPotatoBatteryBlock> BUFF_POTATO_BATTERY = registerBlockWithItem("buff_potato_battery",
			() -> new BuffPotatoBatteryBlock(BlockBehaviour.Properties.of().instabreak()));

	public static final DeferredBlock<ModularPanelBlock> MODULAR_PANEL = registerBlockWithItem("modular_panel",
			() -> new ModularPanelBlock(BlockBehaviour.Properties.of().instabreak()));

	public static void register(IEventBus eventBus) {
		CreativeTabs.addItemToTab(LIGHT_RESISTOR, CreativeTabs.CREATIVE_TABS[0]);
		CreativeTabs.addItemToTab(SOLAR_PANEL, CreativeTabs.CREATIVE_TABS[0]);
		CreativeTabs.addItemToTab(BUFF_POTATO_BATTERY, CreativeTabs.CREATIVE_TABS[0]);
		CreativeTabs.addItemToTab(ANGLED_SOLAR_PANEL, CreativeTabs.CREATIVE_TABS[0]);
		CreativeTabs.addItemToTab(MODULAR_PANEL, CreativeTabs.CREATIVE_TABS[0]);

		BLOCKS.register(eventBus);
	}

	private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> blockSupplier) {
		DeferredBlock<T> deferredBlock = BLOCKS.register(name, blockSupplier);
		ModItems.ITEMS.register(name, () -> new BlockItem(deferredBlock.get(), new Item.Properties()));
		return deferredBlock;
	}
}
