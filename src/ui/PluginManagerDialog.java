package ui;

import plugin.PluginManager;
import plugin.IDEPlugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Диалог «Управление плагинами».
 * Позволяет включать/выключать каждый .jar‑плагин и удалять их.
 */
public final class PluginManagerDialog extends JDialog {

    private final JFrame owner;
    private final JMenuBar menuBar;
    private final JPanel listPanel = new JPanel();
    private final Map<Path, JCheckBox> checkMap = new HashMap<>();

    public PluginManagerDialog(JFrame owner, JMenuBar menuBar) {
        super(owner, "Управление плагинами", true);
        this.owner = owner;
        this.menuBar = menuBar;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createTitledBorder("Установленные плагины"));
        add(scroll, BorderLayout.CENTER);

        refreshList();

        /* ---------- Нижняя панель ---------- */
        JButton btnApply = new JButton("Применить");
        JButton btnClose = new JButton("Закрыть");

        btnApply.addActionListener(e -> applyChanges());
        btnClose.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnApply);
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Перезаполняет список чек‑боксовом представлением всех JAR‑ов */
    private void refreshList() {
        listPanel.removeAll();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(
                PluginManager.getPluginsDir(), "*.jar")) {
            for (Path jar : ds) {
                // Пытаемся узнать имя/описание плагина, не вызывая init()
                String pluginName = jar.getFileName().toString();   // fallback
                String pluginDesc = "";
                try {
                    ServiceLoader<IDEPlugin> sl = ServiceLoader.load(
                            IDEPlugin.class,
                            new URLClassLoader(new URL[]{jar.toUri().toURL()},
                                               PluginManager.class.getClassLoader()));
                    Iterator<IDEPlugin> it = sl.iterator();
                    if (it.hasNext()) {
                        IDEPlugin p = it.next();
                        pluginName = p.getName();
                        pluginDesc = p.getDescription();
                    }
                } catch (Exception ignored) {}

                boolean enabled = !PluginManager.isJarDisabled(jar);
                JCheckBox cb = new JCheckBox(pluginName, enabled);
                cb.setToolTipText("<html><b>Файл:</b> " + jar.getFileName()
                        + "<br><b>Описание:</b> " + pluginDesc + "</html>");
                checkMap.put(jar, cb);

                JPanel row = new JPanel(new BorderLayout(5, 5));
                row.add(cb, BorderLayout.WEST);

                // ---------- Кнопка «Удалить» ----------
                JButton del = new JButton("Удалить");
                del.setMargin(new Insets(2, 5, 2, 5));
                del.addActionListener(ev -> deletePlugin(jar));
                row.add(del, BorderLayout.EAST);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

                listPanel.add(row);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось прочитать каталог плагинов:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    /** Удаляем JAR‑файл плагина (с подтверждением) */
    private void deletePlugin(Path jar) {
        int ans = JOptionPane.showConfirmDialog(this,
                "Точно удалить плагин «" + jar.getFileName() + "»?\nФайл будет безвозвратно удалён.",
                "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        // Снимаем галочку, если она была установлена
        JCheckBox cb = checkMap.get(jar);
        if (cb != null) cb.setSelected(false);

        try {
            Files.deleteIfExists(jar);
            // Удаляем из списка отключённых, если там был
            PluginManager.setJarEnabled(jar, true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось удалить файл:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Обновляем UI
        refreshList();
    }

    /** Сохраняем новые состояния (вкл/выкл) и перезагружаем все плагины */
    private void applyChanges() {
        // 1️⃣ Сохраняем включено/отключено
        checkMap.forEach((jar, cb) -> PluginManager.setJarEnabled(jar, cb.isSelected()));

        // 2️⃣ Перезагружаем плагины (это уже есть в Main → пункт «Перезагрузить все»)
        PluginManager.loadAllPlugins(owner, menuBar);

        // 3️⃣ Информируем пользователя (перезагрузка уже произведена)
        JOptionPane.showMessageDialog(this,
                "Плагины перезагружены.\nНекоторые изменения могут стать видимыми только после закрытия/открытия окна.",
                "Готово", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
