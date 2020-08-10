package eu.binjr.sources.text.adapters;


import com.google.gson.Gson;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.adapters.DataAdapterPreferences;

public class TextAdapterPreferences extends DataAdapterPreferences {
    private static final Gson gson = new Gson();

    public ObservablePreference<Number> defaultTextViewFontSize = integerPreference("defaultTextViewFontSize", 10);

    public ObservablePreference<String[]> foldersToVisit = objectPreference(String[].class,
            "foldersToVisit",
            new String[]{"config", "files", "gct", "hosts"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));

    public ObservablePreference<String[]> textFileExtensions = objectPreference(String[].class,
            "textFileExtensions",
            new String[]{".xml", ".txt", ".env", ".properties"},
            gson::toJson,
            s -> gson.fromJson(s, String[].class));


    public TextAdapterPreferences(Class<? extends DataAdapter<?>> dataAdapterClass) {
        super(dataAdapterClass);
    }
}
