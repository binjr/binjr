/*
 *    Copyright 2018 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package eu.fthevenet.binjr.data.workspace;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Map;
import java.util.stream.Collectors;

public class ParameterMapAdapter extends XmlAdapter<DataAdapterParameters, Map<String, String>> {
    public ParameterMapAdapter() {
    }

    @Override
    public Map<String, String> unmarshal(DataAdapterParameters v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.parameters.stream().collect(Collectors.toMap(o -> o.getName(), o -> o.getValue()));
    }

    @Override
    public DataAdapterParameters marshal(Map<String, String> v) throws Exception {
        if (v == null) {
            return null;
        }
        return new DataAdapterParameters(v.entrySet()
                .stream()
                .map(entry -> new DataAdapterParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }
}
