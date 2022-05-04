package com.ericrobertbrewer.lectern.text;

import java.io.*;
import java.util.List;

public final class TextUtils {

  public static final String REF_START = "{{{";
  public static final String REF_END = "}}}";

  private TextUtils() {
  }

  public static String removeEscapedReferenceMarkers(String text, int escapedRefs) {
    // If references are surrounded by parentheses, remove them, too.
    int lastEnd = 0;
    while (escapedRefs > 0) {
      final int start = text.indexOf(REF_START, lastEnd);
      final int end = text.indexOf(REF_END, start);
      // Candidates for surrounding parentheses.
      final int parenLeftStart = text.lastIndexOf(" (", start);
      final int parenRightEnd = text.indexOf(")", end);
      // Possible intervening parentheses.
      final int parenLeftEnd = text.lastIndexOf(")", start);
      final int parenRightStart = text.indexOf("(", end);

      if (parenLeftStart >= 0 && parenRightEnd >= 0 &&
        (parenLeftEnd == -1 || parenLeftEnd < parenLeftStart) &&
        (parenRightStart == -1 || parenRightStart > parenRightEnd)) {
        if (escapedRefs > 1 && text.indexOf(REF_START, end) < parenRightEnd) {
          // Handle case of multiple parenthesized references: (see Mosiah 2:41; Alma 41:10).
          // https://www.churchofjesuschrist.org/study/general-conference/2022/04/25aidukaitis?lang=eng
          lastEnd = end;
        } else {
          text = text.substring(0, parenLeftStart) + text.substring(parenRightEnd + 1);
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

  public static void writeLines(List<String> lines, File file) throws IOException {
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
