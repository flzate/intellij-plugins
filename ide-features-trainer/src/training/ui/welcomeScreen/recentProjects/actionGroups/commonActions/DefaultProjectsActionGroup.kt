package training.ui.welcomeScreen.recentProjects.actionGroups.commonActions

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.actionSystem.AnAction
import training.ui.welcomeScreen.recentProjects.IFTRecentProjectsManager
import training.ui.welcomeScreen.recentProjects.actionGroups.CommonActionGroup

class DefaultProjectsActionGroup : CommonActionGroup("Projects", emptyList()) {

  override fun getActions(): Array<AnAction?> {
    val elements = (RecentProjectsManager.getInstance() as IFTRecentProjectsManager).getOriginalRecentProjectsActions()
    if (elements.isNullOrEmpty()) {
      return emptyArray()
    }
    if (!isExpanded) return arrayOf(this)
    return arrayOf(this, *elements)
  }
}