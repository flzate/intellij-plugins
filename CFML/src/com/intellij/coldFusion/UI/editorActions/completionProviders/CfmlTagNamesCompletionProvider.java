package com.intellij.coldFusion.UI.editorActions.completionProviders;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.coldFusion.model.CfmlUtil;
import com.intellij.coldFusion.model.files.CfmlFileViewProvider;
import com.intellij.coldFusion.model.psi.CfmlImport;
import com.intellij.coldFusion.model.psi.stubs.CfmlIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: vnikolaenko
 * Date: 17.03.2009
 * Time: 14:17:13
 * To change this template use File | Settings | File Templates.
 */
class CfmlTagNamesCompletionProvider extends CompletionProvider<CompletionParameters> {
  public void addCompletions(@NotNull final CompletionParameters parameters,
                             final ProcessingContext context,
                             @NotNull final CompletionResultSet result) {
    if (!(parameters.getOriginalFile().getViewProvider() instanceof CfmlFileViewProvider)) {
      return;
    }

    for (String s : CfmlUtil.getTagList(parameters.getPosition().getProject())) {
      result.addElement(LookupElementBuilder.create(s).withCaseSensitivity(false));
    }

    CfmlImport[] imports = PsiTreeUtil.getChildrenOfType(parameters.getOriginalFile(), CfmlImport.class);
    if (imports == null) {
      return;
    }
    for (CfmlImport cfmlImport : imports) {
      final String prefix = cfmlImport.getPrefix();
      if (prefix != null) {
        addCompletionsFromDirectory(result, parameters, cfmlImport.getImportString(), prefix);
      }
    }
  }

  private void addCompletionsFromDirectory(CompletionResultSet result, CompletionParameters parameters, String libtag, String prefix) {
    final PsiFile originalFile = parameters.getOriginalFile();
    final VirtualFile folder = CfmlUtil.findFileByLibTag(originalFile, libtag);
    if (folder != null && folder.isDirectory()) {
      final Set<String> names = new THashSet<String>(CfmlIndex.getInstance(originalFile.getProject()).getAllComponentsNames());
      names.retainAll(ContainerUtil.map(folder.getChildren(), new Function<VirtualFile, String>() {
        @Override
        public String fun(VirtualFile virtualFile) {
          return FileUtil.getNameWithoutExtension(virtualFile.getName()).toLowerCase();
        }
      }));
      for (String componentName : names) {
        result.addElement(LookupElementBuilder.create(prefix + ':' + componentName).withCaseSensitivity(false));
      }
    }
  }
}
