package com.ericrobertbrewer.lectern.scrape.text;

import java.io.*;
import java.util.List;

public final class TextUtils {

  public static final String REF_START = "{{";
  public static final String REF_END = "}}";

  private TextUtils() {
  }

  public static String removeEscapedReferenceMarkers(String text, int escapedRefs) {
    return removeEscapedReferenceMarkers(text, escapedRefs, true);
  }

  public static String removeEscapedReferenceMarkers(String text, int escapedRefs, boolean removeParentheses) {
    // If references are surrounded by parentheses, remove them, too.
    int lastEnd = 0;
    while (escapedRefs > 0) {
      final int start = text.indexOf(REF_START, lastEnd);
      final int end = text.indexOf(REF_END, start);
      final int parenLeftOpen, parenRightClose; // Candidates for surrounding parentheses.
      final int parenLeftClose, parenRightOpen; // Possible intervening parentheses.
      if (removeParentheses) {
        parenLeftOpen = text.lastIndexOf(" (", start);
        parenRightClose = text.indexOf(")", end);
        parenLeftClose = text.lastIndexOf(")", start);
        parenRightOpen = text.indexOf("(", end);
      } else {
        parenLeftOpen = parenRightClose = parenLeftClose = parenRightOpen = -1;
      }

      if (parenLeftOpen >= 0 && parenRightClose >= 0 &&
          (parenLeftClose == -1 || parenLeftClose < parenLeftOpen) &&
          (parenRightOpen == -1 || parenRightOpen > parenRightClose)) {
        if (escapedRefs > 1 && text.indexOf(REF_START, end) < parenRightClose) {
          // Handle case of multiple parenthesized references: (see Mosiah 2:41; Alma 41:10).
          // https://www.churchofjesuschrist.org/study/general-conference/2022/04/25aidukaitis?lang=eng
          lastEnd = end;
        } else {
          text = text.substring(0, parenLeftOpen) + text.substring(parenRightClose + 1);
          lastEnd = 0;
        }
      } else {
        // Some references are recited: "the <a>eighth article of faith</a> states,"
        // https://www.churchofjesuschrist.org/study/general-conference/2022/04/14bednar?lang=eng
        text = text.substring(0, start) + // Left of ref marker.
            text.substring(start + REF_START.length(), end) + // Inside ref marker.
            text.substring(end + REF_END.length()); // Right of ref marker.
        lastEnd = 0;
      }
      escapedRefs--;
    }
    return text;
  }

  public static void writeToFile(List<String> lines, File file) throws IOException {
    if (!file.createNewFile()) {
      throw new RuntimeException("Unable to create file: " + file.getPath());
    }
    if (!file.canWrite() && !file.setWritable(true)) {
      throw new RuntimeException("Unable to write to file: " + file.getPath());
    }
    try (final OutputStream outputStream = new FileOutputStream(file);
         final PrintStream out = new PrintStream(outputStream)) {
      for (String line : lines) {
        out.println(line);
      }
    }
  }
}
