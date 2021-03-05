package outskirtslabs.ruuvi;

import fi.tkgwf.ruuvi.common.parser.DataFormatParser;
import fi.tkgwf.ruuvi.common.parser.impl.AnyDataFormatParser;

import java.util.Optional;

/**
 * Creates {@link fi.tkgwf.ruuvi.common.bean.RuuviMeasurement} instances from raw dumps from hcidump.
 */
class BeaconParser {

    private final DataFormatParser parser = new AnyDataFormatParser();

    /**
     * Handles a packet and creates a {@link fi.tkgwf.ruuvi.common.bean.RuuviMeasurement} if the handler
     * understands this packet.
     *
     * @param hciData the data parsed from hcidump
     * @return the measurement if this handler can
     * parse the packet
     */
    public Optional<RuuviValue> parse(HCIData hciData) {
        HCIData.Report.AdvertisementData adData = hciData.findAdvertisementDataByType(0xFF); // Manufacturer-specific data, raw dataformats
        if (adData == null) {
            adData = hciData.findAdvertisementDataByType(0x16); // Eddystone url
            if (adData == null) {
                return Optional.empty();
            }
        }
        fi.tkgwf.ruuvi.common.bean.RuuviMeasurement measurement = parser.parse(adData.dataBytes());
        if (measurement == null) {
            return Optional.empty();
        }
        return Optional.of(fromRuuviMeasurement(measurement)
                .mac(hciData.mac)
                .rssi(hciData.rssi)
                .name("")
                .receiver("")
                .build());
    }

    public static RuuviValue.RuuviValueBuilder fromRuuviMeasurement(fi.tkgwf.ruuvi.common.bean.RuuviMeasurement m) {
        return RuuviValue.builder()
                .dataFormat(m.getDataFormat())
                .temperature(m.getTemperature())
                .humidity(m.getHumidity())
                .pressure(m.getPressure())
                .accelerationX(m.getAccelerationX())
                .accelerationY(m.getAccelerationY())
                .accelerationZ(m.getAccelerationZ())
                .batteryVoltage(m.getBatteryVoltage())
                .txPower(m.getTxPower())
                .movementCounter(m.getMovementCounter())
                .measurementSequenceNumber(m.getMeasurementSequenceNumber());

    }
}
