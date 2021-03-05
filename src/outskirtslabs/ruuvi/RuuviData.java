package outskirtslabs.ruuvi;

import lombok.Value;

@Value
class RuuviData {
    RuuviValue measurement;
    RuuviDerivedValue derived;
}
