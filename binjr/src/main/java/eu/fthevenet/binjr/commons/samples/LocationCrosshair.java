package eu.fthevenet.binjr.commons.samples;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

class LocationCrosshair extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        AnchorPane root = new AnchorPane();

        Pane crosshairArea = new Pane();
        crosshairArea.setPrefWidth(300);
        crosshairArea.setPrefHeight(300);
        crosshairArea.setTranslateX(20);
        crosshairArea.setTranslateY(20);
        crosshairArea.setStyle("-fx-border-color:black");
        crosshairArea.setCursor(Cursor.NONE);

        final Label label = new Label("");
        crosshairArea.getChildren().add(label);

        final Line horizontalLine = new Line(0,0,crosshairArea.getPrefWidth(),0);
        final Line verticalLine = new Line(0,0,0,crosshairArea.getPrefHeight());

        crosshairArea.getChildren().add(horizontalLine);
        crosshairArea.getChildren().add(verticalLine);

        crosshairArea.setOnMouseMoved(event -> {
            label.setVisible(true);
            label.setText(" Location: "+event.getX()+", "+event.getY());

            horizontalLine.setStartY(event.getY());
            horizontalLine.setEndY(event.getY());

            verticalLine.setStartX(event.getX());
            verticalLine.setEndX(event.getX());

        });
        crosshairArea.setOnMouseExited(event -> label.setVisible(false));
        root.getChildren().add(crosshairArea);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Location Crosshair");
        stage.setWidth(500);
        stage.setHeight(470);
        stage.show();
    }
}