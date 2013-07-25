package com.intellij.coldFusion.model.psi.stubs;

import com.intellij.coldFusion.model.psi.CfmlComponent;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;

/**
 * @author vnikolaenko
 */
public class CfmlInheritanceIndex extends StringStubIndexExtension<CfmlComponent> {
  public static final StubIndexKey<String, CfmlComponent> KEY = StubIndexKey.createIndexKey("cfml.inheritors.shortName");

  @NotNull
  public StubIndexKey<String, CfmlComponent> getKey() {
    return KEY;
  }
}
