package eu.fthevenet.binjr.dialogs;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;
import org.controlsfx.dialog.ExceptionDialog;

/**
 * @author Frederic Thevenet
 */
public class Dialogs {
    private static final Logger logger = LogManager.getLogger(Dialogs.class);

    public static void displayException(Exception e) {
        displayException(e.getLocalizedMessage(), e, null);
    }

    public static void displayException(String header, Exception e) {
        displayException(header, e, null);
    }

    public static void displayException(String header, Exception e, Node owner) {
        logger.error(header, e);
        ExceptionDialog dlg = new ExceptionDialog(e);
        dlg.initStyle(StageStyle.UTILITY);
        dlg.initOwner(getStage(owner));
        dlg.getDialogPane().setHeaderText(header);
        dlg.showAndWait();
    }

    public static void notifyError(String title, String message, Node owner) {
        Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(3))
                .position(Pos.CENTER)
                .owner(owner).showError();
    }

    public static Stage getStage(Node node) {
        if (node != null && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }
}
