/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.common.javafx.richtext;

import eu.binjr.common.logging.Logger;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CodeAreaHighlighter {
    private static final Logger logger = Logger.create(CodeAreaHighlighter.class);
    private static final Pattern XML_TAG = Pattern.compile(
            "(?<ELEMENT>(</?\\??\\h*)([\\w:\\-_]+)([^<>]*)(\\h*/?\\??>))" +
                    "|(?<COMMENT><!--[^<>]+-->)");

    private static final Pattern LOGS_SEVERITY = Pattern.compile(
            "(?i)\\[TRACE|DEBUG|PERF|NOTE|INFO|WARN|ERROR|FATAL\\s?\\]");
    private static final Pattern ATTRIBUTES = Pattern.compile("([\\w:]+\\h*)(=)(\\h*\"[^\"]*\")");

    private static final int GROUP_OPEN_BRACKET = 2;
    private static final int GROUP_ELEMENT_NAME = 3;
    private static final int GROUP_ATTRIBUTES_SECTION = 4;
    private static final int GROUP_CLOSE_BRACKET = 5;
    private static final int GROUP_ATTRIBUTE_NAME = 1;
    private static final int GROUP_EQUAL_SYMBOL = 2;
    private static final int GROUP_ATTRIBUTE_VALUE = 3;
    public static final String SEARCH_RESULT_HIGHLIGHT = "search-result-highlight";

    public static class SearchHitRange {
        private final int start;
        private final int end;

        private SearchHitRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class SearchHilightResults {
        private final List<SearchHitRange> searchHitRanges;
        private final StyleSpans<Collection<String>> styleSpans;

        private SearchHilightResults(List<SearchHitRange> searchHitRanges, StyleSpans<Collection<String>> styleSpans) {
            this.searchHitRanges = searchHitRanges;
            this.styleSpans = styleSpans;
        }


        public List<SearchHitRange> getSearchHitRanges() {
            return searchHitRanges;
        }

        public StyleSpans<Collection<String>> getStyleSpans() {
            return styleSpans;
        }
    }

    public static SearchHilightResults computeSearchHitsHighlighting(String text, String searchText, boolean matchCase, boolean regEx) {
        StringBuilder searchExpression = new StringBuilder();
        if (!regEx) {
            searchText.codePoints().forEachOrdered(value -> {
                var c = Character.toString(value);
                String regExSpecialChars = "\\^$.|?*+()[]{}";
                if (regExSpecialChars.contains(c)) {
                    searchExpression.append("\\");
                }
                searchExpression.append(c);
            });
        } else {
            searchExpression.append(searchText);
        }
        if (!matchCase) {
            searchExpression.insert(0, "(?i)");
        }
        logger.debug(() -> "Search expression= " + searchExpression.toString());
        List<SearchHitRange> hits = new ArrayList<>();
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        try {
            var searchPattern = Pattern.compile(searchExpression.toString());
            if (searchText != null && !searchText.isEmpty()) {
                Matcher searchMatcher = searchPattern.matcher(text);
                while (searchMatcher.find()) {
                    spansBuilder.add(Collections.emptyList(), searchMatcher.start() - lastKwEnd);
                    spansBuilder.add(Collections.singleton(SEARCH_RESULT_HIGHLIGHT), searchMatcher.end() - searchMatcher.start());
                    hits.add(new SearchHitRange(searchMatcher.start(), searchMatcher.end()));
                    lastKwEnd = searchMatcher.end();
                }
            }
        } catch (PatternSyntaxException e) {
            logger.error("Incorrect search expression pattern: " + e.getMessage());
            logger.debug("", e);
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return new SearchHilightResults(hits, spansBuilder.create());
    }

    public static StyleSpans<Collection<String>> computeLogsSyntaxHighlighting(String text) {
        Matcher matcher = LOGS_SEVERITY.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }


    public static StyleSpans<Collection<String>> computeXmlSyntaxHighlighting(String text) {
        Matcher matcher = XML_TAG.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                if (matcher.group("ELEMENT") != null) {
                    String attributesText = matcher.group(GROUP_ATTRIBUTES_SECTION);
                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_OPEN_BRACKET) - matcher.start(GROUP_OPEN_BRACKET));
                    spansBuilder.add(Collections.singleton("anytag"), matcher.end(GROUP_ELEMENT_NAME) - matcher.end(GROUP_OPEN_BRACKET));
                    if (!attributesText.isEmpty()) {
                        lastKwEnd = 0;
                        Matcher amatcher = ATTRIBUTES.matcher(attributesText);
                        while (amatcher.find()) {
                            spansBuilder.add(Collections.emptyList(), amatcher.start() - lastKwEnd);
                            spansBuilder.add(Collections.singleton("attribute"), amatcher.end(GROUP_ATTRIBUTE_NAME) - amatcher.start(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("tagmark"), amatcher.end(GROUP_EQUAL_SYMBOL) - amatcher.end(GROUP_ATTRIBUTE_NAME));
                            spansBuilder.add(Collections.singleton("avalue"), amatcher.end(GROUP_ATTRIBUTE_VALUE) - amatcher.end(GROUP_EQUAL_SYMBOL));
                            lastKwEnd = amatcher.end();
                        }
                        if (attributesText.length() > lastKwEnd)
                            spansBuilder.add(Collections.emptyList(), attributesText.length() - lastKwEnd);
                    }
                    lastKwEnd = matcher.end(GROUP_ATTRIBUTES_SECTION);
                    spansBuilder.add(Collections.singleton("tagmark"), matcher.end(GROUP_CLOSE_BRACKET) - lastKwEnd);
                }
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
