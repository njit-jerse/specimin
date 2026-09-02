package org.example;

import external.Helper;

public class AtlasData {

  public interface Field<T> {
    void parse(T object);
  }

  public static class Page {
    public int width;
    public String name;
  }

  public void load(Page page) {
    final String[] entry = new String[5];
    Field<Page> field =
        new Field<Page>() {
          public void parse(Page page) {
            // The scope `Integer` is a NameExpr denoting a type, not a value.
            page.width = Integer.parseInt(entry[1]);
            // An unsolved call, so a stub is generated and addInformation runs.
            page.name = Helper.normalize(entry[0]);
          }
        };
    field.parse(page);
  }
}
