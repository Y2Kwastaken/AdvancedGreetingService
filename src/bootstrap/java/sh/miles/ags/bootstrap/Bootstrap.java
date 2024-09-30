package sh.miles.ags.bootstrap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstraps the main
 */
public class Bootstrap {

    public static final String LAUNCH_RESOURCE = "launch";
    public static final String LIBS_EXTERNAL_FOLDER = "libs";
    public static final String VERSIONS_EXTERNAL_FOLDER = "versions";

    public static void main(String[] args) {
        try {
            new Bootstrap().bootstrap(args);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.err.println("Failed to extract service libraries, exiting!");
        }
    }

    public void bootstrap(String[] args) throws Exception {
        final String launchClassName = readResource(LAUNCH_RESOURCE, BufferedReader::readLine);
        final Path librariesDirectory = Path.of(System.getProperty("libdir", LIBS_EXTERNAL_FOLDER));
        Files.createDirectories(librariesDirectory);

        final List<URL> extractedLibraries = new ArrayList<>();
        readLibraryEntries(LIBS_EXTERNAL_FOLDER, ".", extractedLibraries);
        readLibraryEntries(VERSIONS_EXTERNAL_FOLDER, ".", extractedLibraries);

        final ClassLoader loader = this.getClass().getClassLoader();
        final URLClassLoader serviceLoader = new URLClassLoader(extractedLibraries.toArray(URL[]::new), loader);
        System.out.println("Starting Greeting Service");
        Thread serviceThread = new Thread(() -> {
            try {
                Class<?> mainClass = Class.forName(launchClassName, true, serviceLoader);
                final MethodHandle handle = MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(Void.TYPE, String[].class));
                handle.invokeExact(args);
            } catch (Throwable e) {
                e.printStackTrace(System.out);
                System.err.println("Failed to start Greeting Service");
            }
        });
        serviceThread.setContextClassLoader(serviceLoader);
        serviceThread.start();
    }

    private void readLibraryEntries(final String librariesDirectory, final String outputDirectory, List<URL> extractedLibraries) throws Exception {
        List<LibraryEntry> entries = this.readResource(librariesDirectory + ".list", (reader) -> reader.lines().map(LibraryEntry::parse).toList());
        final Path libsDir = Path.of(outputDirectory).resolve(librariesDirectory);
        for (final LibraryEntry library : entries) {
            final Path output = libsDir.resolve(library.path);
            this.checkAndExtractJar(librariesDirectory, library, output);
            extractedLibraries.add(output.toUri().toURL());
        }
    }

    private void checkAndExtractJar(final String librariesDirectory, LibraryEntry library, Path outputFile) throws Exception {
        if (Files.notExists(outputFile) || !checkIntegrity(outputFile, library.hash())) {
            System.out.printf("Unpacking %s (%s) to %s%n", library.path, library.hash, outputFile);
            this.extract(librariesDirectory, library.path, outputFile);
        }
    }

    private void extract(final String librariesDirectory, final String jarPath, Path outputFile) throws Exception {
        Files.createDirectories(outputFile.getParent());
        try (InputStream input = this.getClass().getResourceAsStream("/META-INF/" + librariesDirectory + "/" + jarPath)) {
            if (input == null) {
                throw new IllegalStateException("Declared library " + jarPath + " not found");
            }
            Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private <T> T readResource(final String resource, final ResourceParser<T> parser) throws Exception {
        String fullPath = "/META-INF/" + resource;
        try (InputStream is = this.getClass().getResourceAsStream(fullPath)) {
            if (is == null) {
                throw new IllegalStateException("Resource " + fullPath + " was not found");
            }
            return parser.parse(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        }
    }

    private static String byteToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(Character.forDigit(b >> 4 & 0xF, 16));
            result.append(Character.forDigit(b >> 0 & 0xF, 16));
        }
        return result.toString();
    }

    private static boolean checkIntegrity(Path file, String expectedHash) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream output = Files.newInputStream(file, new OpenOption[0]);) {
            output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            String actualHash = Bootstrap.byteToHex(digest.digest());
            if (actualHash.equalsIgnoreCase(expectedHash)) {
                boolean bl = true;
                return bl;
            }
            System.out.printf("Expected file %s to have hash %s, but got %s%n", file, expectedHash, actualHash);
        }
        return false;
    }

    public record LibraryEntry(String path, String hash) {
        public static LibraryEntry parse(final String line) {
            final String[] split = line.split("  ");
            if (split.length != 2) {
                throw new IllegalStateException("Malformed library entry: " + line);
            }

            return new LibraryEntry(split[1], split[0]);
        }
    }

    /**
     * Defined behavior for parsing from resource files
     *
     * @param <T> the result parse type
     */
    @FunctionalInterface
    private interface ResourceParser<T> {
        /**
         * Defines parsing behavior fro the {@link BufferedReader}
         *
         * @param reader the {@link BufferedReader}
         * @return the output parsed type
         * @throws Exception the exception thrown if parsing has failed
         */
        T parse(BufferedReader reader) throws Exception;
    }
}
