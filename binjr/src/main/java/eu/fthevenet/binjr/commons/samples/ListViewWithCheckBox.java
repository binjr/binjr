package eu.fthevenet.binjr.commons.samples;
import eu.fthevenet.binjr.controllers.SelectableListItem;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ListViewWithCheckBox extends Application {

    @Override
    public void start(Stage primaryStage) {
        ListView<SelectableListItem> listView = new ListView<>();
        for (int i=1; i<=20; i++) {
            SelectableListItem item = new SelectableListItem("SelectableListItem "+i, false);

            // observe item's on property and display message if it changes:
            item.selectedProperty().addListener((obs, wasOn, isNowOn) -> {
                System.out.println(item.getName() + " changed on state from "+wasOn+" to "+isNowOn);
            });

            listView.getItems().add(item);
        }

        listView.setCellFactory(CheckBoxListCell.forListView(new Callback<SelectableListItem, ObservableValue<Boolean>>() {
            @Override
            public ObservableValue<Boolean> call(SelectableListItem item) {
                return item.selectedProperty();
            }
        }));

        BorderPane root = new BorderPane(listView);
        Scene scene = new Scene(root, 250, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}