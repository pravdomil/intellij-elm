package org.elm.ide.test.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.FileUrlProvider
import com.intellij.execution.testframework.sm.TestsLocationProviderUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.elm.ide.test.core.ElmPluginHelper
import org.elm.ide.test.core.ErrorLabelLocation
import org.elm.ide.test.core.LabelProtocol
import org.elm.ide.test.core.LabelProtocol.*
import org.elm.ide.test.core.LabelUtils


class ElmTestLocator private constructor() : FileUrlProvider() {

    override fun getLocation(protocol: String, path: String, metainfo: String?, project: Project, scope: GlobalSearchScope): List<Location<*>> {
        when (val p = LabelProtocol.valueOf(protocol)) {
            ERROR_PROTOCOL -> {
                val label = ErrorLabelLocation.fromUrl(path)
                val systemIndependentPath = FileUtil.toSystemIndependentName(label.file)
                val virtualFiles = TestsLocationProviderUtil.findSuitableFilesFor(systemIndependentPath, project)
                return virtualFiles
                        .mapNotNull { getErrorLocation(label.line, label.column, project, it) }
            }

            DESCRIBE_PROTOCOL, TEST_PROTOCOL -> {
                val (filePath, labels) = LabelUtils.fromLocationUrlPath(path)

                val systemIndependentPath = FileUtil.toSystemIndependentName(filePath)
                return TestsLocationProviderUtil.findSuitableFilesFor(systemIndependentPath, project)
                        .mapNotNull {
                            getLocation(p == DESCRIBE_PROTOCOL, labels, project, it)
                        }
            }

            else -> {
                return super.getLocation(protocol, path, metainfo, project, scope)
            }
        }
    }

    private fun getLocation(isDescribe: Boolean, labels: String, project: Project, virtualFile: VirtualFile): Location<*>? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null

        val found = ElmPluginHelper.getPsiElement(isDescribe, labels, psiFile)
        return PsiLocation.fromPsiElement(project, found)
    }

    private fun getErrorLocation(line: Int, column: Int, project: Project, virtualFile: VirtualFile): Location<*>? {
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null

        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                ?: return PsiLocation.fromPsiElement(project, psiFile)

        val offset = document.getLineStartOffset(line - 1) + column - 1
        val element = psiFile.findElementAt(offset) ?: return PsiLocation.fromPsiElement(project, psiFile)

        return PsiLocation.fromPsiElement(project, element)
    }

    companion object {
        val INSTANCE = ElmTestLocator()
    }
}
