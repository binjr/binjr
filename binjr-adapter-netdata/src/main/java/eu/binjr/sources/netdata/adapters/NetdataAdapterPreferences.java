package eu.binjr.sources.netdata.adapters;

import eu.binjr.common.preferences.Preference;
import eu.binjr.core.data.adapters.DataAdapterPreferences;
import eu.binjr.sources.netdata.api.GroupingMethod;

public class NetdataAdapterPreferences extends DataAdapterPreferences {
    public Preference<Boolean> disableServerSideDownsampling = booleanPreference("disableServerSideDownsampling", true);
    public Preference<Boolean> disableTimeFrameAlignment = booleanPreference("disableTimeFrameAlignment", false);
    public Preference<GroupingMethod> groupingMethod = enumPreference(GroupingMethod.class, "groupingMethod", GroupingMethod.AVERAGE);
    public Preference<Number> groupingTime = integerPreference("groupingTime", 0);

    private NetdataAdapterPreferences() {
        super(NetdataAdapter.class);
    }

    public static NetdataAdapterPreferences getInstance() {
        return NetdataAdapterPreferencesHolder.instance;
    }

    private static class NetdataAdapterPreferencesHolder {
        private final static NetdataAdapterPreferences instance = new NetdataAdapterPreferences();
    }
}
