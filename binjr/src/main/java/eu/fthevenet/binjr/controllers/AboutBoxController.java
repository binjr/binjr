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

package eu.fthevenet.binjr.controllers;


import eu.fthevenet.binjr.dialogs.Dialogs;
import eu.fthevenet.binjr.preferences.GlobalPreferences;
import eu.fthevenet.binjr.preferences.SysInfoProperty;
import javafx.application.Platform;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

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

        versionCheckFlow.getChildren().clear();
        Label l = new Label("Checking for updates...");
        l.setTextFill(Color.DARKGRAY);
        l.setPadding(new Insets(3, 0, 0, 4));
        versionCheckFlow.getChildren().add(l);
        GlobalPreferences.getInstance().asyncCheckForUpdate(githubRelease -> {
                    versionCheckFlow.getChildren().clear();
                    Hyperlink latestReleaseLink = new Hyperlink("v" + githubRelease.getVersion().toString() + " is available");
                    latestReleaseLink.setTextFill(Color.valueOf("#4BACC6"));
                    latestReleaseLink.setEffect(new Bloom());
                    latestReleaseLink.setOnAction(event -> {
                        try {
                            Dialogs.launchUrlInExternalBrowser(githubRelease.getHtmlUrl());
                        } catch (IOException | URISyntaxException e) {
                            logger.error(e);
                        }
                    });
                    versionCheckFlow.getChildren().add(latestReleaseLink);
                },
                version -> l.setText("binjr is up to date"),
                () -> versionCheckFlow.getChildren().clear());
        sysInfoListTable.getItems().addAll(GlobalPreferences.getInstance().getSysInfoProperties());
        versionLabel.setText("version " + GlobalPreferences.getInstance().getManifestVersion());
        detailsPane.getPanes().forEach(p -> p.expandedProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                p.requestLayout();
                p.getScene().getWindow().sizeToScene();
            });
        }));
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
