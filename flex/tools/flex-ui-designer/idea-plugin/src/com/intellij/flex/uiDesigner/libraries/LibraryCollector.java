package com.intellij.flex.uiDesigner.libraries;

import com.intellij.facet.FacetManager;
import com.intellij.flex.uiDesigner.LogMessageUtil;
import com.intellij.lang.javascript.flex.AutogeneratedLibraryUtils;
import com.intellij.lang.javascript.flex.FlexFacet;
import com.intellij.lang.javascript.flex.FlexUtils;
import com.intellij.lang.javascript.flex.library.FlexLibraryType;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexBuildConfigurationManager;
import com.intellij.lang.javascript.flex.projectStructure.model.FlexIdeBuildConfiguration;
import com.intellij.lang.javascript.flex.projectStructure.options.BCUtils;
import com.intellij.lang.javascript.flex.projectStructure.options.FlexProjectRootsUtil;
import com.intellij.lang.javascript.flex.sdk.AirMobileSdkType;
import com.intellij.lang.javascript.flex.sdk.AirSdkType;
import com.intellij.lang.javascript.flex.sdk.FlexSdkType;
import com.intellij.lang.javascript.psi.resolve.JSResolveUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import com.intellij.util.PlatformUtils;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class LibraryCollector {
  private static final String DOT_SWC = ".swc";

  final List<Library> externalLibraries = new ArrayList<Library>();
  final List<Library> sdkLibraries = new ArrayList<Library>();
  private VirtualFile globalLibrary;

  final LibraryStyleInfoCollector initializer;
  private final Project project;
  private String flexmojosFlexSdkRootPath;

  // AS-200
  private final Set<VirtualFile> uniqueGuard = new THashSet<VirtualFile>();

  private final LibraryManager libraryManager;

  // user can set flex sdk and autogenerated sdk for facet simultaneous, so we prevent duplicates in externalLibraries
  private boolean flexSdkRegistered = false;
  private String flexSdkVersion;

  public LibraryCollector(LibraryManager libraryManager, LibraryStyleInfoCollector initializer, Project project) {
    this.libraryManager = libraryManager;
    this.initializer = initializer;
    this.project = project;
  }

  public String getFlexSdkVersion() {
    return flexSdkVersion;
  }

  @NotNull
  public VirtualFile getGlobalLibrary() {
    return globalLibrary;
  }

  private static boolean isAutomationOrUselessLibrary(String name) {
    return name.startsWith("qtp") || name.startsWith("automation")
           || name.equals("flex.swc") /* flex.swc is only aggregation library */
           || name.equals("servicemonitor.swc")  /* aircore contains all classes */
           || name.equals("utilities.swc")  /* flex sdk 4.1 */
           || name.equals("core.swc") /* hero (4.5) aggregation library */
           || name.equals("applicationupdater.swc") /* applicationupdater_ui contains all classes */
           || name.equals("flash-integration.swc") || name.equals("authoringsupport.swc");
  }

  private boolean isGlobalLibrary(String name, VirtualFile jarFile) {
    final boolean isAirglobal = name.equals("airglobal.swc");
    final boolean isGlobal = isAirglobal || name.equals("playerglobal.swc");
    // flexmojos project may has playerglobal and airglobal simultaneous
    if (isGlobal && (globalLibrary == null || isAirglobal)) {
      globalLibrary = Library.getCatalogFile(jarFile);
    }
    return isGlobal;
  }

  @Nullable
  private VirtualFile getRealFileIfValidSwc(final VirtualFile jarFile) {
    if (jarFile.getFileSystem() instanceof JarFileSystem) {
      VirtualFile file = JarFileSystem.getInstance().getVirtualFileForJar(jarFile);
      if (file != null && !file.isDirectory() && file.getName().endsWith(DOT_SWC) && !isGlobalLibrary(file.getName(), jarFile) &&
          isSwfAndCatalogExists(jarFile) && uniqueGuard.add(file)) {
        return file;
      }
    }

    return null;
  }

  public void collect(Module module) {
    OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
    if (PlatformUtils.isFlexIde()) {
      final FlexIdeBuildConfiguration bc = FlexBuildConfigurationManager.getInstance(module).getActiveConfiguration();
      orderEntries = ContainerUtil.mapNotNull(orderEntries, new Function<OrderEntry, OrderEntry>() {
        @Override
        public OrderEntry fun(OrderEntry orderEntry) {
          if (orderEntry instanceof LibraryOrderEntry) {
            com.intellij.openapi.roots.libraries.Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
            if (library == null || !(((LibraryEx)library).getType() instanceof FlexLibraryType) ||
                !FlexProjectRootsUtil.dependsOnLibrary(bc, library, false)) {
              return null;
            }
          }
          return orderEntry;
        }
      }, new OrderEntry[0]);

      final Sdk sdk = FlexUtils.createFlexSdkWrapper(bc);
      assert sdk != null;
      Function<VirtualFile, VirtualFile> f = new Function<VirtualFile, VirtualFile>() {
        @Override
        public VirtualFile fun(VirtualFile virtualFile) {
          String swcPath = VirtualFileManager.extractPath(StringUtil.trimEnd(virtualFile.getUrl(), JarFileSystem.JAR_SEPARATOR));
          return BCUtils.getSdkEntryLinkageType(swcPath, bc) != null ? virtualFile : null;
        }
      };
      VirtualFile[] roots = ContainerUtil.mapNotNull(sdk.getRootProvider().getFiles(OrderRootType.CLASSES), f, new VirtualFile[0]);
      collectFromSdkOrderEntry(roots);
      flexSdkRegistered = true;
      flexSdkVersion = sdk.getVersionString();
    }
    else {
      // flexmojos module has Java as module jdk, so, grab flex sdk from facet
      FlexFacet facet = FacetManager.getInstance(module).getFacetByType(FlexFacet.ID);
      if (facet != null) {
        final Sdk sdk = facet.getConfiguration().getFlexSdk();
        assert sdk != null;
        final String sdkHomePath = sdk.getHomePath();
        assert sdkHomePath != null;
        flexmojosFlexSdkRootPath = sdkHomePath.substring(0, sdkHomePath.indexOf("flex"));

        flexSdkRegistered = true;
        flexSdkVersion = sdk.getVersionString();
      }
    }

    for (OrderEntry o : orderEntries) {
      final DependencyScope scope = (o instanceof ExportableOrderEntry) ? ((ExportableOrderEntry)o).getScope() : DependencyScope.COMPILE;
      if (scope == DependencyScope.RUNTIME || scope == DependencyScope.TEST) {
        continue;
      }

      if (o instanceof LibraryOrderEntry) {
        collectFromLibraryOrderEnrty((LibraryOrderEntry)o);
      }
      else if (!flexSdkRegistered && o instanceof JdkOrderEntry) {
        final JdkOrderEntry jdkOrderEntry = ((JdkOrderEntry)o);
        SdkType sdkType = jdkOrderEntry.getJdk().getSdkType();
        if (sdkType instanceof FlexSdkType || sdkType instanceof AirSdkType || sdkType instanceof AirMobileSdkType) {
          collectFromSdkOrderEntry(jdkOrderEntry.getRootFiles(OrderRootType.CLASSES));
          flexSdkRegistered = true;
          flexSdkVersion = jdkOrderEntry.getJdk().getVersionString();

          globalCatalogForTests(sdkType);
        }
      }
      else if (o instanceof ModuleOrderEntry) {
        collectLibrariesFromModuleDependency(((ModuleOrderEntry)o).getModule());
      }
    }

    assert flexSdkVersion != null && flexSdkVersion.length() >= 3;
    flexSdkVersion = flexSdkVersion.substring(0, 3);
  }

  private void globalCatalogForTests(SdkType sdkType) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      assert globalLibrary == null;
      globalLibrary = LibraryUtil.getTestGlobalLibrary(sdkType instanceof FlexSdkType);
    }
  }

  private boolean isFlexSdkLibrary(VirtualFile file, VirtualFile jarFile) {
    if (flexmojosFlexSdkRootPath != null) {
      return file.getPath().startsWith(flexmojosFlexSdkRootPath);
    }

    final String name = file.getName();
    for (Pair<String, String> pair : FlexDefinitionMapProcessor.FLEX_LIBS_PATTERNS) {
      if (name.startsWith(pair.first)) {
        return libraryContains(pair.second, jarFile);
      }
    }
    
    if (name.equals("textLayout.swc")) {
      return libraryContains("flashx.textLayout.EditClasses", jarFile);
    }
    else if (name.equals("osmf.swc")) {
      return libraryContains("org.osmf.utils.Version", jarFile);
    }
    // todo check and add
    else if (name.startsWith("miglayout-")) {
      return true;
    }

    return false;
  }

  private boolean libraryContains(String className, VirtualFile jarFile) {
    return JSResolveUtil.findClassByQName(className, GlobalSearchScope.fileScope(project, Library.getSwfFile(jarFile))) != null;
  }

  private void collectFromSdkOrderEntry(VirtualFile[] roots) {
    for (VirtualFile jarFile : roots) {
      VirtualFile file = getRealFileIfValidSwc(jarFile);
      if (file != null && !isAutomationOrUselessLibrary(file.getName())) {
        addLibrary(jarFile, true);
      }
    }
  }

  private void collectFromLibraryOrderEnrty(LibraryOrderEntry libraryOrderEntry) {
    final boolean isAutogeneratedLibrary = AutogeneratedLibraryUtils.isAutogeneratedLibrary(libraryOrderEntry);
    if (isAutogeneratedLibrary && flexSdkRegistered) {
      return;
    }

    for (VirtualFile jarFile : libraryOrderEntry.getRootFiles(OrderRootType.CLASSES)) {
      VirtualFile file = getRealFileIfValidSwc(jarFile);
      if (file != null && (!isAutogeneratedLibrary || !isAutomationOrUselessLibrary(file.getName()))) {
        addLibrary(jarFile, isFlexSdkLibrary(file, jarFile));
      }
    }
  }

  private void addLibrary(VirtualFile jarFile, boolean isFromFlexSdk) {
    (isFromFlexSdk ? sdkLibraries : externalLibraries).add(libraryManager.createOriginalLibrary(jarFile, initializer));
  }

  // IDEA-74117
  private static boolean isSwfAndCatalogExists(VirtualFile jarFile) {
    if (Library.getSwfFile(jarFile) == null || Library.getCatalogFile(jarFile) == null) {
      LogMessageUtil.LOG.warn("SWC is corrupted (library.swf or catalog.xml doesn't exists): " + jarFile.getPath());
      return false;
    }

    return true;
  }

  // 7
  private static void collectLibrariesFromModuleDependency(Module module) {
    for (OrderEntry o : ModuleRootManager.getInstance(module).getOrderEntries()) {
      if (!(o instanceof ExportableOrderEntry) || !((ExportableOrderEntry)o).isExported()) {
        continue;
      }

      final DependencyScope scope = ((ExportableOrderEntry)o).getScope();
      if (scope == DependencyScope.RUNTIME || scope == DependencyScope.TEST) {
        continue;
      }

      if (o instanceof LibraryOrderEntry) {
        LogMessageUtil.LOG.warn("exported lib in module dependency is prohibited");
      }
    }
  }
}