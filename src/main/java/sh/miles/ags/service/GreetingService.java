package sh.miles.ags.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import sh.miles.ags.lang.LanguageBundle;
import sh.miles.ags.load.LanguageLoader;
import sh.miles.ags.load.NativeSupportLanguageLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GreetingService implements Runnable {

    private static final Gson GSON = new Gson();

    private String languageFolder;
    private String language;
    private boolean replaceWithDefaults;
    private boolean debug;

    @Override
    public void run() {
        readConfig();
        final Path languageFolderPath = Path.of(".").resolve(languageFolder);
        NativeSupportLanguageLoader.exportLoadedLanguages(languageFolderPath, this.replaceWithDefaults);
        final LanguageLoader loader = new LanguageLoader(this.language, languageFolderPath);

        final LanguageBundle bundle;
        try {
            bundle = loader.load(GSON);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (debug) {
            System.out.printf("""
                    ACTIVATED GREETING SERVICE DEBUG OPTION
                    bundle-id: %s
                    END DEBUG GREETING SERVICE DEBUG OPTION
                    %n""", bundle.getId());
        }

        System.out.println(bundle.get("standard.greeting"));
    }

    private void readConfig() {
        final Path config = Path.of(".").resolve("config.json");
        if (Files.notExists(config)) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/config.json")))) {
                Files.writeString(config, reader.lines().collect(Collectors.joining("\n")), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        final JsonObject object;
        try {
            object = GSON.fromJson(Files.newBufferedReader(config), JsonObject.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        languageFolder = object.get("language_folder").getAsString();
        language = object.get("language").getAsString().replace("-", File.separator);
        replaceWithDefaults = object.get("replace_with_defaults").getAsBoolean();
        debug = object.get("debug").getAsBoolean();
    }

}
