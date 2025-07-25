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
import eu.binjr.common.io.FileSystemBrowser;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.*;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.indexes.parser.ParsedEvent;
import eu.binjr.core.data.workspace.XYChartsWorksheet;
import eu.binjr.sources.csv.data.parsers.BuiltInCsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CsvEventFormat;
import eu.binjr.sources.csv.data.parsers.CsvParsingProfile;
import eu.binjr.sources.csv.data.parsers.CustomCsvParsingProfile;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.eclipse.fx.ui.controls.tree.FilterableTreeItem;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.time.ZoneId;
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
        this("", ZoneId.systemDefault(), "utf-8", BuiltInCsvParsingProfile.ISO, new String[]{"*"}, new String[]{"*.csv"});
    }

    /**
     * Initializes a new instance of the {@link CsvFileAdapter} class for the provided file and time zone.
     *
     * @param filePath the path to the csv file.
     * @param zoneId   the time zone to used.
     * @throws DataAdapterException if the {@link DataAdapter} could not be initialized.
     */
    public CsvFileAdapter(String filePath, ZoneId zoneId) throws DataAdapterException {
        this(filePath, zoneId, "utf-8", BuiltInCsvParsingProfile.ISO, new String[]{"*"}, new String[]{"*.csv"});
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
                          CsvParsingProfile parsingProfile,
                          String[] folderFilters,
                          String[] fileExtensionsFilters)
            throws DataAdapterException {
        super(filePath, zoneId, encoding, parsingProfile, folderFilters, fileExtensionsFilters);
        numberFormatters = ThreadLocal.withInitial(() -> NumberFormat.getNumberInstance(parsingProfile.getNumberFormattingLocale()));
    }

    @Override
    protected void attachLeafNode(FileSystemBrowser.FileSystemEntry fsEntry,
                                  FilterableTreeItem<SourceBinding> fileBranch) throws DataAdapterException {
        try (InputStream in = fileBrowser.getData(fsEntry.getPath().toString())) {
            List<String> headers = eventFormat.getDataColumnHeaders(in);
            for (int i = 0; i < headers.size(); i++) {
                if (i != parsingProfile.getTimestampColumn()) {
                    String header = headers.get(i).isBlank() ? "Column #" + i : headers.get(i);
                    FilterableTreeItem<SourceBinding> filenode = new FilterableTreeItem<>(
                            new TimeSeriesBinding.Builder()
                                    .withLabel(Integer.toString(i))
                                    .withPath(getId() + "/" + fsEntry.getPath().toString())
                                    .withLegend(header)
                                    .withParent(fileBranch.getValue())
                                    .withAdapter(this)
                                    .build());
                    fileBranch.getInternalChildren().add(filenode);
                }
            }
        } catch (Exception e) {
            throw new DataAdapterException("Failed to create data binding for file " + fsEntry.getPath() + ": " + e.getMessage(), e);
        }
    }


    @Override
    protected CsvEventFormat supplyEventFormat(CsvParsingProfile parsingProfile, ZoneId zoneId, Charset charset) {
        return new CsvEventFormat(parsingProfile, zoneId, Charset.forName(encoding));
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = super.getParams();
        params.put(PARSING_PROFILE, gson.toJson(CustomCsvParsingProfile.of(parsingProfile)));
        return params;
    }

    @Override
    public void loadParams(Map<String, String> params, LoadingContext context) throws DataAdapterException {
        // Convert file path parameter name from old format to new one.
        // WARNING: newly saved workspace will no longer be compatible with older versions.
        if (params.get(CSV_PATH) != null) {
            params.put(PATH, params.get(CSV_PATH));
            params.remove(CSV_PATH);
        }
        super.loadParams(params, context);
        this.parsingProfile = mapParameter(params, PARSING_PROFILE, (p -> gson.fromJson(p, CustomCsvParsingProfile.class)));
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
