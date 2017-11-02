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

/**
 * Module info for binjr
 *
 * @author Frederic Thevenet
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