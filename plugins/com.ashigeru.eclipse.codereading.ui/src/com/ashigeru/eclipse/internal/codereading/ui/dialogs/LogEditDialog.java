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
package com.ashigeru.eclipse.internal.codereading.ui.dialogs;

import java.io.File;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ashigeru.eclipse.codereading.core.utils.DocumentUtils;

/**
 * Dialog for input log contents.
 */
public class LogEditDialog extends Dialog {

    private static final FieldDecoration DECORATION_REQUIRED =
            FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_REQUIRED);

    private static final FieldDecoration DECORATION_ERROR =
            FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);

    private final File defaultLogFile;

    private final IDocument document;

    private Text fieldLogFile;

    private ControlDecoration decorationLogFile;

    private TextViewer fieldLogContents;

    private File resultFile;

    /**
     * Creates a new instance.
     * @param parentShell the parent shell
     * @param defaultLogFile the default log file (nullable)
     * @param initialContents the initial contents
     */
    public LogEditDialog(
            Shell parentShell,
            File defaultLogFile,
            List<String> initialContents) {
        super(parentShell);
        this.defaultLogFile = defaultLogFile;
        this.document = linesToDocument(initialContents);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite pane = (Composite) super.createDialogArea(parent);
        GridLayout layout = (GridLayout) pane.getLayout();
        layout.verticalSpacing = 0;
        layout.numColumns = 2;

        createLogFileSection(pane);
        createLogContentsSection(pane);

        applyDialogFont(pane);
        return pane;
    }

    private void createLogFileSection(Composite pane) {
        assert pane != null;
        Label label = new Label(pane, SWT.NONE);
        label.setText("Log File:");
        GridDataFactory.swtDefaults()
            .span(2, 1)
            .hint(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH, SWT.DEFAULT)
            .align(SWT.FILL, SWT.BEGINNING)
            .grab(true, false)
            .applyTo(label);

        this.fieldLogFile = new Text(pane, SWT.SINGLE | SWT.BORDER);
        if (defaultLogFile != null) {
            fieldLogFile.setText(defaultLogFile.getPath());
        }
        GridDataFactory.swtDefaults()
            .indent(FieldDecorationRegistry.getDefault().getMaximumDecorationWidth(), 0)
            .align(SWT.FILL, SWT.CENTER)
            .grab(true, false)
            .applyTo(fieldLogFile);

        this.decorationLogFile = new ControlDecoration(fieldLogFile, SWT.LEFT | SWT.TOP);
        decorationLogFile.setDescriptionText("Log file must be specified.");

        Button browse = new Button(pane, SWT.PUSH);
        browse.setText("Browse");
        GridDataFactory.swtDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .grab(false, false)
            .applyTo(browse);

        fieldLogFile.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(Event event) {
                onModifyLogFle();
            }
        });
        browse.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                onSelectLogFile();
            }
        });
    }

    private void createLogContentsSection(Composite pane) {
        assert pane != null;
        Label label = new Label(pane, SWT.NONE);
        label.setText("Contents:");
        GridDataFactory.swtDefaults()
            .align(SWT.FILL, SWT.BEGINNING)
            .span(2, 1)
            .hint(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH, SWT.DEFAULT)
            .grab(true, false)
            .applyTo(label);

        this.fieldLogContents = new TextViewer(pane, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        fieldLogContents.setEditable(true);
        GridDataFactory.swtDefaults()
            .align(SWT.FILL, SWT.FILL)
            .span(2, 1)
            .hint(convertWidthInCharsToPixels(80), convertHeightInCharsToPixels(20))
            .grab(true, true)
            .applyTo(fieldLogContents.getControl());
        fieldLogContents.setDocument(document);

        fieldLogContents.getControl().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
    }

    void onSelectLogFile() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setText("Log File");
        String last = fieldLogFile.getText();
        if (last.isEmpty() == false) {
            File file = new File(last);
            dialog.setFilterPath(file.getParent());
            dialog.setFileName(file.getName());
        }
        String result = dialog.open();
        if (result != null) {
            fieldLogFile.setText(result);
        }
    }

    void onModifyLogFle() {
        String text = fieldLogFile.getText();
        if (text.trim().isEmpty()) {
            decorationLogFile.setImage(DECORATION_ERROR.getImage());
            setCompleted(false);
        } else {
            decorationLogFile.setImage(DECORATION_REQUIRED.getImage());
            setCompleted(true);
        }
    }

    private void setCompleted(boolean completed) {
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(completed);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        setCompleted(fieldLogFile.getText().isEmpty() == false);
        decorationLogFile.setImage(DECORATION_REQUIRED.getImage());
        fieldLogContents.getTextWidget().setFocus();
    }

    @Override
    protected void okPressed() {
        this.resultFile = new File(fieldLogFile.getText());
        if (resultFile.exists() == false) {
            boolean create = MessageDialog.openConfirm(
                    getShell(),
                    "New Log File",
                    "Log file does not exist. Do you create it?");
            if (create == false) {
                return;
            }
        }
        super.okPressed();
    }

    /**
     * Returns the contents of
     * @return the resultContents
     */
    public List<String> getResultContents() {
        return documentToLines(document);
    }

    /**
     * Returns the target log file.
     * @return the target log file
     */
    public File getResultFile() {
        return resultFile;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Code Reading");
    }

    private List<String> documentToLines(IDocument doc) {
        try {
            return DocumentUtils.getLines(doc, new Region(0, doc.getLength()));
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IDocument linesToDocument(List<String> lines) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0, n = lines.size(); i < n; i++) {
            if (i != 0) {
                buf.append(String.format("%n")); //$NON-NLS-1$
            }
            buf.append(lines.get(i));
        }
        return new Document(buf.toString());
    }

    @Override
    public int open() {
        try {
            return super.open();
        } finally {
            dispose();
        }
    }

    private void dispose() {
        if (decorationLogFile != null) {
            decorationLogFile.dispose();
        }
    }
}
