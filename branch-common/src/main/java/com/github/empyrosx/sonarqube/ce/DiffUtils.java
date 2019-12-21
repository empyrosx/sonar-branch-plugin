package com.github.empyrosx.sonarqube.ce;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DiffUtils {

    /**
     * Calculates base source line for new line.
     *
     * @param diffs list of git diffs sorted by date
     * @param line  new line
     * @return null if line is new or source line before applying this diffs
     */
    public static Integer getBaseSourceLine(List<String> diffs, int line) {
        List<Integer> addedLines = new ArrayList<>();
        List<Integer> removedLines = new ArrayList<>();

        for (String diff : diffs) {

            Integer oldCounter = 0;
            Integer newCounter = 0;
            Integer oldLineStart = 0;
            Integer newLineStart = 0;

            String[] lines = diff.split("\n");
            for (String header : lines) {
                if (header.startsWith("@@")) {
                    String delta = header.substring(header.indexOf("@@") + 3, header.lastIndexOf("@@"));
                    String[] intervals = delta.split(" ");
                    oldLineStart = Math.abs(Integer.parseInt(intervals[0].split(",")[0]));
                    newLineStart = Math.abs(Integer.parseInt(intervals[1].split(",")[0]));
                    oldCounter = 0;
                    newCounter = 0;
                } else if (header.startsWith("-")) {
                    // line is deleted
                    if (addedLines.contains(newLineStart + newCounter)) {
                        addedLines.sort(Integer::compareTo);
                        int ind = addedLines.indexOf(newLineStart + newCounter);
                        for (int i = ind + 1; i < addedLines.size(); i++) {
                            addedLines.set(i, addedLines.get(i) - 1);
                        }
                        addedLines.remove((Integer) (newLineStart + newCounter));
                    } else {
                        removedLines.add(oldLineStart + oldCounter);
                        for (int i = 0; i < addedLines.size(); i++) {
                            Integer value = addedLines.get(i);
                            if (value > oldLineStart + oldCounter) {
                                addedLines.set(i, value - 1);
                            }
                        }
                        oldCounter++;
                    }
                } else if (header.startsWith("+")) {
                    // line is added
                    addedLines.add(newLineStart + newCounter);
                    addedLines.sort(Integer::compareTo);
                    int ind = addedLines.indexOf(newLineStart + newCounter);
                    for (int i = ind + 1; i < addedLines.size(); i++) {
                        addedLines.set(i, addedLines.get(i) + 1);
                    }
                    newCounter++;
                } else {
                    newCounter++;
                    oldCounter++;
                }
            }
        }

        if (addedLines.contains(line)) {
            return null;
        }

        return calcLineIndex(line, addedLines, removedLines);
    }

    @Nonnull
    private static Integer calcLineIndex(int line, List<Integer> addedLines, List<Integer> removedLines) {
        int addedLinesCount = 0;
        for (Integer item : addedLines) {
            if (item < line) {
                addedLinesCount++;
            } else {
                break;
            }
        }

        int removedLinesCount = 0;
        for (Integer item : removedLines) {
            if (item < line) {
                removedLinesCount++;
            } else {
                break;
            }
        }

        return line - addedLinesCount + removedLinesCount;
    }
}
