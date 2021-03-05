package outskirtslabs.ruuvi;

@FunctionalInterface
public interface MeasurementListener {
    void measured(RuuviValue measurement, RuuviDerivedValue derived);
}
