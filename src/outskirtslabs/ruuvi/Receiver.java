package outskirtslabs.ruuvi;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;

public class Receiver {

    public final static int PROCESS_BACKOFF_DELAY_SEC = 5;

    private static final Logger LOG = Logger.getLogger(Receiver.class);

    private static Optional<Process> startCommand(String command) {
        String[] scan = command.split(" ");
        if (scan.length > 0) {
            try {
                Process process = new ProcessBuilder(scan).start();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
                return Optional.of(process);
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Receiver(String scanCommand, String dumpCommand) {
        this.scanCommand = scanCommand.trim();
        this.dumpCommand = dumpCommand.trim();
        if (scanCommand.length() == 0 || dumpCommand.length() == 0)
            throw new RuntimeException("Scan command and dump command must valid commands.");
    }


    private final ScheduledExecutorService jobScheduler = Executors.newScheduledThreadPool(2);
    private final Set<MeasurementListener> measurementListeners = new CopyOnWriteArraySet<>();
    private final Map<String, ScheduledFuture<?>> jobFutures = new ConcurrentHashMap<>();
    private final String scanCommand;
    private final String dumpCommand;


    public boolean start() {
        SuperviseProcess scanProcess = new SuperviseProcess("scan", scanCommand, false);
        SuperviseProcess dumpProcess = new SuperviseProcess("dump", dumpCommand, true, this::readDump);

        ScheduledFuture<?> scanFuture = jobScheduler.schedule(scanProcess, 0, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> dumpFuture = jobScheduler.schedule(dumpProcess, 0, TimeUnit.MILLISECONDS);
        jobFutures.put("scan", scanFuture);
        jobFutures.put("dump", dumpFuture);
        return true;
    }

    public void dispose() {
        LOG.warn("Disposing Ruuvi Receiver");

        shutdownAndWait(jobScheduler);
        cancelAllFutures(true);

        measurementListeners.clear();
        LOG.debug("Ruuvi Receiver has been disposed");
    }

    public void addMeasurementListener(MeasurementListener listener) {
        measurementListeners.add(listener);
    }

    public void removeMeasurementListener(MeasurementListener listener) {
        measurementListeners.remove(listener);
    }

    private void notifyNewMeasurement(RuuviData data) {
        measurementListeners.forEach(listener -> {
            try {
                listener.measured(data.getMeasurement(), data.getDerived());
            } catch (Exception e) {
                LOG.warn("Notifying listener failed", e);
            }
        });
    }

    private static void shutdownAndWait(ExecutorService executorService) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) { /* do nothing */ }
    }


    private void cancelAllFutures(boolean forceInterrupt) {
        synchronized (jobScheduler) {
            jobFutures.values().forEach(future -> future.cancel(forceInterrupt));
            jobFutures.clear();
        }
    }

    private final class SuperviseProcess implements Runnable {

        private final String name;
        private final String command;
        private final boolean restartOnExit;
        private final Function<BufferedReader, Boolean> reader;

        public SuperviseProcess(String name, String command, boolean restartOnExit) {
            this.name = name;
            this.command = command;
            this.restartOnExit = restartOnExit;
            this.reader = null;
        }

        public SuperviseProcess(String name, String command, boolean restartOnExit, Function<BufferedReader, Boolean> reader) {
            this.name = name;
            this.command = command;
            this.restartOnExit = restartOnExit;
            this.reader = reader;
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {

                Optional<Process> opt = startCommand(command);
                if (opt.isEmpty()) {
                    LOG.warn(String.format("Skipping command %s, invalid command", name));
                    return;
                }
                Process proc = opt.get();
                if (this.reader != null) {
                    reader.apply(new BufferedReader(new InputStreamReader(proc.getInputStream())));
                }

                try {
                    opt.get().waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (!restartOnExit) {
                    LOG.info(String.format("Process %s exited, not restarting.", name));
                    return;
                } else {
                    LOG.info(String.format("Process %s exited, will restart", name));
                }

                try {
                    Thread.sleep(PROCESS_BACKOFF_DELAY_SEC * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            LOG.info("supervisor interrupted. aborting.");
        }


    }

    private boolean readDump(BufferedReader reader) {
        HCIParser parser = new HCIParser();
        boolean dataReceived = false;
        boolean healthy = false;
        BeaconParser beaconParser = new BeaconParser();
        try {
            String line, latestMAC = null;
            while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                if (line.contains("device: disconnected")) {
                    LOG.error(line + ": Either the bluetooth device was externally disabled or physically disconnected");
                    healthy = false;
                }
                if (line.contains("No such device")) {
                    LOG.error(line + ": Check that your bluetooth adapter is enabled and working properly");
                    healthy = false;
                }
                if (!dataReceived) {
                    if (line.startsWith("> ")) {
                        LOG.info("Successfully reading data from hcidump");
                        dataReceived = true;
                        healthy = true;
                    } else {
                        continue; // skip the unnecessary garbage at beginning containing hcidump version and other junk print
                    }
                }
                try {
                    //Read in MAC address from first line
                    if (Util.hasMacAddress(line)) {
                        latestMAC = Util.getMacFromLine(line);
                    }
                    //Apply Mac Address Filtering
                    if (true /*Config.isAllowedMAC(latestMAC)*/) {
                        HCIData hciData = parser.readLine(line);
                        if (hciData != null) {
                            beaconParser
                                    .parse(hciData)
                                    .map(MeasurementValueCalculator::calculateAllValues)
                                    .ifPresent(Receiver.this::notifyNewMeasurement);
                            latestMAC = null; // "reset" the mac to null to avoid misleading MAC addresses when an error happens *after* successfully reading a full packet
                            healthy = true;
                        }
                    }
                } catch (Exception ex) {
                    if (latestMAC != null) {
                        LOG.warn("Uncaught exception while handling measurements from MAC address \"" + latestMAC + "\", if this repeats and this is not a Ruuvitag, try blacklisting it", ex);
                    } else {
                        LOG.warn("Uncaught exception while handling measurements, this is an unexpected event. Please report this to https://github.com/Scrin/RuuviCollector/issues and include this log", ex);
                    }
                    LOG.debug("Offending line: " + line);
                }
            }
            LOG.debug("dump reader finished");
        } catch (IOException ex) {
            LOG.error("Uncaught exception while reading measurements", ex);
            return false;
        }
        return healthy;
    }

}
