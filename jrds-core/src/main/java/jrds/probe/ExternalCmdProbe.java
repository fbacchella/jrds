package jrds.probe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.rrd4j.core.DsDef;
import org.slf4j.event.Level;

import jrds.JrdsSample;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Util;
import lombok.Getter;

/**
 * This abstract class can be used to parse the results of an external command
 * 
 * @author Fabrice Bacchella
 */
public abstract class ExternalCmdProbe extends Probe<String, Number> {

    @Getter
    protected String cmd = null;

    private long sampleTime;

    @Override
    public void readProperties(PropertiesManager pm) {
        cmd = resolvCmdPath(pm.getProperty("path", ""));
    }

    public Boolean configure() {
        if (cmd == null) {
            return false;
        }
        Optional.ofNullable(getPd().getSpecific("arguments"))
                .map(String::trim)
                .filter(s -> ! s.isEmpty())
                .ifPresent(s -> cmd = cmd + " " + Util.parseTemplate(s, this));
        return true;
    }

    protected String resolvCmdPath(String path) {
        List<String> pathelements = new ArrayList<String>();
        pathelements.addAll(Arrays.asList(path.split(";")));
        String envPath = System.getenv("PATH");
        String pathSeparator = System.getProperty("path.separator");
        if (envPath != null && !envPath.isEmpty()) {
            Arrays.stream(envPath.split(pathSeparator)).forEach(pathelements::add);
        }
        String cmdname = getPd().getSpecific("command");
        log(Level.DEBUG, "will look for %s in %s", cmdname, pathelements);
        for (String pathdir: pathelements) {
            File tryfile = new File(pathdir, cmdname);
            log(Level.TRACE, "trying if %s can execute", tryfile);
            if (tryfile.canExecute()) {
                log(Level.DEBUG, "will use %s as a command", tryfile.getAbsolutePath());
                cmd = tryfile.getAbsolutePath();
                break;
            }
        }
        if (cmd == null) {
            log(Level.ERROR, "command %s not found", cmdname);
        }
        return cmd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#getNewSampleValues()
     */
    public Map<String, Number> getNewSampleValues() {
        String perfstring = launchCmd();
        String values[] = perfstring.split(":");
        DsDef[] defs = getPd().getDsDefs(getRequiredUptime());
        int n = values.length;
        if (values.length != defs.length + 1) {
            log(Level.ERROR, "Invalid number of values specified (found " + values.length + ", " + defs.length + " allowed)");
            return Collections.emptyMap();
        }
        String timeToken = values[0];
        if (timeToken.equalsIgnoreCase("N") || timeToken.equalsIgnoreCase("NOW")) {
            sampleTime = Instant.now().getEpochSecond();
        } else {
            try {
                sampleTime = Long.parseLong(timeToken);
            } catch (NumberFormatException ex) {
                log(Level.ERROR, "Invalid sample timestamp %s: %s", timeToken, Util.resolveThrowableException(ex));
                return Collections.emptyMap();
            }
        }
        Map<String, Number> retValues = new HashMap<>(n - 1);
        for (int i = 0; i < defs.length; i++) {
            double value = jrds.Util.parseStringNumber(values[i + 1], Double.NaN);
            retValues.put(defs[i].getDsName(), value);
        }
        return retValues;
    }

    @Override
    public boolean startCollect() {
        sampleTime = -1;
        return super.startCollect();
    }

    @Override
    public void modifySample(JrdsSample sample, Map<String, Number> values) {
        if (sampleTime != -1) {
            sample.setTime(new Date(sampleTime * 1000));
        }
    }

    protected String launchCmd() {
        String perfstring = "";
        Process perfProcess = null;
        boolean needsToKill = false;
        try {
            log(Level.DEBUG, "executing: %s", cmd);
            perfProcess = Runtime.getRuntime().exec(getCmd());
            perfProcess.getInputStream().close();
            try (InputStream  stdout = perfProcess.getInputStream()) {
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
                perfstring = stdoutReader.readLine();
            }
            needsToKill = ! perfProcess.waitFor(getTimeout(), TimeUnit.SECONDS);
            if (perfProcess.exitValue() != 0) {

                try (InputStream stderr = perfProcess.getErrorStream();
                                BufferedReader stderrtReader = new BufferedReader(new InputStreamReader(stderr))) {
                    String errostring = Optional.ofNullable(stderrtReader.readLine()).orElse("");
                    log(Level.ERROR, " command %s failed with %s", cmd, errostring);
                    perfstring = "";
                }
            }
        } catch (InterruptedException e) {
            log(Level.INFO, e, "External command interrupted");
            needsToKill = true;
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log(Level.ERROR, e, "External command failed to launch: %s", e);
        }

        if (needsToKill) {
            perfProcess.destroyForcibly();
        }
        log(Level.DEBUG, "returned line: %s", perfstring);
        return perfstring;
    }

    @Override
    public String getSourceType() {
        return "external command";
    }
}
