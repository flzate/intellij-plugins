package com.intellij.coldFusion.model.info;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author vnikolaenko
 */
public class CfmlTypesInfo {
  private static String[] ourTypeNames = {
    "string",
    "numeric",
    "boolean",
    "timespan",
    "variablename",
    "url",
    "object",
    "regex",
    "struct",
    "datetime",
    "char",
    "any",
    "array",
    "binary",
    "query",
    "querycolumn",
    "node"
  };

  public static final int STRING_TYPE = 0;
  public static final int NUMERIC_TYPE = 1;
  public static final int BOOLEAN_TYPE = 2;
  public static final int TIMESPAN_TYPE = 3;
  public static final int VARIABLENAME_TYPE = 4;
  public static final int URL_TYPE = 5;
  public static final int OBJECT_TYPE = 6;
  public static final int REGEX_TYPE = 7;
  public static final int STRUCT_TYPE = 8;
  public static final int DATETIME_TYPE = 9;
  public static final int CHAR_TYPE = 10;
  public static final int ANY_TYPE = 11;
  public static final int ARRAY_TYPE = 12;
  public static final int BINARY_TYPE = 13;
  public static final int QUERY_TYPE = 14;
  public static final int QUERYCOLUMN_TYPE = 15;
  public static final int NODE_TYPE = 16;

  public static int getTypeByString(@Nullable String s) {
    if (s == null) {
      return ANY_TYPE;
    }
    final int find = ArrayUtil.find(ourTypeNames, s.toLowerCase());
    if (find != -1) {
      return find;
    }
    return ANY_TYPE;
  }
}
