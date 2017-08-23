/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.binjr.dialogs;

import eu.fthevenet.binjr.preferences.AppEnvironment;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.ExceptionDialog;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Defines helper methods to facilitate the display of common dialog boxes
 *
 * @author Frederic Thevenet
 */
public class Dialogs {
    public static final int NOTIFICATION_DURATION = 8;
    private static final Logger logger = LogManager.getLogger(Dialogs.class);

    /**
     * Display an error notification
     *
     * @param e the exception to display
     */
    public static void notifyException(Throwable e) {
        notifyException(e.getLocalizedMessage(), e, null);
    }

    /**
     * Display an error notification
     *
     * @param header the header text for the dialog
     * @param e      the exception to display
     */
    public static void notifyException(String header, Throwable e) {
        notifyException(header, e, null);
    }

    /**
     * Display an error notification
     *
     * @param title the title for the notification
     * @param e     the exception to notify
     * @param owner the node to which the notification is attached
     */
    public static void notifyException(String title, Throwable e, Node owner) {
        logger.error(title + " - " + e.getMessage());
        if (logger.isDebugEnabled()) {
            logger.debug(title, e);
        }
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(e.getMessage())
                .hideAfter(Duration.seconds(NOTIFICATION_DURATION))
                .position(Pos.BOTTOM_RIGHT)
                .action(new Action("Details", ae -> displayException(title, e, owner)))
                .owner(owner).showError());
    }

    /**
     * Displays a modal dialog box to shows details about an {@link Exception}.
     *
     * @param header the header text for the dialog
     * @param e      the exception to display
     * @param owner  the {@link Node} used to recover the stage the dialog should be linked to
     */
    public static void displayException(String header, Throwable e, Node owner) {
        runOnFXThread(() -> {
            ExceptionDialog dlg = new ExceptionDialog(e);
            dlg.initStyle(StageStyle.UTILITY);
            dlg.initOwner(getStage(owner));
            dlg.getDialogPane().setHeaderText(header);
            dlg.showAndWait();
        });
    }

    /**
     * Display an error notification
     *
     * @param title    the title for the notification
     * @param message  the title for the notification
     * @param position the position for the notification
     * @param owner    the node to which the notification is attached
     */
    public static void notifyError(String title, String message, Pos position, Node owner) {
        logger.error(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(NOTIFICATION_DURATION))
                .position(position)
                .owner(owner).showError());
    }

    /**
     * Display an warning notification
     *
     * @param title   the title for the notification
     * @param message the title for the notification
     * @param owner   the node to which the notification is attached
     */
    public static void notifyWarning(String title, String message, Pos position, Node owner) {
        logger.warn(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(NOTIFICATION_DURATION))
                .position(position)
                .owner(owner).showWarning());
    }

    /**
     * Display an info notification
     *
     * @param title   the title for the notification
     * @param message the title for the notification
     * @param owner   the node to which the notification is attached
     */
    public static void notifyInfo(String title, String message, Pos position, Node owner) {
        logger.info(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(Duration.seconds(NOTIFICATION_DURATION))
                .position(position)
                .owner(owner).showInformation());
    }

    /**
     * Returns the {@link Stage} instance to which the provided {@link Node} is attached
     *
     * @param node the node to get the stage for.
     * @return the {@link Stage} instance to which the provided {@link Node} is attached
     */
    public static Stage getStage(Node node) {
        if (node != null && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }

    /**
     * Launch the system default browser to browse the provided URL
     *
     * @param url a string that represent the url to point the browser at
     * @throws IOException        if the default browser is not found or fails to be launched
     * @throws URISyntaxException if the string could not be transform into a proper URI
     */
    public static void launchUrlInExternalBrowser(String url) throws IOException, URISyntaxException {
        switch (AppEnvironment.getInstance().getOsFamily()) {
            case WINDOWS:
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(url));
                    }
                    else {
                        logger.warn("Action Desktop.Action.BROWSE is not supported on this platform");
                    }
                }
                else {
                    logger.warn("java.awt.Desktop is not supported on this platform");
                }
                break;
            case LINUX:
                if (Runtime.getRuntime().exec(new String[]{"which", "xdg-open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                }
                else {
                    logger.warn("Failed to find location for xdg-open");
                }
                break;
            case OSX:
                if (Runtime.getRuntime().exec(new String[]{"which", "open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"open", url});
                }
                else {
                    logger.warn("Failed to find location for open");
                }
                break;

            case UNSUPPORTED:
            default:
                logger.error("Cannot launch a url in a browser on this system");
        }
    }

    /**
     * Displays a dialog box that provides the end user with an opportunity to save the current state
     *
     * @param node     the node to get the stage for
     * @param fileName the name of the file to save
     * @return the {@link ButtonType} for the button that was chosen by the user
     */
    public static ButtonType confirmSaveDialog(Node node, String fileName) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(Dialogs.getStage(node));
        dlg.setTitle("Save");
        dlg.getDialogPane().setHeaderText("Do you want to save changes to " + fileName + "?");
        ImageView img = new ImageView(new Image(Dialogs.class.getResourceAsStream("/images/save_96.png")));
        img.setFitHeight(32);
        img.setFitWidth(32);
        dlg.getDialogPane().setGraphic(img);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        return dlg.showAndWait().orElse(ButtonType.CANCEL);
    }

    private static void runOnFXThread(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        }
        else {
            Platform.runLater(r);
        }
    }

    public static void setAlwaysOnTop(Dialog dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Dialog cannot be null");
        }
        Stage dlgStage = Dialogs.getStage(dialog.getDialogPane());
        if (dlgStage != null) {
            dlgStage.setAlwaysOnTop(true);
        }
        else {
            logger.debug("Failed to retreive dialog's stage: cannot set dialog to be always on top");
        }
    }
}
