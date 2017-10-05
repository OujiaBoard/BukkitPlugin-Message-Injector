package me.itzsomebody.messageinjector;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings("serial")
public class Injector extends JFrame {
	private JTextField field;
	private JTextField messagetoinject;
	
	public static void main(String[] args) {
        createGUI();
    }
	
	private static void createGUI() {
		SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
                catch (Exception ex) {}
                Injector injector = new Injector();
                injector.setTitle("Bukkit Message Injector");
                injector.setResizable(false);
                injector.setSize(440, 155);
                injector.setLocationRelativeTo(null);
                injector.setDefaultCloseOperation(3);
                injector.getContentPane().setLayout(new FlowLayout());
                JLabel label = new JLabel("Select File:");
                JLabel label2 = new JLabel("Inject Msg: ");
                injector.field = new JTextField();
                injector.field.setEditable(true);
                injector.field.setColumns(30);
                injector.messagetoinject = new JTextField();
                injector.messagetoinject.setEditable(true);
                injector.messagetoinject.setColumns(18);
                JButton selectButton = new JButton("Select");
                selectButton.setToolTipText("Select jar file");
                selectButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser chooser = new JFileChooser();
                        if (injector.field.getText() != null && !injector.field.getText().isEmpty()) {
                            chooser.setSelectedFile(new File(injector.field.getText()));
                        }
                        chooser.setMultiSelectionEnabled(false);
                        chooser.setFileSelectionMode(0);
                        int result = chooser.showOpenDialog(injector);
                        if (result == 0) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    injector.field.setText(chooser.getSelectedFile().getAbsolutePath());
                                }
                            });
                        }
                    }
                });
                JButton startButton = new JButton("Start");
                startButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (injector.field.getText() == null || injector.field.getText().isEmpty() || !injector.field.getText().endsWith(".jar")) {
                            JOptionPane.showMessageDialog(null, "You must select a valid jar file!", "Error", 0);
                            return;
                        }
                        if (injector.messagetoinject.getText() == null || injector.messagetoinject.getText().isEmpty()) {
                        	JOptionPane.showMessageDialog(null, "You must enter a message to be injected!", "Error", 0);
                        	return;
                        }
                        File output = null;
                        try {
                            File input = new File(injector.field.getText());
                            if (!input.getName().endsWith(".jar")) {
                                throw new IllegalArgumentException("File must be a jar.");
                            }
                            if (!input.exists()) {
                                throw new FileNotFoundException("The file " + input.getName() + " doesn't exist.");
                            }
                            output = new File(String.format("%s-Output.jar", input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf("."))));
                            if (output.exists()) {
                                output.delete();
                            }
                            process(input, output, injector.messagetoinject.getText());
                            checkFile(output);
                            JOptionPane.showMessageDialog(null, "Done: " + output.getAbsolutePath(), "Done", 1);
                        }
                        catch (Throwable t) {
                            JOptionPane.showMessageDialog(null, t, "Error", 0);
                            t.printStackTrace();
                            if (output != null) {
                                output.delete();
                            }
                        }
                        finally {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    injector.field.setText("");
                                    injector.messagetoinject.setText("");
                                }
                            });
                        }
                    }
                });
                JPanel panel = new JPanel(new FlowLayout());
                panel.add(label);
                panel.add(injector.field);
                panel.add(selectButton);
                JPanel panel2 = new JPanel(new FlowLayout());
                panel2.add(startButton);
                JPanel panel3 = new JPanel(new FlowLayout());
                panel3.add(label2);
                panel3.add(injector.messagetoinject);
                JPanel border = new JPanel(new BorderLayout());
                border.add(panel, "North");
                border.add(panel3, "Center");
                border.add(panel2, "South");
                injector.getContentPane().add(border);
                injector.setVisible(true);
            }
        });
	}
	
	private static void checkFile(File jarFile) throws Throwable {
        if (!jarFile.exists()) {
            throw new IllegalStateException("Output file not found.");
        }
    }
	
	private static void writeToFile(ZipOutputStream outputStream, InputStream inputStream) throws Throwable {
        byte[] buffer = new byte[4096];
        try {
            while (inputStream.available() > 0) {
                int data = inputStream.read(buffer);
                outputStream.write(buffer, 0, data);
            }
        }
        finally {
            inputStream.close();
            outputStream.closeEntry();
        }
    }
	
	private static void process(File jarFile, File outputFile, String message) throws Throwable {
        ZipFile zipFile = new ZipFile(jarFile);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFile));
        try {
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry)entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(in);
                        ClassNode classNode = new ClassNode();
                        cr.accept(classNode, 0);
                        
                        messageInjector(classNode, message);
                        
                        ClassWriter cw = new ClassWriter(0);
                        classNode.accept(cw);
                        ZipEntry newEntry = new ZipEntry(entry.getName());
                        newEntry.setTime(System.currentTimeMillis());
                        out.putNextEntry(newEntry);
                        writeToFile(out, new ByteArrayInputStream(cw.toByteArray()));
                    }
                }
                else {
                	entry.setTime(System.currentTimeMillis());
                    out.putNextEntry(entry);
                    writeToFile(out, zipFile.getInputStream(entry));
                }
            }
        }
        finally {
            zipFile.close();
            if (out != null) {
                out.close();
            }
        }
    }
	
	private static void messageInjector(ClassNode classNode, String message) {
		if (classNode.superName.equals("org/bukkit/plugin/java/JavaPlugin")) {
			for (MethodNode methodNode : classNode.methods) {
				if (methodNode.name.equals("onEnable")) {
					InsnList instructions = new InsnList();
					instructions.add(new MethodInsnNode(184, "org/bukkit/Bukkit", "getConsoleSender", "()Lorg/bukkit/command/ConsoleCommandSender;", false));
					instructions.add(new LdcInsnNode(message));
					instructions.add(new MethodInsnNode(185, "org/bukkit/command/ConsoleCommandSender", "sendMessage", "(Ljava/lang/String;)V", true));
					methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), instructions);
					return;
				}
			}
		}
	}
}
