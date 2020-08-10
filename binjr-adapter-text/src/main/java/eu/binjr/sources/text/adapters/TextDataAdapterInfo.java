
package eu.binjr.sources.text.adapters;


import eu.binjr.core.data.adapters.AdapterMetadata;
import eu.binjr.core.data.adapters.BaseDataAdapterInfo;
import eu.binjr.core.data.adapters.SourceLocality;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.preferences.AppEnvironment;


/**
 * Defines the metadata associated with TextDataAdapterInfo.
 *
 * @author Frederic Thevenet
 */
@AdapterMetadata(
        name = "Text",
        description = "Text File Data Adapter",
        copyright = AppEnvironment.COPYRIGHT_NOTICE,
        license = AppEnvironment.LICENSE,
        siteUrl = AppEnvironment.HTTP_WWW_BINJR_EU,
        adapterClass = TextDataAdapter.class,
        dialogClass = TextDataAdapterDialog.class,
        preferencesClass = TextAdapterPreferences.class,
        sourceLocality = SourceLocality.LOCAL,
        apiLevel = AppEnvironment.PLUGIN_API_LEVEL
)
public class TextDataAdapterInfo extends BaseDataAdapterInfo {

    /**
     * Initialises a new instance of the {@link TextDataAdapterInfo} class.
     */
    public TextDataAdapterInfo() throws CannotInitializeDataAdapterException {
        super(TextDataAdapterInfo.class);
    }
}
