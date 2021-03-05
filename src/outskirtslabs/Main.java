package outskirtslabs;


import outskirtslabs.ruuvi.Receiver;

public final class Main {
    public static void main(String[] args) throws Exception {
        Receiver receiver = new Receiver(
                "hcitool lescan --duplicates --passive",
                "hcidump --raw"
        );
        receiver.addMeasurementListener((measurement, derived) -> {
            System.out.println(measurement);
        });
        receiver.start();
        Thread.sleep(5000);
        receiver.dispose();
    }

}
