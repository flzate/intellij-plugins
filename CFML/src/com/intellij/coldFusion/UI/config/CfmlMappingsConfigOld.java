package com.intellij.coldFusion.UI.config;

import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * @author vnikolaenko
 */
@Deprecated
@Tag("CfmlMappingsConfig")
public class CfmlMappingsConfigOld implements Cloneable {
  @NotNull
  private Map<String, String> serverMappings = new HashMap<String, String>();

  public CfmlMappingsConfigOld(@NotNull Map<String, String> mappings) {
    serverMappings = mappings;
  }

  public CfmlMappingsConfigOld() {
  }

  @NotNull
  @Tag("server_mappings")
  @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, keyAttributeName = "directory",
                 entryTagName = "mapping", valueAttributeName = "logical_path", surroundValueWithTag = false)
  public Map<String, String> getServerMappings() {
    return serverMappings;
  }

  public void setServerMappings(@NotNull Map<String, String> serverMappings) {
    this.serverMappings = serverMappings;
  }


  public List<String> mapVirtualToReal(@NotNull String virtualPath) {
    List<String> result = new LinkedList<String>();

    Set<Map.Entry<String, String>> entries = serverMappings.entrySet();
    for (Map.Entry<String, String> entry : entries) {
      StringTokenizer st_lp = new StringTokenizer(entry.getKey(), "\\/");
      StringTokenizer st = new StringTokenizer(virtualPath, ".");
      int numberOfTokens = st.countTokens();

      if (numberOfTokens < st_lp.countTokens()) {
        continue;
      }

      boolean checkFailed = false;
      while (st_lp.hasMoreTokens()) {
        if (!st_lp.nextToken().equals(st.nextToken())) {
          checkFailed = true;
          break;
        }
      }

      if (checkFailed) {
        continue;
      }

      StringBuilder relativePath = new StringBuilder(entry.getValue());

      while (st.hasMoreTokens()) {
        relativePath.append(File.separatorChar);
        relativePath.append(st.nextToken());
      }

      result.add(relativePath.toString());
    }
    return result;
  }

  @Override
  public CfmlMappingsConfigOld clone() {
    HashMap<String, String> newServerMappings = new HashMap<String, String>();
    newServerMappings.putAll(serverMappings);
    return new CfmlMappingsConfigOld(newServerMappings);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final CfmlMappingsConfigOld that = (CfmlMappingsConfigOld)o;

    if (!serverMappings.equals(that.serverMappings)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return serverMappings.hashCode();
  }
}