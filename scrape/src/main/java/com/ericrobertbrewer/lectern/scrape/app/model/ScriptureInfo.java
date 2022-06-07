package com.ericrobertbrewer.lectern.scrape.app.model;

import java.util.*;

public final class ScriptureInfo {

  public static final String[] TESTAMENTS = {"ot", "nt", "bofm", "dc-testament", "pgp"};
  public static final Map<String, String[]> TESTAMENT_BOOKS =
      Map.of("ot", new String[]{
          "gen", "ex", "lev", "num", "deut",
          "josh", "judg", "ruth", "1-sam", "2-sam", "1-kgs", "2-kgs", "1-chr", "2-chr", "ezra", "neh", "esth",
          "job", "ps", "prov", "eccl", "song",
          "isa", "jer", "lam", "ezek", "dan",
          "hosea", "joel", "amos", "obad", "jonah", "micah", "nahum", "hab", "zeph", "hag", "zech", "mal"
      }, "nt", new String[]{
          "matt", "mark", "luke", "john", "acts",
          "rom", "1-cor", "2-cor", "gal", "eph", "philip", "col", "1-thes", "2-thes",
          "1-tim", "2-tim", "titus", "philem", "heb", "james", "1-pet", "2-pet", "1-jn", "2-jn", "3-jn", "jude",
          "rev"
      }, "bofm", new String[]{
          "bofm-title", "introduction", "three", "eight", "js", "explanation",
          "1-ne", "2-ne", "jacob", "enos", "jarom", "omni",
          "w-of-m", "mosiah", "alma", "hel",
          "3-ne", "4-ne", "morm", "ether", "moro"
      }, "dc-testament", new String[]{
          "introduction", "dc", "od"
      }, "pgp", new String[]{
          "introduction", "moses", "abr", "js-m", "js-h", "a-of-f"
      });
  public static final String[] STUDY_HELPS = {"tg", "bd"};

  /**
   * Books containing only one chapter.
   * <p>
   * During scraping, instead of collecting chapters, append a "/1" to the book's base URL.
   */
  public static final Set<String> BOOKS_ONE_CHAPTER =
      Set.of("obad", "philem", "2-jn", "3-jn", "jude",
          "enos", "jarom", "omni", "w-of-m", "4-ne",
          "js-m", "js-h", "a-of-f");
  /**
   * Names of "books" with text content instead of chapters.
   * <p>
   * Note: Though the name 'introduction' collides for three different testaments,
   * they all happen to be paragraph style.
   */
  public static final Set<String> BOOKS_TEXT_CONTENT =
      Set.of("bofm-title", "introduction", "three", "eight", "js", "explanation");

  private ScriptureInfo() {
  }
}
