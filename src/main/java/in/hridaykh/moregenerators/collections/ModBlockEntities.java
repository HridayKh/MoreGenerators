package in.hridaykh.moregenerators.collections;

import in.hridaykh.moregenerators.MoreGenerators;
import in.hridaykh.moregenerators.SimpleMultiBlockEntity;
import in.hridaykh.moregenerators.content.battery.buff.BuffPotatoBatteryBE;
import in.hridaykh.moregenerators.content.resistor.ResistorBE;
import in.hridaykh.moregenerators.content.solar.SolarBE;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MoreGenerators.MOD_ID);
	@SuppressWarnings("null")
	public static final Supplier<BlockEntityType<ResistorBE>> RESISTOR_BE = BLOCK_ENTITIES.register("resistor",
			() -> BlockEntityType.Builder.of(ResistorBE::new, ModBlocks.LIGHT_RESISTOR.get()).build(null));
	@SuppressWarnings("null")
	public static final Supplier<BlockEntityType<SolarBE>> SOLAR_PANEL_BE = BLOCK_ENTITIES.register("solar_panel",
			() -> BlockEntityType.Builder.of(SolarBE::new, ModBlocks.SOLAR_PANEL.get()).build(null));
	@SuppressWarnings("null")
	public static final Supplier<BlockEntityType<SolarBE>> ANGLED_SOLAR_PANEL_BE = BLOCK_ENTITIES.register("angled_solar_panel",
			() -> BlockEntityType.Builder.of(SolarBE::new, ModBlocks.ANGLED_SOLAR_PANEL.get()).build(null));
	@SuppressWarnings("null")
	public static final Supplier<BlockEntityType<BuffPotatoBatteryBE>> BUFF_POTATO_BATTERY_BE = BLOCK_ENTITIES.register("buff_potato_battery",
			() -> BlockEntityType.Builder.of(BuffPotatoBatteryBE::new, ModBlocks.BUFF_POTATO_BATTERY.get()).build(null));
	@SuppressWarnings("null")
	public static final Supplier<BlockEntityType<SimpleMultiBlockEntity>> SIMPLE_MULTIBLOCK_BE = BLOCK_ENTITIES.register("simple_multiblock",
			() -> BlockEntityType.Builder.of(SimpleMultiBlockEntity::new, ModBlocks.SIMPLE_MULTIBLOCK.get()).build(null));

	public static void register(IEventBus eventBus) {
		BLOCK_ENTITIES.register(eventBus);
	}

}