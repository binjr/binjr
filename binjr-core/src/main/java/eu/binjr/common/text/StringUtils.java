/*
 *    Copyright 2019 Frederic Thevenet
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

package eu.binjr.common.text;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringUtils {
    private final static String NON_THIN = "[^iIl1\\.,']";
    private static final Logger logger = LogManager.getLogger(StringUtils.class);

    private static int textWidth(String str) {
        return str.length() - str.replaceAll(NON_THIN, "").length() / 2;
    }

    public static String ellipsize(String text, int max) {

        if (textWidth(text) <= max)
            return text;

        // Start by chopping off at the word before max
        // This is an over-approximation due to thin-characters...
        int end = text.lastIndexOf(' ', max - 3);

        // Just one long word. Chop it off.
        if (end == -1)
            return text.substring(0, max - 3) + "...";

        // Step forward as long as textWidth allows.
        int newEnd = end;
        do {
            end = newEnd;
            newEnd = text.indexOf(' ', end + 1);

            // No more spaces.
            if (newEnd == -1)
                newEnd = text.length();

        } while (textWidth(text.substring(0, newEnd) + "...") < max);

        return text.substring(0, end) + "...";
    }

    public static boolean contains(String str, String searchStr, boolean caseSensitive) {
        if (str == null || searchStr == null) return false;
        if (searchStr.isEmpty()) return true;
        if (caseSensitive) {
            return str.contains(searchStr);
        }
        final int length = searchStr.length();
        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }

    public static String integerToOrdinal(int i) {
        if (i < 0) {
            throw new UnsupportedOperationException("Only positive integer are supported.");
        }
        int mod100 = i % 100;
        int mod10 = i % 10;
        if (mod10 == 1 && mod100 != 11) {
            return i + "st";
        } else if (mod10 == 2 && mod100 != 12) {
            return i + "nd";
        } else if (mod10 == 3 && mod100 != 13) {
            return i + "rd";
        } else {
            return i + "th";
        }
    }

    public static String stringToEscapeSequence(String val) {
        if (val == null){
            return null;
        }
        switch (val.trim()) {
            case "\\t" -> {
                return "\t";
            }
            case "'" -> {
                return "'";
            }
            case "\\" -> {
                return "\"";
            }
            case "\\n" -> {
                return "\n";
            }
            case "\\r" -> {
                return "\r";
            }
            case "\\f" -> {
                return "\f";
            }
            case "\\b" -> {
                return "\b";
            }
            case "\\\\" -> {
                return "\\";
            }
            case "\\r\\n" -> {
                return "\r\n";
            }
            case "\\n\\r" -> {
                return "\n\r";
            }
            default -> {
                return val;
            }
        }
    }


}
