/*
 * Copyright (C) 2026 Fedotov Vladislav Igorevich (niktoonion)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package ui;

import extra.WorkspaceManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class FileTreePanel extends JPanel {

    private final JTree tree;
    private final DefaultTreeModel model;
    private final Consumer<File> fileOpenHandler;

    public FileTreePanel(Consumer<File> fileOpenHandler) {
        this.fileOpenHandler = fileOpenHandler;
        setLayout(new BorderLayout());

        // корень – текущий workspace
        File rootFile = WorkspaceManager.getWorkspaceDir().toFile();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileNode(rootFile));
        model = new DefaultTreeModel(rootNode);
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new FileTreeCellRenderer());

        populateNode(rootNode);
        tree.expandRow(0);

        /* ---------- Мыши ---------- */
        tree.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // двойной клик – открыть файл
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    FileNode fn = (FileNode) node.getUserObject();
                    if (fn.file != null && fn.file.isFile()) {
                        fileOpenHandler.accept(fn.file);
                    }
                }
                // правый клик – контекстное меню
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    if (row == -1) return;
                    tree.setSelectionRow(row);
                    showContextMenu(e.getX(), e.getY());
                }
            }
        });

        /* ---------- Горячие клавиши на дереве ---------- */
        InputMap im = tree.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = tree.getActionMap();

        // Del – удалить выбранный файл/папку
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { deleteNode(); }
        });

        // F2 – переименовать
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
        am.put("rename", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { renameNode(); }
        });

        // Ctrl+N – новый файл
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newFile");
        am.put("newFile", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { createNewFile(); }
        });

        // Ctrl+Shift+N – новая папка
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "newFolder");
        am.put("newFolder", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { createNewFolder(); }
        });

        /* ---------- lazy‑loading ---------- */
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                if (node.getChildCount() == 1) {
                    DefaultMutableTreeNode first = (DefaultMutableTreeNode) node.getChildAt(0);
                    FileNode fn = (FileNode) first.getUserObject();
                    if (fn.file == null) {
                        node.removeAllChildren();
                        populateNode(node);
                        model.reload(node);
                    }
                }
            }
            @Override public void treeWillCollapse(TreeExpansionEvent event) {}
        });

        JScrollPane scroll = new JScrollPane(tree);
        add(scroll, BorderLayout.CENTER);
    }

    /* ---------- Заполнение узла ---------- */
    private void populateNode(DefaultMutableTreeNode node) {
        FileNode fn = (FileNode) node.getUserObject();
        File dir = fn.file;
        if (dir == null || !dir.isDirectory()) return;

        File[] children = dir.listFiles();
        if (children == null) return;

        Arrays.sort(children, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File child : children) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(child));
            node.add(childNode);
            if (child.isDirectory()) {
                childNode.add(new DefaultMutableTreeNode(new FileNode(null)));
            }
        }
    }

    /* ---------- Контекстное меню ---------- */
    private void showContextMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem miNewFile   = new JMenuItem("Новый файл");
        JMenuItem miNewFolder = new JMenuItem("Новая папка");
        JMenuItem miRename    = new JMenuItem("Переименовать");
        JMenuItem miDelete    = new JMenuItem("Удалить");
        JMenuItem miRefresh   = new JMenuItem("Обновить");
        JMenuItem miSwitchWs  = new JMenuItem("Сменить workspace…");

        miNewFile.addActionListener(e -> createNewFile());
        miNewFolder.addActionListener(e -> createNewFolder());
        miRename.addActionListener(e -> renameNode());
        miDelete.addActionListener(e -> deleteNode());
        miRefresh.addActionListener(e -> refreshNode());
        miSwitchWs.addActionListener(e -> switchWorkspaceDialog());

        menu.add(miNewFile);
        menu.add(miNewFolder);
        menu.addSeparator();
        menu.add(miRename);
        menu.add(miDelete);
        menu.addSeparator();
        menu.add(miRefresh);
        menu.addSeparator();
        menu.add(miSwitchWs);

        menu.show(tree, x, y);
    }

    /* ---------- Операции над узлом ---------- */
    private DefaultMutableTreeNode getSelectedNode() {
        TreePath tp = tree.getSelectionPath();
        return (tp == null) ? null : (DefaultMutableTreeNode) tp.getLastPathComponent();
    }

    private void createNewFile() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        FileNode fn = (FileNode) node.getUserObject();
        File parent = fn.file.isDirectory() ? fn.file : fn.file.getParentFile();

        String name = JOptionPane.showInputDialog(this,
                "Имя нового файла:", "Создать файл", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        File newFile = new File(parent, name);
        try {
            if (newFile.createNewFile()) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(new FileNode(newFile));
                node.add(child);
                model.nodesWereInserted(node, new int[]{node.getIndex(child)});
                fileOpenHandler.accept(newFile);
            } else {
                JOptionPane.showMessageDialog(this, "Файл уже существует",
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось создать файл:\n" + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewFolder() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        FileNode fn = (FileNode) node.getUserObject();
        File parent = fn.file.isDirectory() ? fn.file : fn.file.getParentFile();

        String name = JOptionPane.showInputDialog(this,
                "Имя новой папки:", "Создать папку", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        File newDir = new File(parent, name);
        if (newDir.mkdir()) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(new FileNode(newDir));
            child.add(new DefaultMutableTreeNode(new FileNode(null)));
            node.add(child);
            model.nodesWereInserted(node, new int[]{node.getIndex(child)});
        } else {
            JOptionPane.showMessageDialog(this, "Не удалось создать папку",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renameNode() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        FileNode fn = (FileNode) node.getUserObject();
        File file = fn.file;

        String newName = JOptionPane.showInputDialog(this,
                "Новое имя:", "Переименовать", JOptionPane.PLAIN_MESSAGE);
        if (newName == null || newName.trim().isEmpty()) return;

        File dest = new File(file.getParentFile(), newName);
        if (file.renameTo(dest)) {
            fn.file = dest;
            model.nodeChanged(node);
        } else {
            JOptionPane.showMessageDialog(this, "Не удалось переименовать",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteNode() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        FileNode fn = (FileNode) node.getUserObject();
        File file = fn.file;

        int ans = JOptionPane.showConfirmDialog(this,
                "Удалить «" + file.getName() + "»?",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        if (deleteRecursively(file)) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent != null) {
                int idx = parent.getIndex(node);
                node.removeFromParent();
                model.nodesWereRemoved(parent, new int[]{idx}, new Object[]{node});
            }
        } else {
            JOptionPane.showMessageDialog(this, "Не удалось удалить",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (File kid : kids) {
                    if (!deleteRecursively(kid)) return false;
                }
            }
        }
        return f.delete();
    }

    private void refreshNode() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        node.removeAllChildren();
        populateNode(node);
        model.reload(node);
    }

    /** Обновить всё дерево (используется после смены workspace). */
    public void refreshRoot() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        populateNode(root);
        model.reload(root);
    }

    /* ---------- Смена workspace ---------- */
    private void switchWorkspaceDialog() {
        List<String> wsList = WorkspaceManager.listAllWorkspaces();
        wsList.add(0, "(Новый workspace)");
        String selected = (String) JOptionPane.showInputDialog(this,
                "Выберите workspace:", "Смена workspace",
                JOptionPane.PLAIN_MESSAGE, null,
                wsList.toArray(), wsList.get(0));
        if (selected == null) return;
        if ("(Новый workspace)".equals(selected)) {
            String name = JOptionPane.showInputDialog(this,
                    "Имя нового workspace:", "Новый workspace",
                    JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) return;
            Path newWs = WorkspaceManager.getWorkspaceDir().resolve(name);
            WorkspaceManager.setActiveWorkspace(newWs);
        } else {
            Path chosen = WorkspaceManager.getWorkspaceDir().resolve(selected);
            WorkspaceManager.setActiveWorkspace(chosen);
        }
        // после переключения – перерисовать дерево
        refreshRoot();
    }

    /* ---------- Внутренний класс-узел ---------- */
    private static final class FileNode {
        File file;
        FileNode(File f) { this.file = f; }
        @Override public String toString() {
            return (file == null) ? "" : file.getName();
        }
    }

    /* ---------- Рендерер ---------- */
    private static final class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private final Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
        private final Icon fileIcon   = UIManager.getIcon("FileView.fileIcon");

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                    boolean sel, boolean expanded,
                                                    boolean leaf, int row,
                                                    boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                    leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            FileNode fn = (FileNode) node.getUserObject();
            if (fn.file != null) {
                setText(fn.file.getName());
                setIcon(fn.file.isDirectory() ? folderIcon : fileIcon);
            } else {
                setText("");
                setIcon(null);
            }
            return this;
        }
    }
}
