package com.opencode.pycharm.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile

val Editor.virtualFile: VirtualFile?
    get() = FileDocumentManager.getInstance().getFile(document)
