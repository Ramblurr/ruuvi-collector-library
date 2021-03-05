package outskirtslabs.ruuvi;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RuuviDerivedValue {
    /**
     * Total acceleration
     */
    Double accelerationTotal;
    /**
     * The angle between the acceleration vector and X axis
     */
    Double accelerationAngleFromX;
    /**
     * The angle between the acceleration vector and Y axis
     */
    Double accelerationAngleFromY;
    /**
     * The angle between the acceleration vector and Z axis
     */
    Double accelerationAngleFromZ;
    /**
     * Absolute humidity in g/m^3
     */
    Double absoluteHumidity;
    /**
     * Dew point in Celsius
     */
    Double dewPoint;
    /**
     * Vapor pressure of water
     */
    Double equilibriumVaporPressure;
    /**
     * Density of air
     */
    Double airDensity;
}
