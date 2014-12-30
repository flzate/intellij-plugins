package org.jetbrains.training;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;

/**
 * Created by karashevich on 17/12/14.
 */
public class StartLesson extends AnAction {

    private boolean isRecording = false;


    public void actionPerformed(final AnActionEvent e) {

        try {

            Messages.showMessageDialog(e.getProject(), "Welcome to IntelliJ IDEA training course.", "Information", Messages.getInformationIcon());

            final VirtualFile vf;
            vf = createFile(e.getProject());

            OpenFileDescriptor descriptor = new OpenFileDescriptor(e.getProject(), vf);
            final Editor editor = FileEditorManager.getInstance(e.getProject()).openTextEditor(descriptor, true);
            Document document = editor.getDocument();

            InputStream is = this.getClass().getResourceAsStream("JavaLessonExample2.java");
            final String target = new Scanner(is).useDelimiter("\\Z").next();
            final ActionsRecorder recorder = new ActionsRecorder(e.getProject(), document, target);

            isRecording = true;
            Disposer.register(recorder, new Disposable() {
                @Override
                public void dispose() {
                    isRecording = false;
                }
            });

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    recorder.startRecording();
                    final Editor editor1 = FileEditorManager.getInstance(e.getProject()).openTextEditor(new OpenFileDescriptor(e.getProject(), vf), true);

                    final String newScript = "package org.jetbrains.training;\n" +
                            "\n" +
                            "/**\n" +
                            " * Created by jetbrains on 17/12/14.\n" +
                            " */\n" +
                            "public class JavaLessonExample2 {\n" +
                            "\n" +
                            "\tprivate String privateString;\n" +
                            "\n" +
                            "\tpublic JavaLessonExample2() {\n" +
                            "\n" +
                            "\t\tprivateString = \"defined\";\n" +
                            "\n" +
                            "\t}\n" +
                            "\n" +
                            "\tpublic String getPrivateString() {\n" +
                            "\t\treturn privateString;\n" +
                            "\t}\n" +
                            "\n" +
                            "\tpublic void setPrivateString(String privateString) {\n" +
                            "\t\tthis.privateString = privateString;\n" +
                            "\t}\n" +
                            "}";

                    final Thread roboThread = new Thread("RoboThread") {
                        @Override
                        public void run() {
                            try {
                                boolean isTyping = true;
                                final int[] i = {0};

                                while (isTyping) {
                                    Thread.sleep(30);
                                    final int finalI = i[0];
                                    WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
                                        @Override
                                        public void run() {
                                            editor1.getDocument().insertString(finalI, newScript.subSequence(i[0], i[0] + 1));
                                            editor1.getCaretModel().moveToOffset(finalI + 1);
                                        }
                                    });
                                    i[0]++;
                                    if (i[0] == newScript.length()) {
                                        isTyping = false;
                                    }
                                    if (i[0] == 20) {
                                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                showBalloon(e, editor1, editor);
                                            }

                                        });
                                        synchronized (editor) {
                                            editor.wait();
                                        }
                                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                performAction("EditorDuplicate", editor);
                                            }
                                        });
                                        synchronized (editor) {
                                            editor.wait();
                                        }

                                    }
                                }
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    };
                    roboThread.start();



                }
            });


        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     *
     * @param e
     * @param editor - editor where to show balloon
     * @param lockEditor - using for suspending typing robot until balloon will have been hidden
     */
    private void showBalloon(AnActionEvent e, Editor editor, final Editor lockEditor) {
        FileEditorManager instance = FileEditorManager.getInstance(e.getProject());
        if (instance == null) return;
        if (editor == null) return;

        int offset = editor.getCaretModel().getCurrentCaret().getOffset();
        VisualPosition position = editor.offsetToVisualPosition(offset);
        Point point = editor.visualPositionToXY(position);
        BalloonBuilder builder =
                JBPopupFactory.getInstance().
                        createHtmlTextBalloonBuilder("<html>Type <b>CMD + D</b> to duplicate current string.", null, Color.LIGHT_GRAY, null)
                        .setHideOnClickOutside(false).setCloseButtonEnabled(true).setHideOnKeyOutside(false);
        Balloon myBalloon = builder.createBalloon();

        myBalloon.show(new RelativePoint(editor.getContentComponent(), point), Balloon.Position.above);

        myBalloon.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(LightweightWindowEvent lightweightWindowEvent) {

            }

            @Override
            public void onClosed(LightweightWindowEvent lightweightWindowEvent) {
                synchronized (lockEditor){
                    lockEditor.notify();
                }
            }
        });

    }

    /**
     * performing internal platform action
      * @param actionName - name of IntelliJ Action. For full list please see http://git.jetbrains.org/?p=idea/community.git;a=blob;f=platform/platform-api/src/com/intellij/openapi/actionSystem/IdeActions.java;hb=HEAD
     * @param lockEditor - using for suspending typing robot until this action will be performed
     */
    private void performAction(String actionName, final Editor lockEditor){
        final ActionManager am = ActionManager.getInstance();
        final AnAction targetAction = am.getAction(actionName);
        final InputEvent inputEvent = getInputEvent(actionName);

        am.tryToExecute(targetAction, inputEvent, null, null, false).doWhenDone(new Runnable() {
            @Override
            public void run() {
                synchronized (lockEditor){
                    lockEditor.notify();
                }
            }
        });

    }

    /**
     * Some util method for <i>performAction</i> method
     * @param actionName - please see it in <i>performAction</i> method
     * @return
     */
    public static InputEvent getInputEvent(String actionName) {
        final Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionName);
        KeyStroke keyStroke = null;
        for (Shortcut each : shortcuts) {
            if (each instanceof KeyboardShortcut) {
                keyStroke = ((KeyboardShortcut)each).getFirstKeyStroke();
                if (keyStroke != null) break;
            }
        }

        if (keyStroke != null) {
            return new KeyEvent(JOptionPane.getRootFrame(),
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    keyStroke.getModifiers(),
                    keyStroke.getKeyCode(),
                    keyStroke.getKeyChar(),
                    KeyEvent.KEY_LOCATION_STANDARD);
        } else {
            return new MouseEvent(JOptionPane.getRootFrame(), MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 1, false, MouseEvent.BUTTON1);
        }


    }


    @Nullable
    private VirtualFile createFile(final Project project) throws IOException {
        final String fileName = "JavaLessonExampleEmpty.java";


        Module[] modules = ModuleManager.getInstance(project).getModules();
        final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(modules[0]).getSourceRoots();

        InputStream is = this.getClass().getResourceAsStream(fileName);
        //final String content = new Scanner(is).useDelimiter("\\Z").next();
        final String content = "";

        /*
        Creating new file in Project view;
        copying content from initial file "fileName"
         */

        return ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
            @Override
            @Nullable
            public VirtualFile compute() {
                try {
                    VirtualFile vFile = sourceRoots[0].createChildData(this, fileName);
                    VfsUtil.saveText(vFile, content);
                    return vFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });

    }


}
