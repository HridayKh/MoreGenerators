package in.hridaykh.moregenerators.content;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import in.hridaykh.moregenerators.collections.ModPartialModels;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.loading.FMLEnvironment;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulbState;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class LedLightBulb extends LightBulb {
	private static final float RATED_POWER_WATTS = 3.0f;
	private static final float RATED_VOLTAGE_VOLTS = 120.0f;
	private static final float TEMPERATURE_AT_RATED_RESISTANCE = 1450.0f;
	private static final float MIN_RESISTANCE_FACTOR = 0.85f;
	private static final float THERMAL_MASS = 0.00015f;
	private static final float OVERHEAT_TEMPERATURE = 2100.0f;
	private static final float DISSIPATION_DIVISOR = 1450.0f;

	public LedLightBulb(Item.Properties settings) {
		super(settings);
		this.canBeDyed = true;
		this.modelSupplier = () -> state -> switch (state) {
			case OFF -> ModPartialModels.MODEL_OFF;
			case LOW_POWER, ON -> ModPartialModels.MODEL_ON;
			case BROKEN -> ModPartialModels.MODEL_BROKEN;
			case LIGHT -> ModPartialModels.MODEL_LIGHT;
		};
		this.dyedModelSupplier = () -> state -> switch (state) {
			case OFF -> ModPartialModels.DYED_MODEL_OFF;
			case LOW_POWER, ON -> ModPartialModels.DYED_MODEL_ON;
			case BROKEN -> ModPartialModels.DYED_MODEL_BROKEN;
			case LIGHT -> ModPartialModels.DYED_MODEL_LIGHT;
			case BULB -> ModPartialModels.DYED_MODEL_BULB;
		};

		applyRatedValues(RATED_POWER_WATTS, RATED_VOLTAGE_VOLTS, TEMPERATURE_AT_RATED_RESISTANCE, THERMAL_MASS);
	}

	@Override
	public LightBulbState createState(LightFixtureBlockEntity fixture) {
		return new State(this, fixture, modelSupplier, dyedModelSupplier);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
		if (FMLEnvironment.dist.isClient()) {
			LedLightBulbClientTooltip.appendTooltip(this, stack, tooltipComponents);
		}
	}

	private void applyRatedValues(float ratedPower, float ratedVoltage, float maxTemperature, float thermalMass) {
		this.power = ratedPower;
		this.voltage = ratedVoltage;
		this.T_max = maxTemperature;
		this.R_max = (ratedVoltage * ratedVoltage) / ratedPower;
		this.R_min = this.R_max * MIN_RESISTANCE_FACTOR;
		this.thermalProperties = new ILightBulb.Properties(ratedPower / DISSIPATION_DIVISOR, thermalMass, OVERHEAT_TEMPERATURE);
	}

	public static class State extends LightBulb.SimpleState {
		public <T extends Item & ILightBulb> State(T bulb, LightFixtureBlockEntity fixture, Supplier<Function<LightBulb.State, PartialModel>> modelProviderSupplier, Supplier<Function<DyedState, PartialModel>> dyedModelProviderSupplier) {
			super(bulb, fixture, modelProviderSupplier, dyedModelProviderSupplier);
		}

		@Override
		protected void updatePowerLevel(int newLevel) {
			super.updatePowerLevel(newLevel > 0 ? 2 : 0);
		}
	}
}
