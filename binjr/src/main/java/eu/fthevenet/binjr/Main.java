package eu.fthevenet.binjr;

import eu.fthevenet.binjr.commons.logging.Profiler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception{
        logger.info(()-> "Starting binjr");
        Parent root = FXMLLoader.load(getClass().getResource("/views/mainView.fxml"));

        primaryStage.setTitle("binjr");

        try(Profiler p= Profiler.start("Set scene", logger::trace)) {
            primaryStage.setScene(new Scene(root));
        }
        try(Profiler p= Profiler.start("show", logger::trace)) {
            primaryStage.show();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }



}
