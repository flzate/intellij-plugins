package com.intellij.coldFusion;

import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.coldFusion.UI.editorActions.surroundWith.CfmlSharpSurrounder;
import com.intellij.coldFusion.model.CfmlLanguage;
import com.intellij.lang.LanguageSurrounders;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Nadya.Zabrodina
 * Date: 12/5/11
 */
public class CfmlSurroundWithTest extends CfmlCodeInsightFixtureTestCase {


  public void testSurroundSelectionWithSharps() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsOfCompositeElement() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsInStringText() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsNoSurround() throws Exception {
    doTestNoSurround();
  }

  public void testSurroundSelectionWithSharpsNoSurround2() throws Exception {
    doTestNoSurround();
  }

  public void testSurroundSelectionWithSharpsOfFunctionArgument() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsOfFunctionCall() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsInHtmlTag() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  public void testSurroundSelectionWithSharpsInCfoutput() throws Exception {
    doTest(new CfmlSharpSurrounder());
  }

  private void doTest(final Surrounder surrounder) throws Exception {

    myFixture.configureByFile(Util.getInputDataFileName(getTestName(true)));
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        SurroundWithHandler.invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), surrounder);
      }
    });
    myFixture.checkResultByFile(Util.getExpectedDataFileName(getTestName(true)));
  }

  private void doTestNoSurround() {
    myFixture.configureByFile(Util.getInputDataFileName(getTestName(true)));
    List<SurroundDescriptor> surroundDescriptors = new ArrayList<SurroundDescriptor>();
    surroundDescriptors.addAll(LanguageSurrounders.INSTANCE.allForLanguage(CfmlLanguage.INSTANCE));
    for (SurroundDescriptor descriptor : surroundDescriptors) {
      assertEquals(descriptor
                     .getElementsToSurround(myFixture.getFile(), myFixture.getEditor().getSelectionModel().getSelectionStart(),
                                            myFixture.getEditor().getSelectionModel().getSelectionEnd()), PsiElement.EMPTY_ARRAY);
    }
  }

  @Override
  protected String getBasePath() {
    return "/surroundWith";
  }
}
