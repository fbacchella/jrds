package jrds.standalone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import jrds.SecretStore;

public class SecretsHandling extends CommandStarterImpl {

    // Secret sources
    @Parameter(names = {"--secret", "-S"}, description = "Secret")
    String secretValue = null;
    @Parameter(names = {"--file", "-f"}, description = "Password file")
    String fromFile = null;
    @Parameter(names = {"--console", "-c"}, description = "Read from console")
    boolean fromConsole = false;
    @Parameter(names = {"--stdin", "-i"}, description = "Read from stdin")
    boolean fromStdin = false;

    // Identification elements
    @Parameter(names = {"--alias", "-a"}, description = "Secret entry alias")
    String alias = null;
    @Parameter(names = {"--store", "-s"}, description = "The store file", required = true)
    String storeFile = null;

    // Actions
    @Parameter(names = {"--add"}, description = "Add a secret")
    boolean add = false;
    @Parameter(names = {"--del"}, description = "Delete a secret")
    boolean delete = false;
    @Parameter(names = {"--list"}, description = "List secrets")
    boolean list = false;
    @Parameter(names = {"--create"}, description = "Create te store file")
    boolean create = false;

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;

    private byte[] readSecret() throws IOException {
        byte[] secret;
        if (fromConsole) {
            secret = new String(System.console().readPassword()).getBytes(StandardCharsets.UTF_8);
        } else if (secretValue != null) {
            secret = secretValue.getBytes(StandardCharsets.UTF_8);
        } else if (fromStdin) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] readbuffer = new byte[256];
            int read;
            while ((read = System.in.read(readbuffer)) > 0) {
                buffer.write(readbuffer, 0, read);
            }
            secret = buffer.toByteArray();
        } else if (fromFile != null) {
            secret = Files.readAllBytes(Paths.get(fromFile));
        } else {
            throw new IllegalStateException("No secret source defined");
        }
        return secret;
    }

    @Override
    public void start(String[] args) throws Exception {
        JCommander jcom = JCommander
                .newBuilder()
                .addObject(this)
                .acceptUnknownOptions(true)
                .build();
        try {
            jcom.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        if (help) {
            jcom.usage();
            System.exit(0);
        }
        if ((add ? 1 : 0) + (delete ? 1 : 0) + (list ? 1 : 0) + (create ? 1 : 0) != 1) {
            throw new IllegalStateException("A single action is required");
        }
        if ((add ? 1 : 0) + (delete ? 1 : 0)  == 1 && alias == null) {
            throw new IllegalStateException("Alias missing for entry operation");
        }
        if ((fromConsole ? 1 : 0) + (fromStdin ? 1 : 0) + (secretValue != null ? 1 : 0) + (fromFile != null ? 1 : 0) > 1) {
            throw new IllegalStateException("Multiple secret sources given, pick one");
        }
        if ((fromConsole ? 1 : 0) + (fromStdin ? 1 : 0) + (secretValue != null ? 1 : 0) + (fromFile != null ? 1 : 0) == 0) {
            // The default input is console
            fromConsole = true;
        }
        if (add) {
            try (SecretStore sh = SecretStore.load(storeFile)) {
                sh.add(alias, readSecret());
            }
        } else if (delete) {
            try (SecretStore sh = SecretStore.load(storeFile)) {
                sh.delete(alias);
            }
        } else if (list) {
            try (SecretStore sh = SecretStore.load(storeFile)) {
                sh.list().map(Map.Entry::getKey).forEach(System.out::println);
            }
        } else if (create) {
            try (SecretStore sh = SecretStore.create(storeFile)) {
                // Nothing to do
            }
        }
    }

    @Override
    public String getName() {
        return "secrets";
    }

}
