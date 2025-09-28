/*
 *    Copyright 2020-2025 Frederic Thevenet
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
package eu.binjr.core.controllers;

import com.google.gson.Gson;
import eu.binjr.common.javafx.controls.NodeUtils;
import eu.binjr.common.javafx.controls.TableViewUtils;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.indexes.parser.capture.CaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.NamedCaptureGroup;
import eu.binjr.core.data.indexes.parser.capture.TemporalCaptureGroup;
import eu.binjr.core.data.indexes.parser.profile.CustomParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingProfile;
import eu.binjr.core.data.indexes.parser.profile.ParsingFailureMode;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.core.preferences.UserHistory;
import eu.binjr.core.preferences.UserPreferences;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.binjr.core.data.indexes.parser.capture.CaptureGroup.SEVERITY;

/**
 * The controller class for ParsingProfileView
 */
public abstract class ParsingProfilesController<T extends ParsingProfile> implements Initializable {
    private static final Logger logger = Logger.create(ParsingProfilesController.class);
    protected static final Pattern GROUP_TAG_PATTERN = Pattern.compile("\\$[a-zA-Z0-9]{2,}");
    protected static final Gson gson = new Gson();
    protected final T[] builtinParsingProfiles;
    protected final T defaultProfile;
    @FXML
    protected TableColumn<NameExpressionPair, String> expressionColumn;
    @FXML
    protected TableColumn<NameExpressionPair, NamedCaptureGroup> nameColumn;
    @FXML
    protected Label notificationLabel;
    @FXML
    protected TableView<NameExpressionPair> captureGroupTable;
    @FXML
    protected Button deleteGroupButton;
    @FXML
    protected Button addGroupButton;
    @FXML
    protected AnchorPane root;
    @FXML
    protected VBox expressions;
    @FXML
    protected CodeArea lineTemplateExpression;
    @FXML
    protected Button runTestButton;
    @FXML
    protected CodeArea testArea;
    @FXML
    protected ComboBox<T> profileComboBox;
    @FXML
    protected Button addProfileButton;
    @FXML
    protected Button deleteProfileButton;
    @FXML
    protected Button cloneProfileButton;
    @FXML
    protected Button importProfileButton;
    @FXML
    protected Button exportProfileButton;
    @FXML
    protected Region builtinIcon;
    @FXML
    protected VBox setupPane;
    @FXML
    protected VBox testPane;
    @FXML
    protected ChoiceBox<ParsingFailureMode> onParseFailureChoiceBox;

    protected final AtomicInteger groupSequence = new AtomicInteger(0);
    protected final T selectedProfile;
    private final Set<T> userParsingProfiles;
    private final boolean allowTemporalCaptureGroupsOnly;
    private final Charset defaultCharset;
    private final ZoneId defaultZoneId;
    private boolean inhibitProfileUpdate = false;


    @FXML
    protected void handleOnCloneProfile(ActionEvent actionEvent) {
        try {
            if (this.profileComboBox.getValue() != null) {
                duplicateProfile(this.profileComboBox.getValue()).ifPresent(p -> {
                    this.profileComboBox.getItems().add(p);
                    this.profileComboBox.getSelectionModel().select(p);
                });
            }
        } catch (Throwable e) {
            Dialogs.notifyException("Error cloning profile", e, root);
        }
    }

    @FXML
    protected void handleOnAddProfile(ActionEvent actionEvent) {
        try {
            newProfile().ifPresent(p -> {
                this.profileComboBox.getItems().add(p);
                this.profileComboBox.getSelectionModel().select(p);
            });
        } catch (Throwable e) {
            Dialogs.notifyException("Error creating profile", e, root);
        }
    }

    @FXML
    void handleOnDeleteProfile(ActionEvent event) {
        if (!this.profileComboBox.getValue().isBuiltIn()) {
            this.profileComboBox.getItems().remove(this.profileComboBox.getValue());
        }
    }

    @FXML
    void handleOnExportProfile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Profiles");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Parsing Profiles", "*.json"));
        fileChooser.setInitialFileName("parsing_profiles.json");
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        var exportPath = fileChooser.showSaveDialog(NodeUtils.getStage(root));
        if (exportPath != null) {
            try {
                // Apply changes before exporting to file
                this.applyChanges();
                Files.deleteIfExists(exportPath.toPath());
                UserHistory.getInstance().mostRecentSaveFolders.push(exportPath.toPath().getParent());
                Files.writeString(exportPath.toPath(), gson.toJson(profileComboBox.getItems().stream()
                        .filter(p -> !p.isBuiltIn())
                        .toList()));
                logger.info("Parsing profiles successfully exported to " + exportPath);
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while exporting profiles: " + e.getMessage(), e, root);
            }
        }

    }

    @FXML
    void handleOnImportProfile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Profiles");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Parsing Profiles", "*.json"));
        Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
        File importPath = fileChooser.showOpenDialog(NodeUtils.getStage(root));
        if (importPath != null) {
            try {
                var profiles = deSerializeProfiles(Files.readString(importPath.toPath()));
                profileComboBox.getItems().addAll(profiles);
                logger.info("Parsing profiles successfully imported to " + importPath);
            } catch (Exception e) {
                Dialogs.notifyException("An error occurred while importing profiles: " + e.getMessage(), e, root);
            }
        }
    }

    protected abstract List<T> deSerializeProfiles(String profileString);

    protected abstract void doTest() throws Exception;

    @FXML
    protected void handleOnRunTest(ActionEvent event) {
        try {
            resetTest();
            if (applyChanges()) {
                doTest();
            }
        } catch (Exception e) {
            notifyError("Parsing failed: " + e.getMessage());
            logger.error("Error testing parsing rules: " + e.getMessage());
            logger.debug(() -> "Stack Trace", e);
        }
    }

    @FXML
    protected void handleOnAddGroup(ActionEvent actionEvent) {
        this.captureGroupTable.getItems().add(new NameExpressionPair(CaptureGroup.of("GROUP" + groupSequence.incrementAndGet()), ".*"));
    }

    @FXML
    protected void handleOnDeleteGroup(ActionEvent actionEvent) {
        var idx = this.captureGroupTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            this.captureGroupTable.getItems().remove(idx);
        }
    }

    @FXML
    protected void handleOnClearTestArea(ActionEvent actionEvent) {
        testArea.clear();
    }

    @FXML
    protected void handleOnCopyTestArea(ActionEvent actionEvent) {
        testArea.selectAll();
        testArea.copy();
    }

    @FXML
    protected void handleOnPasteToTestArea(ActionEvent actionEvent) {
        testArea.selectAll();
        testArea.paste();
    }

    @FXML
    protected void handleOnClearField(ActionEvent actionEvent) {
        var node = (Node) actionEvent.getSource();
        var parent = node.getParent();
        if (parent != null) {
            var field = parent.lookup(".search-field-inner");
            if (field instanceof TextField textField) {
                textField.setText("");
            }
        }
    }

    @FXML
    protected void handleOnOpenFileToTestArea(ActionEvent event) {
        File selectedFile = displayFileChooser((Node) event.getSource());
        if (selectedFile != null) {
            testArea.clear();
            try (var reader = Files.newBufferedReader(selectedFile.toPath())) {
                int lineRead = 0;
                while (lineRead < UserPreferences.getInstance().maxLinesFileTestPreview.get().intValue()) {
                    var line = reader.lines().findFirst();
                    if (line.isPresent()) {
                        testArea.appendText(line.get());
                        testArea.appendText("\n");
                        lineRead++;
                    } else {
                        lineRead = Integer.MAX_VALUE;
                    }
                }
            } catch (Exception e) {
                Dialogs.notifyException("Could not load data into test area", e, root);
            }
        }
    }

    private File displayFileChooser(Node owner) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open file");
            additionalExtensions().ifPresent(ext -> fileChooser.getExtensionFilters().addAll(ext));
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*", "*"));
            Dialogs.getInitialDir(UserHistory.getInstance().mostRecentSaveFolders).ifPresent(fileChooser::setInitialDirectory);
            return fileChooser.showOpenDialog(NodeUtils.getStage(owner));
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    protected Optional<List<FileChooser.ExtensionFilter>> additionalExtensions() {
        return Optional.empty();
    }

    @FXML
    protected void handleOnOk(ActionEvent actionEvent) {
        NodeUtils.getStage(root).close();
    }

    @FXML
    protected void handleOnCancel(ActionEvent actionEvent) {
        NodeUtils.getStage(root).close();
    }


    /**
     * Initalizes a new instance of the {@link ParsingProfilesController} class.
     */
    public ParsingProfilesController(T[] builtinParsingProfiles,
                                     T[] userParsingProfiles,
                                     T defaultProfile,
                                     T selectedProfile,
                                     Charset defaultCharset,
                                     ZoneId defaultZoneId) {
        this(builtinParsingProfiles, userParsingProfiles, defaultProfile, selectedProfile, false, defaultCharset, defaultZoneId);
    }

    /**
     * Initalizes a new instance of the {@link ParsingProfilesController} class.
     */
    public ParsingProfilesController(T[] builtinParsingProfiles,
                                     T[] userParsingProfiles,
                                     T defaultProfile,
                                     T selectedProfile,
                                     boolean allowTemporalCaptureGroupsOnly,
                                     Charset defaultCharset,
                                     ZoneId defaultZoneId) {
        this.builtinParsingProfiles = builtinParsingProfiles;
        this.defaultProfile = defaultProfile;
        this.selectedProfile = selectedProfile;
        this.userParsingProfiles = new HashSet<>(Arrays.stream(userParsingProfiles).toList());
        this.allowTemporalCaptureGroupsOnly = allowTemporalCaptureGroupsOnly;
        this.defaultCharset = defaultCharset;
        this.defaultZoneId = defaultZoneId;
        if (selectedProfile instanceof CustomParsingProfile) {
            this.userParsingProfiles.add(selectedProfile);
        }
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        assert root != null : "fx:id=\"root\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert expressions != null : "fx:id=\"expressions\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert lineTemplateExpression != null : "fx:id=\"lineTemplateExpression\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert runTestButton != null : "fx:id=\"testLineTemplate\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert testArea != null : "fx:id=\"testArea\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert profileComboBox != null : "fx:id=\"profileComboBox\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert addProfileButton != null : "fx:id=\"importProfileButton2\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert deleteProfileButton != null : "fx:id=\"deleteProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert cloneProfileButton != null : "fx:id=\"importProfileButton1\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert importProfileButton != null : "fx:id=\"importProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";
        assert exportProfileButton != null : "fx:id=\"exportProfileButton\" was not injected: check your FXML file 'ParsingRulesView.fxml'.";

        var inlineHelpIconDisplayBinding = Bindings.createObjectBinding(
                () -> UserPreferences.getInstance().showInlineHelpButtons.get() ? ContentDisplay.RIGHT : ContentDisplay.TEXT_ONLY,
                UserPreferences.getInstance().showInlineHelpButtons.property());
        this.profileComboBox.getItems().setAll(builtinParsingProfiles);
        if (userParsingProfiles != null) {
            this.profileComboBox.getItems().addAll(userParsingProfiles);
        }

        this.profileComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (!inhibitProfileUpdate && newValue != null) {
                loadParserParameters(newValue);
            }
        });
        this.profileComboBox.setConverter(new StringConverter<T>() {
            @Override
            public String toString(T object) {
                return object.toString();
            }

            @Override
            public T fromString(String string) {
                var val = profileComboBox.getValue();
                if (!inhibitProfileUpdate && !val.isBuiltIn() && !val.getProfileName().equals(string)) {
                    applyChanges();
                }
                return val;
            }
        });
        this.onParseFailureChoiceBox.getItems().setAll(getSupportedUnparseableBehaviors());
        this.onParseFailureChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ParsingFailureMode o) {
                return o != null ? o.toString() : "";
            }

            @Override
            public ParsingFailureMode fromString(String s) {
                return onParseFailureChoiceBox.getValue();
            }
        });


        lineTemplateExpression.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Node source = (Node) event.getSource();
            if (event.getCode() == KeyCode.ENTER) {
                source.fireEvent(new ActionEvent(this, null));
                event.consume();
            } else if (event.getCode() == KeyCode.TAB) {
                if (event.isShiftDown()) {
                    navigateToPreviousNode(source);
                } else {
                    navigateToNextNode(source);
                }
                event.consume();
            }
        });
        testArea.textProperty().addListener((obs, oldText, newText) -> resetTest());
        lineTemplateExpression.textProperty().addListener((obs, oldText, newText) -> {
            resetTest();
            colorLineTemplateField(newText);
        });
        profileComboBox.getSelectionModel().select(profileComboBox.getItems().stream()
                .filter(p -> Objects.equals(p.getProfileId(), selectedProfile.getProfileId()))
                .findAny().orElse(this.defaultProfile));
        NamedCaptureGroup[] knownGroups;
        if (allowTemporalCaptureGroupsOnly) {
            knownGroups = TemporalCaptureGroup.values();
        } else {
            knownGroups = new NamedCaptureGroup[TemporalCaptureGroup.values().length + 1];
            System.arraycopy(TemporalCaptureGroup.values(), 0, knownGroups, 0, TemporalCaptureGroup.values().length);
            knownGroups[TemporalCaptureGroup.values().length] = CaptureGroup.of(SEVERITY);
        }
        this.nameColumn.setCellFactory(list -> new ColoredTableCell(new StringConverter<>() {
            @Override
            public String toString(NamedCaptureGroup object) {
                return object.toString();
            }

            @Override
            public NamedCaptureGroup fromString(String string) {
                return CaptureGroup.of(string);
            }
        }, knownGroups));
        this.nameColumn.setOnEditCommit(
                t -> {
                    NamedCaptureGroup newVal = t.getNewValue();
                    if (!(newVal instanceof TemporalCaptureGroup)) {
                        for (var group : TemporalCaptureGroup.values()) {
                            if (group.name().equals(newVal.name())) {
                                newVal = group;
                                break;
                            }
                        }
                    }
                    t.getTableView().getItems().get(t.getTablePosition().getRow()).setName(newVal);
                    applyChanges();
                    colorLineTemplateField();
                }
        );
        this.expressionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        this.expressionColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setExpression(t.getNewValue())
        );

        TableViewUtils.autoFillTableWidthWithColumn(captureGroupTable, 1);
    }

    protected abstract ParsingFailureMode[] getSupportedUnparseableBehaviors();

    private void navigateToNextNode(Node source) {
        Node parent = source.getParent();
        if (parent != null) {
            var siblings = source.getParent().getChildrenUnmodifiable();
            var pos = siblings.indexOf(source);
            if (pos == siblings.size() - 1) {
                // last node of the branch, go back to parent
                navigateToNextNode(parent);
            } else {
                // Focus on next node in branch
                siblings.get(pos + 1).requestFocus();
            }
        }
    }

    private void navigateToPreviousNode(Node source) {
        Node parent = source.getParent();
        if (parent != null) {
            var siblings = source.getParent().getChildrenUnmodifiable();
            var pos = siblings.indexOf(source);
            if (pos == 0) {
                // first node of the branch, go back to parent
                navigateToPreviousNode(parent);
            } else {
                // Focus on previous node in branch
                siblings.get(pos - 1).requestFocus();
            }
        }
    }

    public T getSelectedProfile() {
        return profileComboBox.getValue();
    }

    public List<T> getCustomProfiles() {
        return profileComboBox.getItems().stream()
                .filter(p -> !p.isBuiltIn())
                .toList();
    }

    protected void colorLineTemplateField() {
        colorLineTemplateField(lineTemplateExpression.getText());
    }

    protected void colorLineTemplateField(String text) {
        lineTemplateExpression.setStyleSpans(0, computeParsingProfileSyntaxHighlighting(text));
    }

    protected void resetTest() {
        clearNotification();
        var spans = new StyleSpansBuilder<Collection<String>>();
        spans.add(Collections.emptyList(), testArea.getText().length());
        testArea.setStyleSpans(0, spans.create());
    }

    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    public ZoneId getDefaultZoneId() {
        return defaultZoneId;
    }

    protected class ColoredTableCell extends ComboBoxTableCell<NameExpressionPair, NamedCaptureGroup> {
        public ColoredTableCell(StringConverter<NamedCaptureGroup> converter, NamedCaptureGroup... items) {
            super(converter, items);
            setComboBoxEditable(true);
        }

        @Override
        public void updateItem(NamedCaptureGroup item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                var idx = getCaptureGroupPaletteIndex(item);
                this.setStyle("-fx-font-weight: bold; -fx-text-fill:-palette-color-" + idx + ";");
                setText(item.toString());
            }
        }
    }

    protected void loadParserParameters(T profile) {
        try {
            resetTest();
            this.builtinIcon.setVisible(profile.isBuiltIn());
            this.builtinIcon.setManaged(profile.isBuiltIn());
            this.captureGroupTable.getItems().clear();
            profile.getCaptureGroups().forEach((k, v) -> {
                this.captureGroupTable.getItems().add(new NameExpressionPair(k, v));
            });
            this.lineTemplateExpression.clear();
            this.lineTemplateExpression.appendText(profile.getLineTemplateExpression());
            this.setupPane.setDisable(profile.isBuiltIn());
            this.deleteProfileButton.setDisable(profile.isBuiltIn());
            this.captureGroupTable.setEditable(!profile.isBuiltIn());
            this.profileComboBox.getEditor().setEditable(!profile.isBuiltIn());
            this.onParseFailureChoiceBox.getSelectionModel().select(profile.onParsingFailure());
        } catch (
                Exception e) {
            Dialogs.notifyException("Error loading profile", e, root);
        }
    }

    protected abstract Optional<T> updateProfile(String profileName,
                                                 String profileId,
                                                 Map<NamedCaptureGroup, String> groups,
                                                 String lineExpression,
                                                 ParsingFailureMode onParsingFailure);

    protected Optional<T> duplicateProfile(T profile) {
        return updateProfile("Copy of " + profile.getProfileName(),
                UUID.randomUUID().toString(),
                profile.getCaptureGroups(),
                profile.getLineTemplateExpression(),
                profile.onParsingFailure());
    }

    protected Optional<T> newProfile() {
        return updateProfile("New profile",
                UUID.randomUUID().toString(),
                new HashMap<>(),
                "",
                ParsingFailureMode.IGNORE);
    }

    public boolean applyChanges() {
        clearNotification();
        var profile = profileComboBox.getValue();
        if (!profile.isBuiltIn()) {
            var name = profileComboBox.getEditor().getText();
            var groups = this.captureGroupTable.getItems().stream()
                    .collect(Collectors.toMap(NameExpressionPair::getName, NameExpressionPair::getExpression));
            var updated = updateProfile(name, profile.getProfileId(), groups, this.lineTemplateExpression.getText(), onParseFailureChoiceBox.getValue());
            if (updated.isPresent()) {
                // Suspend profile update in listeners
                this.inhibitProfileUpdate = true;
                try {
                    var idx = profileComboBox.getItems().indexOf(profile);
                    profileComboBox.getItems().remove(profile);
                    profileComboBox.getItems().add(idx, updated.get());
                    profileComboBox.getSelectionModel().select(updated.get());
                } catch (Exception e) {
                    logger.error("Error will apply change to profile properties: {}", e.getMessage());
                    logger.debug("Stack trace", e);
                } finally {
                    // Resume profile update in listeners
                    this.inhibitProfileUpdate = false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    protected final AtomicInteger paletteEntriesSequence = new AtomicInteger(0);
    protected final Map<NamedCaptureGroup, Integer> paletteLookupTable = new HashMap<>();

    protected int getCaptureGroupPaletteIndex(NamedCaptureGroup cg) {
        return paletteLookupTable.computeIfAbsent(cg, key -> paletteEntriesSequence.getAndIncrement() % 12);
    }

    protected StyleSpans<Collection<String>> computeParsingProfileSyntaxHighlighting(String text) {
        Matcher matcher = GROUP_TAG_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            var tag = matcher.group();
            var cg = captureGroupTable.getItems().stream().filter(n -> tag.equals("$" + n.getName())).findAny();
            if (cg.isPresent()) {
                var idx = getCaptureGroupPaletteIndex(cg.get().getName());
                spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(List.of("capture-group", "capture-group-" + idx), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    protected StyleSpans<Collection<String>> highlightTextArea(Pattern parsingRegex, String text) {
        var matcher = parsingRegex.matcher(text);
        int lastMatchEnd = 0;
        StyleSpansBuilder<Collection<String>> groupSpansBuilder = new StyleSpansBuilder<>();
        StyleSpansBuilder<Collection<String>> logsSpansBuilder = new StyleSpansBuilder<>();
        logsSpansBuilder.add(Collections.emptyList(), 0);
        while (matcher.find()) {
            logsSpansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            logsSpansBuilder.add(List.of("complete-log"), matcher.end() - matcher.start());
            groupSpansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
            SortedMap<Integer, NamedCaptureGroup> ordered = new TreeMap<>();
            for (Map.Entry<NamedCaptureGroup, String> entry : profileComboBox.getValue().getCaptureGroups().entrySet()) {
                var captureGroup = entry.getKey();
                var idx = getCaptureGroupPaletteIndex(captureGroup);
                var parsed = matcher.group(captureGroup.name());
                if (parsed != null && !parsed.isBlank()) {
                    ordered.put(matcher.start(captureGroup.name()), captureGroup);
                }
            }
            var cursor = matcher.start();
            for (var entry : ordered.entrySet()) {
                var groupPos = entry.getKey();
                var groupName = entry.getValue();
                var spanLen = groupPos - cursor;
                if (spanLen > 0) {
                    groupSpansBuilder.add(Collections.emptyList(), spanLen);
                    cursor += spanLen;
                }
                var paletteIndex = getCaptureGroupPaletteIndex(groupName);
                var len = matcher.end(groupName.name()) - matcher.start(groupName.name());
                groupSpansBuilder.add(List.of("capture-group", "capture-group-" + paletteIndex), len);
                cursor += len;
            }
            groupSpansBuilder.add(Collections.emptyList(), matcher.end() - cursor);
            lastMatchEnd = matcher.end();
        }
        groupSpansBuilder.add(Collections.emptyList(), text.length() - lastMatchEnd);
        return groupSpansBuilder.create().overlay(logsSpansBuilder.create(), (strings, strings2) -> Stream.concat(strings.stream(),
                strings2.stream()).collect(Collectors.toCollection(ArrayList<String>::new)));
    }

    protected void clearNotification() {
        notificationLabel.setVisible(false);
        notificationLabel.setManaged(false);
        notificationLabel.setText("");
    }

    protected void notifyInfo(String message) {
        notify(message, "notification-info");
    }

    protected void notifyWarn(String message) {
        notify(message, "notification-warn");
    }

    protected void notifyError(String message) {
        notify(message, "notification-error");
    }

    protected void notify(String message, String styleClass) {
        notificationLabel.getStyleClass().setAll(styleClass);
        notificationLabel.setText(message);
        notificationLabel.setManaged(true);
        notificationLabel.setVisible(true);
    }

    /**
     * Defines a name/expression pair.
     */
    public static class NameExpressionPair {
        private NamedCaptureGroup name;
        private String expression;

        private NameExpressionPair(NamedCaptureGroup name, String expression) {
            this.name = name;
            this.expression = expression;
        }

        /**
         * Returns he expression
         *
         * @return The expression
         */
        public String getExpression() {
            return expression;
        }

        /**
         * Returns the name
         *
         * @return The name
         */
        public NamedCaptureGroup getName() {
            return name;
        }

        /**
         * Sets the name
         *
         * @param name
         */
        public void setName(NamedCaptureGroup name) {
            this.name = name;
        }

        /**
         * Sets the expression
         *
         * @param expression
         */
        public void setExpression(String expression) {
            this.expression = expression;
        }


    }


}

