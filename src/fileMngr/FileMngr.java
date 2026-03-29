package fileMngr;

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import extra.Extra;

public class FileMngr {
	 private static String data;
	 private static JTextField getPath;
	 public static String fl_data;
	 public static boolean is_opened = false;
	 
	 public static void fl_openPrj(JFrame frame) {
		 JDialog dialog = new JDialog(frame, "Открыть проект", true);
	        dialog.setSize(Extra.sc_getWidth()/2, Extra.sc_getHeight()/2);
	        dialog.setLocationRelativeTo(frame);

	        // Контент диалога
	        JPanel dialogPanel = new JPanel();
	        getPath = new JTextField("Путь");
	        JButton agree = new JButton("Agree");
	        
	        
	        getPath.setPreferredSize(new Dimension(500, 28));
	        
	        
	        agree.addActionListener(e -> fl_openFile(frame, dialog));
	        
	        
	        dialogPanel.add(getPath);
	        dialogPanel.add(agree);
	        dialog.add(dialogPanel);

	        dialog.setVisible(true);
	 }
	 
	 public static String readFile(File file) throws IOException {
	        StringBuilder sb = new StringBuilder();
	        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	                sb.append(line).append(System.lineSeparator());
	            }
	        }
	        return sb.toString();
	    }
	 
	 public static void writeFile(File file, String content) throws IOException {
	        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
	            bw.write(content);
	        }
	    }
	 
	 
	 public static void fl_createPrj(JFrame frame) {
	        // Создание диалога
	        JDialog dialog = new JDialog(frame, "Создать проект", true);
	        dialog.setSize(Extra.sc_getWidth()/2, Extra.sc_getHeight()/2);
	        dialog.setLocationRelativeTo(frame);

	        // Контент диалога
	        JPanel dialogPanel = new JPanel();
	        getPath = new JTextField("Путь");
	        JButton agree = new JButton("Agree");
	        
	        
	        getPath.setPreferredSize(new Dimension(500, 28));
	        
	        
	        agree.addActionListener(e -> fl_createFile());
	        
	        
	        dialogPanel.add(getPath);
	        dialogPanel.add(agree);
	        dialog.add(dialogPanel);

	        dialog.setVisible(true);
	    }
	
	 
	 private static void fl_createFile() {
		 data = getPath.getText();
		 File file = new File(data);

	        try {
	            if (file.createNewFile()) {
	                System.out.println("Файл успешно создан: " + file.getAbsolutePath());
	            } else {
	                System.out.println("Файл уже существует");
	            }
	        } catch (IOException e) {
	            System.err.println("Ошибка при создании файла: " + e.getMessage());
	        }
	 }
	 
	 
	 
	 private static void fl_openFile(JFrame frame, JDialog di) {
		 data = getPath.getText();
		 StringBuilder content = new StringBuilder();

	        try (BufferedReader reader = new BufferedReader(new FileReader(data))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                content.append(line).append("\n"); // Добавляем строку и перенос
	            }
	        } catch (IOException e) {
	            System.err.println("Ошибка: " + e.getMessage());
	            return;
	        }
	        
	        fl_data = content.toString();
	        
	        //System.out.print(fl_data);
	        cd_open(frame);
	        di.dispose();
	 }
	 
	 private static void cd_open(JFrame frame) {
		 	//JPanel p = new JPanel();
		 	//p.setBackground(Color.darkGray);
			
		 	JTextArea code = new JTextArea(FileMngr.fl_data);
			code.setText(FileMngr.fl_data);
			code.setForeground(Color.magenta);
			code.setBackground(Color.black);
			code.setAutoscrolls(true);
			code.setCaretColor(Color.magenta);
			code.setFont(new Font("Arial", Font.PLAIN, 32));

			JScrollPane scrollPane = new JScrollPane(code);
			
			//p.add(code, BorderLayout.CENTER);
			frame.add(scrollPane, BorderLayout.CENTER);
			System.out.print("HI ONION");
 }
}
