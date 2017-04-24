package eu.fthevenet.binjr.controllers;


import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.preferences.SysInfoProperty;
import eu.fthevenet.util.github.GithubRelease;
import eu.fthevenet.util.version.Version;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.MaskerPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ForkJoinPool;

/**
 * The controller for the about dialog
 *
 * @author Frederic Thevenet
 */
public class AboutBoxController implements Initializable {
    private static final Logger logger = LogManager.getLogger(AboutBoxController.class);
    @FXML
    private DialogPane aboutRoot;

    @FXML
    private Label versionLabel;

    @FXML
    private TextFlow versionCheckFlow;

//    @FXML
//    private Hyperlink newReleaseURL;

    public Hyperlink getBinjrUrl() {
        return binjrUrl;
    }

    @FXML
    private Hyperlink binjrUrl;

    @FXML
    private Accordion detailsPane;
    @FXML
    private ListView<String> sysInfoListView;

    @FXML
    private TableView<SysInfoProperty> sysInfoListTable;

    @FXML
    private TitledPane sysInfoPane;

    @FXML
    private TitledPane licensePane;

    @FXML
    private TextFlow acknowledgementTextFlow;

    @FXML
    private TextFlow licenseTextFlow;

    @FXML
    private TitledPane acknowledgementPane;

    @FXML
    private MaskerPane maskerPane;

    @FXML
    private void handleCloseButtonAction(ActionEvent event) {
        ((Stage) aboutRoot.getScene().getWindow()).close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert aboutRoot != null : "fx:id\"aboutRoot\" was not injected!";
        assert versionLabel != null : "fx:id\"versionLabel\" was not injected!";
        assert sysInfoListView != null : "fx:id\"sysInfoListView\" was not injected!";
        assert binjrUrl != null : "fx:id\"binjrUrl\" was not injected!";
        assert detailsPane != null : "fx:id\"detailsPane\" was not injected!";
        assert sysInfoPane != null : "fx:id\"sysInfoPane\" was not injected!";
        assert sysInfoListTable != null : "fx:id\"sysInfoListTable\" was not injected!";
        assert licensePane != null : "fx:id\"licensePane\" was not injected!";
        assert acknowledgementPane != null : "fx:id\"thirdPartiesPane\" was not injected!";
        assert licenseTextFlow != null : "fx:id\"licenseTextFlow\" was not injected!";
        assert acknowledgementTextFlow != null : "fx:id\"acknowledgementTextFlow\" was not injected!";
        assert versionCheckFlow != null : "fx:id\"versionCheckFlow\" was not injected!";

        try {
            BufferedReader sr = new BufferedReader(new InputStreamReader(getClass().getResource("/text/about_license.txt").openStream(), "utf-8"));
            Text binjrTxt = new Text("binjr\n");//
            binjrTxt.setFont(Font.font("Bauhaus 93", FontWeight.BOLD, 18));
            Text noticeTxt = new Text(sr.lines().reduce("", (s, s2) -> s.concat(s2 + "\n")));

            licenseTextFlow.getChildren().add(binjrTxt);
            licenseTextFlow.getChildren().add(noticeTxt);
        } catch (IOException e) {
            logger.error("Failed to get resource \"/text/about_license.txt\"", e);
        }
        try {
            BufferedReader sr = new BufferedReader(new InputStreamReader(getClass().getResource("/text/about_Acknowledgement.txt").openStream(), "utf-8"));
            acknowledgementTextFlow.getChildren().add(new Text(sr.lines().reduce("", (s, s2) -> s.concat(s2 + "\n"))));

        } catch (IOException e) {
            logger.error("Failed to get resource \"/text/about_Acknowledgement.txt\"", e);
        }
        Platform.runLater(() -> {
            Pane header = (Pane) sysInfoListTable.lookup("TableHeaderRow");
            if (header != null) {
                header.setMaxHeight(0);
                header.setMinHeight(0);
                header.setPrefHeight(0);
                header.setVisible(false);
            }
        });
        checkNewRelease();
        sysInfoListTable.getItems().addAll(GlobalPreferences.getInstance().getSysInfoProperties());
        versionLabel.setText("version " + GlobalPreferences.getInstance().getManifestVersion());
        detailsPane.getPanes().forEach(p -> p.expandedProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                p.requestLayout();
                p.getScene().getWindow().sizeToScene();
            });
        }));
    }

    private void checkNewRelease() {
        //  maskerPane.setVisible(true);
        versionCheckFlow.getChildren().clear();
        Label l =new Label("Checking for updates...");
        l.setTextFill(Color.LIGHTGRAY);
        l.setPadding(new Insets(3,0,0,4));
        versionCheckFlow.getChildren().add(l);
        Task<Optional<GithubRelease>> getLatestTask = new Task<Optional<GithubRelease>>() {
            @Override
            protected Optional<GithubRelease> call() throws Exception {
                logger.trace("checkForNewerRelease running on " + Thread.currentThread().getName());
                return GlobalPreferences.getInstance().checkForNewerRelease();
            }
        };
        getLatestTask.setOnSucceeded(workerStateEvent -> {
            logger.trace("UI update running on " + Thread.currentThread().getName());
            Optional<GithubRelease> latest = getLatestTask.getValue();
            //    maskerPane.setVisible(false);

            Version current = GlobalPreferences.getInstance().getManifestVersion();
            if (latest.isPresent()) {
//                versionCheckFlow.getChildren().add(new Text("You're currently running version " + current.toString() + ".\n"));
//                versionCheckFlow.getChildren().add(new Text("Version  " + latest.get().getVersion().toString() + " is available at:\n"));
                versionCheckFlow.getChildren().clear();
                Hyperlink latestReleaseLink = new Hyperlink("v" + latest.get().getVersion().toString() + " is available");
                latestReleaseLink.setTextFill(Color.valueOf("#ff6500"));
                latestReleaseLink.setEffect(new Bloom());
                latestReleaseLink.setOnAction(event -> {
                    try {
                        Dialogs.launchUrlInExternalBrowser(latest.get().getHtmlUrl());
                    } catch (IOException | URISyntaxException e) {
                        logger.error(e);
                    }
                });
                versionCheckFlow.getChildren().add(latestReleaseLink);
            }
            else {
                l.setText("binjr is up to date");
              //  versionCheckFlow.getChildren().add(l);
            }
        });
        ForkJoinPool.commonPool().submit(getLatestTask);
    }

    public void goTobinjrDotEu(ActionEvent actionEvent) {
        try {
            Dialogs.launchUrlInExternalBrowser(GlobalPreferences.HTTP_WWW_BINJR_EU);
        } catch (IOException | URISyntaxException e) {
            logger.error(e);
        }
        binjrUrl.setVisited(false);
    }

}
