package eu.fthevenet.util.ui.dialogs;

import eu.fthevenet.util.ui.controls.ZonedDateTimePicker;
import eu.fthevenet.binjr.data.workspace.ChartType;
import eu.fthevenet.binjr.data.workspace.UnitPrefixes;
import eu.fthevenet.binjr.data.workspace.Worksheet;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * A dialog box to create a new worksheet
 *
 * @author Frederic Thevenet
 */
public class EditWorksheetDialog<T extends Number> extends Dialog<Worksheet> {
    private static final Logger logger = LogManager.getLogger(EditWorksheetDialog.class);
    private static final String BINJR_SUGGEST = "binjr/suggest";
   // private Worksheet<T> result = null;
    private AutoCompletionBinding<String> autoCompletionBinding;
    private final Set<String> suggestedUnits;
    private static final String SUGGEST_WORKSHEET_UNITS = "suggest_worksheet_units";


    /**
     * Initializes a new instance of the {@link EditWorksheetDialog} class.
     *
     * @param worksheet the worksheet to edit
     * @param owner the owner window for the dialog
     */
    public EditWorksheetDialog(Worksheet<T> worksheet, Node owner) {
        // Clone the worksheet before binding it to the UI (we don't want to mutate the provided instance in case the user cancel the edition)
        Worksheet<T> resultWorksheet = new Worksheet<>(worksheet);
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("Worksheet");
        Preferences prefs = Preferences.userRoot().node(BINJR_SUGGEST);
        suggestedUnits = new HashSet<>(Arrays.asList(prefs.get(SUGGEST_WORKSHEET_UNITS, "").split(" ")));

        try {
            FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/EditWorkSheetDialog.fxml"));
            Parent parent = fXMLLoader.load();
            this.setDialogPane((DialogPane) parent);

            TextField nameField = (TextField) parent.lookup("#nameField");
            TextField timezoneField = (TextField) parent.lookup("#timezoneField");
            TextField unitNameField = (TextField) parent.lookup("#unitNameField");
            ChoiceBox<ChartType> chartTypeChoice =(ChoiceBox<ChartType>) parent.lookup("#chartTypeChoice");
            ChoiceBox<UnitPrefixes> unitPrefixesChoice =(ChoiceBox<UnitPrefixes>) parent.lookup("#unitPrefixesChoice");
            ZonedDateTimePicker fromDatePicker = (ZonedDateTimePicker)parent.lookup("#fromDatePicker");
            ZonedDateTimePicker toDatePicker = (ZonedDateTimePicker)parent.lookup("#toDatePicker");

            unitNameField.textProperty().bindBidirectional(resultWorksheet.unitProperty());
            nameField.textProperty().bindBidirectional(resultWorksheet.nameProperty());
            TextFormatter<ZoneId> formatter = new TextFormatter<ZoneId>(new StringConverter<ZoneId>() {
                @Override
                public String toString(ZoneId object) {
                    return object.toString();
                }

                @Override
                public ZoneId fromString(String string) {
                    return ZoneId.of(string);
                }
            });
            formatter.valueProperty().bindBidirectional(resultWorksheet.timeZoneProperty());
            timezoneField.setTextFormatter(formatter);

            fromDatePicker.dateTimeValueProperty().bindBidirectional(resultWorksheet.fromDateTimeProperty());
            toDatePicker.dateTimeValueProperty().bindBidirectional(resultWorksheet.toDateTimeProperty());
            // TODO: ChartType.LINE is not an option for the time being
            chartTypeChoice.getItems().setAll(Arrays.stream(ChartType.values()).filter(v-> v != ChartType.LINE).collect(Collectors.toSet()));
            chartTypeChoice.getSelectionModel().select(resultWorksheet.getChartType());
            resultWorksheet.chartTypeProperty().bind(chartTypeChoice.getSelectionModel().selectedItemProperty());

            unitPrefixesChoice.getItems().setAll(UnitPrefixes.values());
            unitPrefixesChoice.getSelectionModel().select(resultWorksheet.getUnitPrefixes());
            resultWorksheet.unitPrefixesProperty().bind(unitPrefixesChoice.getSelectionModel().selectedItemProperty());

            autoCompletionBinding = TextFields.bindAutoCompletion(unitNameField, suggestedUnits);
            final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

            okButton.addEventFilter(ActionEvent.ACTION, ae -> {
                try {
                    autoCompletionLearnWord(unitNameField);
                } catch (DateTimeException de) {
                    Dialogs.notifyError("Invalid Timezone", de.getLocalizedMessage(), Pos.CENTER, timezoneField);
                    ae.consume();
                }
            });

            this.setResultConverter(dialogButton -> {
                        ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                        if (data == ButtonBar.ButtonData.OK_DONE) {
                            suggestedUnits.stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> prefs.put(SUGGEST_WORKSHEET_UNITS, s));
                            return resultWorksheet;
                        }
                        return null;
                    }
            );
            TextFields.bindAutoCompletion(timezoneField, ZoneId.getAvailableZoneIds());
            timezoneField.setText(ZoneId.systemDefault().toString());
        } catch (IOException e) {
            logger.error("Failed to load /views/EditWorkSheetDialog.fxml", e);
        }
    }

    private void autoCompletionLearnWord(TextField field) {
        suggestedUnits.add(field.getText());
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedUnits);
    }

}
