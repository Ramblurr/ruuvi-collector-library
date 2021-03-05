package outskirtslabs.ruuvi;

import lombok.Builder;
import lombok.Value;

/**
 * This class contains all the possible fields/data acquirable from a RuuviTag
 * in a "human format", for example the temperature as a decimal number rather
 * than an integer meaning one 200th of a degree. Not all fields are necessarily
 * present depending on the data format and implementations.
 */
@Value
@Builder
public class RuuviValue {

    Integer dataFormat;
    Double temperature;
    Double humidity;
    Double pressure;
    Double accelerationX;
    Double accelerationY;
    Double accelerationZ;
    Double batteryVoltage;
    Integer txPower;
    Integer movementCounter;
    Integer measurementSequenceNumber;

    /**
     * Timestamp in milliseconds, normally not populated to use local time
     */
    Long time;
    /**
     * Friendly name for the tag
     */
    String name;
    /**
     * MAC address of the tag as seen by the receiver
     */
    String mac;
    /**
     * Arbitrary string associated with the receiver.
     */
    String receiver;
    /**
     * The RSSI at the receiver
     */
    Integer rssi;
}
