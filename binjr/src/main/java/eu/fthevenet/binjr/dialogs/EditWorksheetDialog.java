package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.charts.XYChartSelection;
import eu.fthevenet.binjr.controls.ZonedDateTimePicker;
import eu.fthevenet.binjr.data.workspace.ChartType;
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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

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
    private final Set<String> suggestedNames;
    public static final String SUGGEST_WORSHEET_NAMES = "suggest_worsheet_names";


//    public EditWorksheetDialog(Node owner){
//        this(new Worksheet<T>(), owner);
//    }

    /**
     * Initializes a new instance of the {@link EditWorksheetDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public EditWorksheetDialog(Worksheet worksheet, Node owner) {
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("New Worksheet");
        Preferences prefs = Preferences.userRoot().node(BINJR_SUGGEST);
        suggestedNames = new HashSet<>(Arrays.asList(prefs.get(SUGGEST_WORSHEET_NAMES, "").split(" ")));

        try {
            FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("/views/EditWorkSheetDialog.fxml"));
            Parent parent = fXMLLoader.load();
            this.setDialogPane((DialogPane) parent);

            TextField nameField = (TextField) parent.lookup("#nameField");
            TextField timezoneField = (TextField) parent.lookup("#timezoneField");
            ChoiceBox<ChartType> chartTypeChoice =(ChoiceBox<ChartType>) parent.lookup("#chartTypeChoice");
            ZonedDateTimePicker fromDatePicker = (ZonedDateTimePicker)parent.lookup("#fromDatePicker");
            ZonedDateTimePicker toDatePicker = (ZonedDateTimePicker)parent.lookup("#toDatePicker");

            nameField.textProperty().bindBidirectional(worksheet.nameProperty());
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
            formatter.valueProperty().bindBidirectional(worksheet.timeZoneProperty());
            timezoneField.setTextFormatter(formatter);

            fromDatePicker.dateTimeValueProperty().bindBidirectional(worksheet.fromDateTimeProperty());
            toDatePicker.dateTimeValueProperty().bindBidirectional(worksheet.toDateTimeProperty());
//            fromDatePicker.setDateTimeValue(worksheet.getSelection().getStartX());
//            toDatePicker.setDateTimeValue(worksheet.getSelection().getEndX());
            chartTypeChoice.getItems().setAll(ChartType.values());

            chartTypeChoice.getSelectionModel().select(worksheet.getChartType());

            autoCompletionBinding = TextFields.bindAutoCompletion(nameField, suggestedNames);
            final Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);

            okButton.addEventFilter(ActionEvent.ACTION, ae -> {
                try {
                    worksheet.setChartType(chartTypeChoice.getValue());
                    autoCompletionLearnWord(nameField);
                } catch (DateTimeException de) {
                    Dialogs.notifyError("Invalid Timezone", de.getLocalizedMessage(), Pos.CENTER, timezoneField);
                    ae.consume();
                }
            });

            this.setResultConverter(dialogButton -> {
                        ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                        if (data == ButtonBar.ButtonData.OK_DONE) {
                            suggestedNames.stream().reduce((s, s2) -> s + " " + s2).ifPresent(s -> prefs.put(SUGGEST_WORSHEET_NAMES, s));
                            return worksheet;
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
        suggestedNames.add(field.getText());
        if (autoCompletionBinding != null) {
            autoCompletionBinding.dispose();
        }
        autoCompletionBinding = TextFields.bindAutoCompletion(field, suggestedNames);
    }

}
