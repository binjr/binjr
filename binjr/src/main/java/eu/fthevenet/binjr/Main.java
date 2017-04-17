package eu.fthevenet.binjr;

import eu.fthevenet.util.logging.Profiler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

/**
 * The entry point fo the application.
 *
 * @author Frederic Thevenet
 */
public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);



    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info(() -> "Starting binjr");
        Parent root = FXMLLoader.load(getClass().getResource("/views/MainView.fxml"));

        primaryStage.setTitle("binjr");
        primaryStage.getIcons().addAll(
                new Image(getClass().getResourceAsStream("/icons/binjr_16.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_32.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_48.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_128.png")),
                new Image(getClass().getResourceAsStream("/icons/binjr_256.png")));

        try (Profiler p = Profiler.start("Set scene", logger::trace)) {
            primaryStage.setScene(new Scene(root));
        }
        try (Profiler p = Profiler.start("show", logger::trace)) {

            primaryStage.show();
        }

        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }

    }

    /**
     * The entry point fo the application.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Disabling accessibility support could work around hanging issue on Windows  10
        // see "https://bugs.openjdk.java.net/browse/JDK-8132897"
        // System.setProperty("glass.accessible.force", "false");
        launch(args);
    }

}
