/*
 *    Copyright 2017-2021 Frederic Thevenet
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

package eu.binjr.core.dialogs;

import eu.binjr.common.logging.Logger;
import eu.binjr.common.preferences.MostRecentlyUsedList;
import eu.binjr.common.preferences.ObservablePreference;
import eu.binjr.core.preferences.AppEnvironment;
import eu.binjr.core.preferences.UserPreferences;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.ExceptionDialog;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Defines helper methods to facilitate the display of common dialog boxes
 *
 * @author Frederic Thevenet
 */
public class Dialogs {
    private static final Logger logger = Logger.create(Dialogs.class);
    private static final double TOOL_BUTTON_SIZE = 20;

    /**
     * Displays an error notification
     *
     * @param e the exception to display
     */
    public static void notifyException(Throwable e) {
        notifyException(e.getMessage(), e, null);
    }

    /**
     * Displays an error notification
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
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(Pos.BOTTOM_RIGHT)
                .action(new Action("Details", ae -> displayException(title, e)))
                .owner(owner).showError());
    }

    /**
     * Displays a modal dialog box to shows details about an {@link Exception}.
     *
     * @param header the header text for the dialog
     * @param e      the exception to display
     */
    private static void displayException(String header, Throwable e) {
        Dialogs.displayException(header, e, null);
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
            dlg.initOwner(org.controlsfx.tools.Utils.getWindow(owner));
            dlg.getDialogPane().setHeaderText(header);
            dlg.showAndWait();
        });
    }

    /**
     * Displays an error notification
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
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(position)
                .owner(owner).showError());
    }

    /**
     * Displays an error notification
     *
     * @param title    the title for the notification
     * @param t        the {@link Throwable} to display.
     * @param position the position for the notification
     * @param owner    the node to which the notification is attached
     */
    public static void notifyError(String title, Throwable t, Pos position, Node owner) {
        logger.debug(() -> title, t);
        notifyError(title, t.getMessage(), position, owner);
    }

    /**
     * Displays an warning notification
     *
     * @param title    the title for the notification
     * @param message  the title for the notification
     * @param position the position of the notification on screen.
     * @param owner    the node to which the notification is attached
     */
    public static void notifyWarning(String title, String message, Pos position, Node owner) {
        logger.warn(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(position)
                .owner(owner).showWarning());
    }

    /**
     * Display an info notification
     *
     * @param title    the title for the notification
     * @param message  the title for the notification
     * @param position the position of the notification on screen.
     * @param owner    the node to which the notification is attached
     */
    public static void notifyInfo(String title, String message, Pos position, Node owner) {
        logger.info(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(position)
                .owner(owner).showInformation());
    }

    /**
     * Display an info notification
     *
     * @param title   the title for the notification
     * @param message the title for the notification
     */
    public static void notifyInfo(String title, String message) {
        logger.info(title + " - " + message);
        runOnFXThread(() -> Notifications.create()
                .title(title)
                .text(message)
                .hideAfter(UserPreferences.getInstance().notificationPopupDuration.get().getDuration())
                .position(Pos.BOTTOM_RIGHT)
                .owner(null).showInformation());
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

    public static double getOutputScaleX(Node node) {
        var stage = Dialogs.getStage(node);
        return stage == null ? Screen.getPrimary().getOutputScaleX() : stage.getOutputScaleX();
    }

    public static double getOutputScaleY(Node node) {
        var stage = Dialogs.getStage(node);
        return stage == null ? Screen.getPrimary().getOutputScaleY() : stage.getOutputScaleY();
    }

    /**
     * Launches the system default browser to browse the provided URL
     *
     * @param url the url to point the browser at.
     * @throws IOException        if the default browser is not found or fails to be launched
     * @throws URISyntaxException if the string could not be transform into a proper URI
     */
    public static void launchUrlInExternalBrowser(String url) throws IOException, URISyntaxException {
        launchUrlInExternalBrowser(new URL(url));
    }

    /**
     * Launches the system default browser to browse the provided URL
     *
     * @param url a string that represent the url to point the browser at
     * @throws IOException        if the default browser is not found or fails to be launched
     * @throws URISyntaxException if the string could not be transform into a proper URI
     */
    public static void launchUrlInExternalBrowser(URL url) throws IOException, URISyntaxException {
        switch (AppEnvironment.getInstance().getOsFamily()) {
            case WINDOWS:
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(url.toURI());
                    } else {
                        logger.warn("Action Desktop.Action.BROWSE is not supported on this platform");
                    }
                } else {
                    logger.warn("java.awt.Desktop is not supported on this platform");
                }
                break;
            case LINUX:
                if (Runtime.getRuntime().exec(new String[]{"which", "xdg-open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url.toExternalForm()});
                } else {
                    logger.warn("Failed to find location for xdg-open");
                }
                break;
            case OSX:
                if (Runtime.getRuntime().exec(new String[]{"which", "open"}).getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{"open", url.toExternalForm()});
                } else {
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
        String msg = "Workspace \"" + fileName + "\" contains unsaved modifications.";
        Region icon = new Region();
        icon.getStyleClass().addAll("dialog-icon", "fileSave-icon");
        return confirmDialog(node, msg, "Save the changes?", icon, null, true);
    }

    /**
     * Displays a confirmation dialog box.
     *
     * @param node    a node attached to the stage to be used as the owner of the dialog.
     * @param header  the header message for the dialog.
     * @param content the main message for the dialog.
     * @return the {@link ButtonType} corresponding to user's choice.
     */
    public static ButtonType confirmDialog(Node node, String header, String content) {
        return confirmDialog(node, header, content, null, null, false);
    }

    public static ButtonType confirmDialog(Node node,
                                           String header,
                                           String content,
                                           ObservablePreference<Boolean> doNotAskAgain) {
        return confirmDialog(node, header, content, null, doNotAskAgain, false);
    }

    /**
     * Displays a confirmation dialog box.
     *
     * @param node    a node attached to the stage to be used as the owner of the dialog.
     * @param header  the header message for the dialog.
     * @param content the main message for the dialog.
     * @param icon    the icon for the dialog.
     * @return the {@link ButtonType} corresponding to user's choice.
     */
    public static ButtonType confirmDialog(Node node,
                                           String header,
                                           String content,
                                           Node icon,
                                           ObservablePreference<Boolean> doNotAskAgain,
                                           boolean isCancelable) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(Dialogs.getStage(node));
        setAlwaysOnTop(dlg);
        dlg.setTitle(AppEnvironment.APP_NAME);
        // Workaround JDK-8179073 (ref: https://bugs.openjdk.java.net/browse/JDK-8179073)
        dlg.setResizable(AppEnvironment.getInstance().isResizableDialogs());
        dlg.getDialogPane().setHeaderText(header);
        dlg.getDialogPane().setContentText(content);
        if (icon == null) {
            icon = new Region();
            icon.getStyleClass().addAll("dialog-icon", "help-icon");
        }
        dlg.getDialogPane().setGraphic(icon);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        if (isCancelable) {
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        }
        if (doNotAskAgain != null) {
            if (doNotAskAgain.get()) {
                return ButtonType.YES;
            }
            var checkBox = new CheckBox("Do not ask again");
            checkBox.selectedProperty().bindBidirectional(doNotAskAgain.property());
            dlg.getDialogPane().setExpandableContent(checkBox);
            dlg.getDialogPane().setExpanded(true);
        }

        return dlg.showAndWait().orElse(ButtonType.CANCEL);
    }

    /**
     * Ensures that the provided {@link Runnable} is executed on the JavaFX application thread.
     *
     * @param r the {@link Runnable} to execute on the JavaFX application thread
     */
    public static void runOnFXThread(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    /**
     * Forces the provided {@link Dialog} to always appear on top of other windows.
     *
     * @param dialog the dialog to set always on top.
     */
    public static void setAlwaysOnTop(Dialog dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Dialog cannot be null");
        }
        Stage dlgStage = Dialogs.getStage(dialog.getDialogPane());
        if (dlgStage != null) {
            dlgStage.setAlwaysOnTop(true);
        } else {
            logger.debug("Failed to retrieve dialog's stage: cannot set dialog to be always on top");
        }
    }

    public static Optional<File> getInitialDir(MostRecentlyUsedList<Path> mru) {
        try {
            var initDir = mru.peek().orElse(Paths.get(System.getProperty("user.home")));
            if (!Files.isDirectory(initDir) && initDir.getParent() != null) {
                initDir = initDir.getParent();
            }
            if (initDir.toRealPath().toFile().exists()) {
                return Optional.of(initDir.toFile());
            }
        } catch (Exception e) {
            logger.debug("Failed to retrieve initial dir for file chooser", e);
        }
        return Optional.empty();
    }

}
