/**
 * Copyright 2014 ashigeru.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ashigeru.eclipse.internal.codereading.ui.handlers;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.AbstractMultiEditor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.ashigeru.eclipse.codereading.core.utils.DocumentUtils;
import com.ashigeru.eclipse.internal.codereading.ui.Activator;
import com.ashigeru.eclipse.internal.codereading.ui.LogUtil;
import com.ashigeru.eclipse.internal.codereading.ui.dialogs.LogEditDialog;

/**
 * Appends source code snippet into log.
 */
public class LogSnippetHandler extends AbstractHandler {

    static final String DIALOG_KEY_LOG_FILE = "logfile"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = getEditor(event);
        if (editor == null) {
            return null;
        }
        File lastLogFile = loadLogFile();
        List<String> template = getTemplate(editor);
        if (template == null) {
            return null;
        }

        LogEditDialog dialog = new LogEditDialog(HandlerUtil.getActiveShellChecked(event), lastLogFile, template);
        if (dialog.open() != Window.OK) {
            return null;
        }

        File resultFile = dialog.getResultFile();
        List<String> resultContents = dialog.getResultContents();
        try {
            appendLog(resultFile, resultContents);
        } catch (IOException e) {
            IStatus status = LogUtil.log(IStatus.ERROR, e, MessageFormat.format(
                    "Failed to append to log file: {0}",
                    resultFile));
            MessageDialog.openError(
                    HandlerUtil.getActiveShellChecked(event),
                    "Error",
                    status.getMessage());
        }
        saveLogFile(resultFile);
        return null;
    }

    private IEditorPart getEditor(ExecutionEvent event) {
        assert event != null;
        return HandlerUtil.getActiveEditor(event);
    }

    private List<String> getTemplate(IEditorPart editor) {
        assert editor != null;
        IPath path = getPath(editor);
        IDocument document = getDocument(editor);
        IRegion region = getRegion(editor);
        String location = getLocation(path, document, region);
        List<String> lines = getContents(document, region);
        String language = getLanguageKind(path, document, region);

        // FIXME meta-template
        List<String> results = new ArrayList<String>();
        results.add(""); //$NON-NLS-1$
        if (location != null || lines != null) {
            results.add(""); //$NON-NLS-1$
            if (language == null) {
                results.add("```"); //$NON-NLS-1$
            } else {
                results.add(String.format("```%s", language)); //$NON-NLS-1$
            }
            if (location != null) {
                results.add(String.format("// %s", location)); //$NON-NLS-1$
            }
            if (lines != null) {
                results.addAll(lines);
            }
            results.add("```"); //$NON-NLS-1$
            results.add(""); //$NON-NLS-1$
        }
        results.add("********"); //$NON-NLS-1$
        return results;
    }

    private IPath getPath(IEditorPart editor) {
        assert editor != null;
        IEditorInput input = editor.getEditorInput();
        IFile file = (IFile) input.getAdapter(IFile.class);
        if (file != null) {
            IProject project = file.getProject();
            if (project != null) {
                return Path.fromPortableString(project.getName()).append(file.getProjectRelativePath());
            } else {
                return file.getFullPath();
            }
        }
        IPath path = (IPath) input.getAdapter(IPath.class);
        if (path != null) {
            return path;
        }
        String name = input.getName();
        if (name != null) {
            return Path.fromPortableString(name);
        }
        return null;
    }

    private IDocument getDocument(IEditorPart editor) {
        assert editor != null;
        if (editor instanceof ITextEditor) {
            IDocumentProvider provider = ((ITextEditor) editor).getDocumentProvider();
            if (provider != null) {
                return provider.getDocument(editor.getEditorInput());
            }
        } else if (editor instanceof MultiPageEditorPart) {
            IEditorPart[] children = ((MultiPageEditorPart) editor).findEditors(editor.getEditorInput());
            for (IEditorPart child : children) {
                IDocument document = getDocument(child);
                if (document != null) {
                    return document;
                }
            }
        } else if (editor instanceof AbstractMultiEditor) {
            IEditorPart active = ((AbstractMultiEditor) editor).getActiveEditor();
            return getDocument(active);
        }
        return null;
    }

    private IRegion getRegion(IEditorPart editor) {
        assert editor != null;
        ISelectionProvider provider = editor.getSite().getSelectionProvider();
        if (provider != null) {
            ISelection selection = provider.getSelection();
            if (selection instanceof ITextSelection) {
                ITextSelection ts = (ITextSelection) selection;
                int offset = ts.getOffset();
                int length = ts.getLength();
                if (offset >= 0 && length >= 0) {
                    return new Region(offset, length);
                }
            }
        }
        return null;
    }

    private String getLocation(IPath path, IDocument document, IRegion region) {
        if (path == null) {
            return null;
        }
        if (document != null && region != null) {
            int lineNumber = getLineNumber(document, region);
            if (lineNumber >= 0) {
                return String.format("%s:L%d", path.toPortableString(), lineNumber + 1); //$NON-NLS-1$
            }
        }
        return path.toPortableString();
    }

    private int getLineNumber(IDocument document, IRegion region) {
        assert document != null;
        assert region != null;
        try {
            return document.getLineOfOffset(region.getOffset());
        } catch (BadLocationException e) {
            LogUtil.log(IStatus.ERROR, e, MessageFormat.format(
                    "Failed to compute line number: {0}",
                    region));
            return -1;
        }
    }

    /**
     * Infers programming language kind of the target.
     * @param path the target path
     * @param document the target document
     * @param region the selected region
     * @return the programming language kind, or {@code null} if it is not inferred
     */
    private String getLanguageKind(IPath path, IDocument document, IRegion region) {
        if (path == null) {
            return null;
        }
        String extension = path.getFileExtension();
        if (extension != null) {
            return extension.toLowerCase();
        }
        return null;
    }

    private List<String> getContents(IDocument document, IRegion region) {
        if (document == null || region == null) {
            return null;
        }
        int tabSize = getTabSize();
        try {
            IRegion block = DocumentUtils.toLines(document, region);
            List<String> lines = DocumentUtils.getLines(document, block);
            lines = DocumentUtils.extractHorizontalTabs(lines, tabSize);
            lines =  DocumentUtils.trimLeadingWhitespaces(lines);
            return lines;
        } catch (BadLocationException e) {
            LogUtil.log(IStatus.ERROR, e, MessageFormat.format(
                    "Failed to obtain line contents: {0}",
                    region));
            return null;
        }
    }

    private int getTabSize() {
        IPreferenceStore prefs = EditorsUI.getPreferenceStore();
        int value = prefs.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        return value;
    }

    private File loadLogFile() {
        IDialogSettings settings = Activator.getDialogSettings(getClass().getSimpleName());
        String value = settings.get(DIALOG_KEY_LOG_FILE);
        if (value == null) {
            return null;
        }
        return new File(value);
    }

    private void saveLogFile(File file) {
        IDialogSettings settings = Activator.getDialogSettings(getClass().getSimpleName());
        if (file != null) {
            settings.put(DIALOG_KEY_LOG_FILE, file.getPath());
        }
    }

    private void appendLog(File file, List<String> lines) throws IOException {
        String lineBreak = getLogLineBreak();

        // FIXME out of UI thread
        Appendable appendable = openLogFile(file);
        try {
            for (String line : lines) {
                appendable.append(line);
                appendable.append(lineBreak);
            }
        } finally {
            if (appendable instanceof Closeable) {
                ((Closeable) appendable).close();
            }
        }
    }

    private Charset getLogFileEncoding() {
        // FIXME make it configurable
        return Charset.forName("UTF-8"); //$NON-NLS-1$
    }

    private String getLogLineBreak() {
        // FIXME make it configurable
        return String.format("%n"); //$NON-NLS-1$
    }

    private Writer openLogFile(File file) throws IOException {
        Charset encoding = getLogFileEncoding();
        File parent = file.getParentFile();
        if (parent.mkdirs() == false && parent.isDirectory() == false) {
            throw new IOException(MessageFormat.format(
                    "Failed to create directory for create log file: {0}",
                    parent));
        }
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), encoding));
    }
}
