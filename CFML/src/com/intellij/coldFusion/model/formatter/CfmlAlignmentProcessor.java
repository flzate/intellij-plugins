package com.intellij.coldFusion.model.formatter;

import com.intellij.coldFusion.model.lexer.CfscriptTokenTypes;
import com.intellij.coldFusion.model.parsers.CfmlElementTypes;
import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

/**
 * Created by IntelliJ IDEA.
 * User: Nadya.Zabrodina
 * Date: 8/9/11
 */
public class CfmlAlignmentProcessor extends CfmlFormatterUtil {

  private final ASTNode myNode;
  private final Alignment myBaseAlignment;
  private final CommonCodeStyleSettings mySettings;

  public CfmlAlignmentProcessor(ASTNode node,
                                CommonCodeStyleSettings settings) {
    myNode = node;
    myBaseAlignment = Alignment.createAlignment();
    mySettings = settings;
  }


  @Nullable
  Alignment createChildAlignment() {
    IElementType parentType = myNode.getElementType();


    if (parentType == CfmlElementTypes.BINARY_EXPRESSION && mySettings.ALIGN_MULTILINE_BINARY_OPERATION) {
      return myBaseAlignment;
    }
    if (parentType == CfmlElementTypes.TERNARY_EXPRESSION && mySettings.ALIGN_MULTILINE_TERNARY_OPERATION) {
      return myBaseAlignment;
    }

    if (parentType == CfmlElementTypes.PARAMETERS_LIST || parentType == CfmlElementTypes.ARGUMENT_LIST) {
      ASTNode boundParent = myNode.getTreeParent();
      IElementType boundElType = boundParent.getElementType();
      boolean doAlign = false;
      if (boundElType == CfmlElementTypes.FUNCTION_DEFINITION) {
        doAlign = mySettings.ALIGN_MULTILINE_PARAMETERS;
      }
      else if (boundElType == CfmlElementTypes.FUNCTION_CALL_EXPRESSION) {
        doAlign = mySettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS;
      }
      if (doAlign) {
        return myBaseAlignment;
      }
    }

    if (parentType == CfmlElementTypes.FOREXPRESSION) {
      if (mySettings.ALIGN_MULTILINE_FOR) {
        return myBaseAlignment;
      }
    }

    return null;
  }
}
