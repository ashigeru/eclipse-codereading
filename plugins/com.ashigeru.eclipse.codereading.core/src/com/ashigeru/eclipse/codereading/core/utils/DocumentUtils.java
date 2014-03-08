/**
 * Copyright 2014 ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashigeru.eclipse.codereading.core.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Utilities about {@link IDocument}.
 */
public final class DocumentUtils {

    /**
     * Returns the line number of the region head.
     * @param document the target document
     * @param region the selection region
     * @return the line number (0-origin)
     * @throws BadLocationException if the region is not valid
     */
    public static int getLineNumber(IDocument document, IRegion region) throws BadLocationException {
        return document.getLineOfOffset(region.getOffset());
    }

    /**
     * Converts a text selection region into the its line selection region.
     * @param document the target document
     * @param region the selection region
     * @return the adjusted region
     * @throws BadLocationException if the region is not valid
     */
    public static IRegion toLines(IDocument document, IRegion region) throws BadLocationException {
        int startChar = region.getOffset();
        int endChar = startChar + region.getLength();
        IRegion startLine = document.getLineInformationOfOffset(startChar);
        IRegion endLine = document.getLineInformationOfOffset(endChar);
        if (startLine.getOffset() == endLine.getOffset()) {
            return startLine;
        }

        // end of region is on line head -> rewind a line
        if (endLine.getOffset() == endChar) {
            endLine = document.getLineInformationOfOffset(endChar - 1);
        }

        int newStartChar = startLine.getOffset();
        int newEndChar = endLine.getOffset() + endLine.getLength();
        return new Region(newStartChar, newEndChar - newStartChar);
    }

    /**
     * Extracts line contents from the document.
     * @param document the target document
     * @param region the target region
     * @return the line contents
     * @throws BadLocationException if the region is not valid
     */
    public static List<String> getLines(IDocument document, IRegion region) throws BadLocationException {
        List<String> results = new ArrayList<String>();
        int start = document.getLineOfOffset(region.getOffset());
        int end = document.getLineOfOffset(region.getOffset() + region.getLength());
        for (int lineAt = start; lineAt <= end; lineAt++) {
            IRegion lineInfo = document.getLineInformation(lineAt);
            String lineContent = document.get(lineInfo.getOffset(), lineInfo.getLength());
            results.add(lineContent);
        }
        return results;
    }

    /**
     * Extracts horizontal tabs in lines.
     * @param lines the source lines
     * @param tabSize the tab column size
     * @return the extracted lines
     */
    public static List<String> extractHorizontalTabs(List<String> lines, int tabSize) {
        List<String> results = new ArrayList<String>();
        for (String line : lines) {
            results.add(extractHorizontalTabs(line, tabSize));
        }
        return results;
    }

    private static String extractHorizontalTabs(String line, int tabSize) {
        assert line != null;
        StringBuilder buf = new StringBuilder();
        int column = 0;
        for (int i = 0, n = line.length(); i < n; i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                int count = tabSize - column % tabSize;
                for (int j = 0; j < count; j++) {
                    buf.append(' ');
                }
            } else {
                buf.append(c);
                column++;
            }
        }
        return buf.toString();
    }

    /**
     * Trims leading whitespaces.
     * @param lines the source lines
     * @return the trimmed lines
     */
    public static List<String> trimLeadingWhitespaces(List<String> lines) {
        if (lines.isEmpty()) {
            return lines;
        }
        String lead = null;
        for (int i = 0, n = lines.size(); i < n; i++) {
            String line = lines.get(i);
            if (line.isEmpty() == false) {
                if (lead == null) {
                    lead = getLeadingWhitespaces(line);
                } else {
                    lead = getCommonPrefix(lead, getLeadingWhitespaces(line));
                }
            }
        }
        if (lead == null || lead.isEmpty()) {
            return lines;
        }
        List<String> results = new ArrayList<String>();
        for (String line : lines) {
            if (line.length() >= lead.length()) {
                results.add(line.substring(lead.length()));
            }
        }
        return results;
    }

    private static String getLeadingWhitespaces(String string) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);
            if (Character.isWhitespace(c) == false) {
                break;
            }
            buf.append(c);
        }
        return buf.toString();
    }

    private static String getCommonPrefix(String a, String b) {
        StringBuilder common = new StringBuilder();
        for (int i = 0, n = Math.min(a.length(), b.length()); i < n; i++) {
            if (a.charAt(i) == b.charAt(i)) {
                common.append(a.charAt(i));
            } else {
                break;
            }
        }
        return common.toString();
    }

    private DocumentUtils() {
        return;
    }
}
