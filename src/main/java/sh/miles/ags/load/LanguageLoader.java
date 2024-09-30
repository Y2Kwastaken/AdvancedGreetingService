package sh.miles.ags.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import sh.miles.ags.lang.LanguageBundle;
import sh.miles.ags.load.exception.LanguageNotFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class LanguageLoader {

    private final String language;
    private final Path languageFolder;

    public LanguageLoader(final String language, final Path languageFolder) {
        this.language = language;
        this.languageFolder = languageFolder;
    }

    public LanguageBundle load(final Gson jsonInterpreter) throws LanguageNotFoundException, IOException {
        final Path langFolder = languageFolder.resolve(language.replace("-", File.separator));
        if (Files.notExists(langFolder)) {
            throw new LanguageNotFoundException("Could not find language %s at location %s".formatted(language, langFolder));
        }

        if (Files.notExists(langFolder.resolve("lang.json"))) {
            throw new LanguageNotFoundException("Could not find language %s, folder was found at %s, however, no lang.json was found".formatted(language, langFolder));
        }

        try (final BufferedReader reader = Files.newBufferedReader(langFolder.resolve("lang.json"))) {
            final String json = reader.lines().collect(Collectors.joining(" "));
            return LanguageBundle.fromJsonFile(jsonInterpreter.fromJson(json, JsonElement.class));
        }
    }
}
