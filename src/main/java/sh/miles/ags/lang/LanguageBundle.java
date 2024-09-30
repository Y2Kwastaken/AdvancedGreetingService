package sh.miles.ags.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class LanguageBundle {

    private final String id;
    private final Map<String, String> langMap;

    public LanguageBundle(final String id, Map<String, String> langMap) {
        this.id = id;
        this.langMap = langMap;
    }

    public String get(String key) {
        return langMap.getOrDefault(key, "Not Found");
    }

    public String getUnsafe(String key) {
        return langMap.get(key);
    }

    public String getId() {
        return id;
    }

    public static LanguageBundle fromJsonFile(final JsonElement root) {
        final JsonObject parent = root.getAsJsonObject();
        final String id = parent.get("id").getAsString();
        final Map<String, String> lookup = new LinkedHashMap<>();
        for (final String key : parent.keySet()) {
            if (key.equals("id")) {
                continue;
            }
            lookup.put(key, parent.get(key).getAsString());
        }

        return new LanguageBundle(id, lookup);
    }
}
