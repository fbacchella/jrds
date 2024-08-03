package jrds.probe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.rrd4j.core.DsDef;
import org.slf4j.event.Level;

import jrds.JrdsSample;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Util;
import jrds.factories.ProbeBean;
import lombok.Getter;
import lombok.Setter;

/**
 * This abstract class can be used to parse the results of an external command
 * 
 * @author Fabrice Bacchella
 */
@ProbeBean({"cmd", "arguments", "output"})
public abstract class ExternalCmdProbe extends Probe<String, Number> {

    protected enum Output {
        STDOUT,
        STDERR
    }

    @Getter @Setter
    protected String cmd = null;

    @Getter
    private String arguments = null;

    private List<String> argsList = List.of();

    protected Output output = Output.STDOUT;

    private long sampleTime;
    private ProcessBuilder processBuilder = null;

    @Override
    public void setPd(ProbeDesc<String> pd) {
        super.setPd(pd);
        cmd = pd.getSpecific("command");
        arguments = pd.getSpecific("arguments");
        argsList = parseArgsLine(arguments);
        output = Optional.ofNullable(pd.getSpecific("output"))
                         .map(o -> Output.valueOf(o.toUpperCase(Locale.ENGLISH)))
                         .orElse(Output.STDOUT);
    }

    @Override
    public void readProperties(PropertiesManager pm) {
        cmd = resolvCmdPath(pm.getProperty("path", ""), cmd);
    }

    public Boolean configure() {
        if (cmd == null) {
            return false;
        }
        List<String> cmdList = new ArrayList<>();
        cmdList.add(cmd);
        cmdList.addAll(argsList);
        processBuilder = new ProcessBuilder(cmdList); //.redirectInput(ProcessBuilder.Redirect.DISCARD);
        return true;
    }

    protected String resolvCmdPath(String path, String trycmd) {
        Set<String> pathElements = new LinkedHashSet<>();
        pathElements.add("");
        pathElements.addAll(Arrays.asList(path.split(";")));
        String envPath = System.getenv("PATH");
        if (envPath != null && !envPath.isEmpty()) {
            pathElements.addAll(Arrays.asList(envPath.split(File.pathSeparator)));
        }
        log(Level.DEBUG, "Will look for %s in %s", trycmd, pathElements);
        String resolvedCmd = null;
        for (String pathdir: pathElements) {
            File tryfile = new File(pathdir, trycmd);
            log(Level.TRACE, "Trying if %s can execute", tryfile);
            if (tryfile.canExecute()) {
                log(Level.DEBUG, "Will use %s as a command", tryfile.getAbsolutePath());
                resolvedCmd = tryfile.getAbsolutePath();
                break;
            }
        }
        if (resolvedCmd == null) {
            log(Level.ERROR, "Command %s not found", trycmd);
        }
        return resolvedCmd;
    }

    public Map<String, Number> getNewSampleValues() {
        return launchCmd().map(this::resolveSampleValues).orElseGet(Map::of);
    }

    protected Map<String, Number> resolveSampleValues(String output) {
        String[] values = output.split(":");
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
            double value = Util.parseStringNumber(values[i + 1], Double.NaN);
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

    protected Optional<String> launchCmd() {
        String perfstring = null;
        Process perfProcess = null;
        boolean needsToKill = false;
        try {
            log(Level.DEBUG, "Executing: %s %s", cmd, arguments);
            perfProcess = processBuilder.start();
            Supplier<InputStream> dataStream = switch (output) {
                case STDOUT -> perfProcess::getInputStream;
                case STDERR -> perfProcess::getErrorStream;
            };
            try (InputStream  stdout = dataStream.get()) {
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdout));
                perfstring = stdoutReader.readLine();
            }
            needsToKill = ! perfProcess.waitFor(getTimeout(), TimeUnit.SECONDS);
            if (perfProcess.exitValue() != 0) {
                perfstring = null;
                log(Level.ERROR, " command '%s %s' failed with status %s", cmd, arguments, perfProcess.exitValue());
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
        return Optional.ofNullable(perfstring);
    }

    protected List<String> parseArgsLine(String input) {
        try (Reader r = new StringReader(input)) {
            return parseArgsLine(r);
        } catch (IOException e) {
            return List.of();
        }
    }

    protected List<String> parseArgsLine(Reader input) throws IOException {
        StreamTokenizer tokenizer = new StreamTokenizer(input);
        tokenizer.wordChars(33, 255);
        tokenizer.whitespaceChars(0, ' ');
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');
        List<String> result = new ArrayList<>();
        int token;
        String previousWord = "";
        while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            if (token != StreamTokenizer.TT_EOL) {
                String currentWord = previousWord.isEmpty() ? tokenizer.sval : previousWord + tokenizer.sval;
                if (currentWord.endsWith("\\")) {
                    previousWord =  currentWord.substring(0, currentWord.length() - 1) + " ";
                } else {
                    previousWord = "";
                    result.add(currentWord);
                }
            }
        }

        if (! previousWord.isEmpty()) {
            result.add(previousWord);
        }

        return result;
    }

    @Override
    public String getSourceType() {
        return "external command";
    }

    public String getOutput() {
        return output.name();
    }

    public void setOutput(String output) {
        this.output = Output.valueOf(output.toUpperCase(Locale.ENGLISH));
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
        argsList = parseArgsLine(arguments);
    }

    public void setArgsList(List<String> argsList) {
        this.argsList = List.copyOf(argsList);
        this.arguments = String.join(" ", argsList);
    }

}
