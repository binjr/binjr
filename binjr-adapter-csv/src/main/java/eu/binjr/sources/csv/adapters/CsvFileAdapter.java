/*
 *    Copyright 2017-2025 Frederic Thevenet
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

package eu.binjr.sources.csv.adapters;

import com.google.gson.Gson;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.exceptions.FetchingDataFromAdapterException;
import eu.binjr.core.data.exceptions.InvalidAdapterParameterException;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.sources.csv.data.parsers.BuiltInCsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CsvEventFormat;
import eu.binjr.sources.csv.data.parsers.CsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CustomCsvParsingProfile;
import javafx.scene.control.TreeItem;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link DataAdapter} implementation used to feed {@link XYChartsWorksheet} instances
 * with  data from a local CSV formatted file.
 *
 * @author Frederic Thevenet
 */
public class CsvFileAdapter extends IndexBackedFileAdapter<CsvEventFormat, CsvParsingProfile> {
    private static final Logger logger = Logger.create(CsvFileAdapter.class);
    private static final Gson gson = new Gson();
    protected static final String CSV_PATH = "csvPath";
    private final ThreadLocal<NumberFormat> numberFormatters;


    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with a set of default values.
     *
     * @throws DataAdapterException if the {@link DataAdapter} could not be initializes.
     */
    public CsvFileAdapter() throws DataAdapterException {
        this("", ZoneId.systemDefault(), "utf-8", BuiltInCsvParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class for the provided file and time zone.
     *
     * @param filePath the path to the csv file.
     * @param zoneId   the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String filePath, ZoneId zoneId) throws DataAdapterException {
        this(filePath, zoneId, "utf-8", BuiltInCsvParsingProfile.ISO);
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class with the provided parameters.
     *
     * @param filePath       the path to the csv file.
     * @param zoneId         the time zone to used.
     * @param encoding       the encoding for the csv file.
     * @param parsingProfile a pattern to decode time stamps.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String filePath,
                          ZoneId zoneId,
                          String encoding,
                          CsvParsingProfile parsingProfile)
            throws DataAdapterException {
        super();
        initParams(zoneId, filePath, encoding, parsingProfile);
        numberFormatters = ThreadLocal.withInitial(() -> NumberFormat.getNumberInstance(parsingProfile.getNumberFormattingLocale()));
    }

    @Override
    public FilterableTreeItem<SourceBinding> getBindingTree() throws DataAdapterException {
        FilterableTreeItem<SourceBinding> tree = new FilterableTreeItem<>(
                new TimeSeriesBinding.Builder()
                        .withLabel(getSourceName())
                        .withPath("/")
                        .withAdapter(this)
                        .build());
        try (InputStream in = Files.newInputStream(filePath)) {
            List<String> headers = parser.getDataColumnHeaders(in);
            for (int i = 0; i < headers.size(); i++) {
                if (i != parsingProfile.getTimestampColumn()) {
                    String header = headers.get(i).isBlank() ? "Column #" + i : headers.get(i);
                    var b = new TimeSeriesBinding.Builder()
                            .withLabel(Integer.toString(i))
                            .withPath(getId() + "/" + filePath.toString())
                            .withLegend(header)
                            .withParent(tree.getValue())
                            .withAdapter(this)
                            .build();
                    tree.getInternalChildren().add(new TreeItem<>(b));
                }
            }
        } catch (IOException e) {
            throw new FetchingDataFromAdapterException(e);
        }
        return tree;
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(ZONE_ID, zoneId.toString());
        params.put(ENCODING, encoding);
        params.put(PARSING_PROFILE, gson.toJson(CustomCsvParsingProfile.of(parsingProfile)));
        params.put(CSV_PATH, filePath.toString());
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
                validateParameterNullity(params, CSV_PATH),
                validateParameterNullity(params, ENCODING),
                gson.fromJson(validateParameterNullity(params, PARSING_PROFILE), CustomCsvParsingProfile.class));
    }

    private void initParams(ZoneId zoneId,
                            String csvPath,
                            String encoding,
                            CsvParsingProfile parsingProfile) {
        this.zoneId = zoneId;
        this.filePath = Path.of(csvPath);
        this.encoding = encoding;
        this.parsingProfile = parsingProfile;
        this.parser = new CsvEventFormat(parsingProfile, zoneId, Charset.forName(encoding));
    }

    @Override
    protected Document mapEventToDocument(Document doc, ParsedEvent event) {
        event.getTextFields().forEach((key, value) -> doc.add(new StoredField(key, formatToDouble(value))));
        return doc;
    }

    private double formatToDouble(String value) {
        if (value != null) {
            try {
                return numberFormatters.get().parse(value).doubleValue();
            } catch (Exception e) {
                logger.trace(() -> "Failed to convert '" + value + "' to double");
            }
        }
        return Double.NaN;
    }
}
