/*
 *    Copyright 2025 Frederic Thevenet
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
 */

package eu.binjr.sources.json.adapters;

import com.google.gson.Gson;
import eu.binjr.common.io.JarFsPathResolver;
import eu.binjr.common.javafx.controls.TreeViewUtils;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.sources.json.data.parsers.BuiltInJsonParsingProfile;
import eu.binjr.sources.json.data.parsers.JsonEventFormat;
import eu.binjr.sources.json.data.parsers.JsonParsingProfile;
import eu.binjr.sources.json.data.parsers.CustomJsonParsingProfile;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local JSON formatted file.
 *
 * @author Frederic Thevenet
 */
public class JsonFileAdapter extends IndexBackedFileAdapter<JsonEventFormat, JsonParsingProfile> {
    private static final Gson gson = new Gson();

    /**
     * Initializes a new instance of the {@link JsonFileAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public JsonFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", BuiltInJsonParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link JsonFileAdapter} class for the provided file and time zone.
     *
     * @param jsonPath the path to the json file.
     * @param zoneId   the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JsonFileAdapter(String jsonPath, ZoneId zoneId) throws DataAdapterException {
        this(jsonPath, zoneId, "utf-8", BuiltInJsonParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link JsonFileAdapter} class with the provided parameters.
     *
     * @param jsonPath       the path to the json file.
     * @param zoneId         the time zone to used.
     * @param encoding       the encoding for the json file.
     * @param parsingProfile a pattern to decode time stamps.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public JsonFileAdapter(String jsonPath,
                           ZoneId zoneId,
                           String encoding,
                           JsonParsingProfile parsingProfile)
            throws DataAdapterException {
        super();
        initParams(zoneId, jsonPath, encoding, parsingProfile);
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PARSING_PROFILE, gson.toJson(CustomJsonParsingProfile.of(parsingProfile)));
        params.put(PATH, filePath.toString());
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params) throws DataAdapterException {
        if (params == null) {
            throw new InvalidAdapterParameterException("Could not find parameter list for adapter " + getSourceName());
        }
        initParams(validateParameter(params, ZONE_ID,
                        s -> {
                            if (s == null) {
                                throw new InvalidAdapterParameterException("Parameter '" + ZONE_ID + "'  is missing in adapter " + getSourceName());
                            }
                            return ZoneId.of(s);
                        }),
                validateParameterNullity(params, PATH),
                validateParameterNullity(params, ENCODING),
                gson.fromJson(validateParameterNullity(params, PARSING_PROFILE), CustomJsonParsingProfile.class));
    }

    private void initParams(ZoneId zoneId,
                            String jsonPath,
                            String encoding,
                            JsonParsingProfile parsingProfile) {
        this.zoneId = zoneId;
        this.filePath = Path.of(jsonPath);
        this.encoding = encoding;
        this.parsingProfile = parsingProfile;
        this.parser = new JsonEventFormat(parsingProfile, zoneId, Charset.forName(encoding));
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        int i = 0;
        for (var defs : parsingProfile.getJsonDefinition().series()) {
            Path index = JarFsPathResolver.get(defs.path());
            var currentBranch = tree;
            for (int j = 0; j < index.getNameCount(); j++) {
                var currentName = index.getName(j);
                Path subpath = index.getRoot().resolve(index.subpath(0, j + 1));
                FilterableTreeItem<SourceBinding> finalCurrentBranch = currentBranch;
                FilterableTreeItem<SourceBinding> branchNode = (FilterableTreeItem<SourceBinding>) TreeViewUtils.findFirstInTree(
                        tree, t -> JarFsPathResolver.get(t.getValue().getLabel()).equals(subpath)).orElseGet(() -> {
                    final var newBranch = new TimeSeriesBinding.Builder()
                            .withLabel(subpath.toString())
                            .withPath(getId() + "/" + filePath.toString())
                            .withLegend(currentName.toString())
                            .withParent(tree.getValue())
                            .withGraphType(defs.graphType())
                            .withUnitName(defs.unit())
                            .withPrefix(defs.prefix())
                            .withAdapter(this)
                            .build();
                    final FilterableTreeItem<SourceBinding> parentBranch = new FilterableTreeItem<>(newBranch);
                    finalCurrentBranch.getInternalChildren().add(parentBranch);
                    return parentBranch;
                });
                currentBranch = branchNode;
            }
        }
        return tree;
    }

    @Override
    protected Document mapEventToDocument(Document doc, ParsedEvent event) {
        event.getNumberFields().forEach((key, value) -> doc.add(new StoredField(key, value.doubleValue())));
        return doc;
    }

}
