package sh.miles.ags.load;

import sh.miles.ags.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class NativeSupportLanguageLoader {

    private static final List<String> LANGUAGES = List.of(
            "en/gb",
            "en/us",
            "en/uwu"
    );

    private NativeSupportLanguageLoader() {
        throw new UnsupportedOperationException("Can not instantiate NativeSupportLanguageLoader");
    }

    public static void exportLoadedLanguages(final Path languagesFolder, boolean replaceExisting) {
        final String format = "/langs/%s/lang.json";

        System.out.println("export");
        for (final String language : LANGUAGES) {
            System.out.println(language);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream(format.formatted(language))))) {
                final Path exportTo = languagesFolder.resolve("%s/lang.json".formatted(language));
                if (Files.notExists(exportTo.getParent())) {
                    System.out.println("dir already exists");
                    Files.createDirectories(exportTo.getParent());
                }

                if (replaceExisting || Files.notExists(exportTo)) {
                    System.out.println("file already exists");
                    Files.writeString(exportTo, reader.lines().collect(Collectors.joining("\n")));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.printf("end %s%n", language);
        }
        System.out.println("end export");
    }
}
