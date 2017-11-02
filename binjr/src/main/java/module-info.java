/**
 * Created by FTT2 on 20/06/2017.
 */
module eu.fthevenet.binjr {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;
    requires javafx.swing;
    requires log4j.api;
    requires controlsfx;
    requires jfxutils;
    requires java.xml.bind;
    requires jaxb.java.time.adapters;
    requires java.prefs;
    requires httpclient;
    requires gson;
    requires httpcore;
    requires jdk.security.auth;
    requires javafx.web;
    exports eu.fthevenet.binjr;
    exports eu.fthevenet.binjr.controllers;
    opens eu.fthevenet.binjr.controllers;
    exports eu.fthevenet.util.javafx.controls;
}