import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;


public class GUIUtils {

	public static JLabel createLabel(String text) {
		return new JLabel(text);
	}
	
	public static JButton createButton(String text, ActionListener a) {
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.addActionListener(a);
		return button;
	}

	public static JCheckBox createCheckBox(String text, ActionListener a) {
		JCheckBox checkBox = new JCheckBox(text);
		checkBox.addActionListener(a);
		checkBox.setFocusable(false);
		return checkBox;
	}

	public static JRadioButton createRadioButton(String text, ActionListener a, ButtonGroup group) {
		JRadioButton button = new JRadioButton(text);
		button.addActionListener(a);
		button.setFocusable(false);
		group.add(button);
		return button;
	}
	
	public static <T> JComboBox<T> createComboBox(ActionListener a) {
		JComboBox<T> comboBox = new JComboBox<T>();
		comboBox.addActionListener(a);
		comboBox.setFocusable(false);
		return comboBox;
	}

	public static <T> JComboBox<T> createComboBox(ActionListener a, T[] items) {
		JComboBox<T> comboBox = createComboBox(a);
		for (int i = 0; i < items.length; i++) {
			comboBox.addItem(items[i]);
		}
		return comboBox;
	}
	
	public static JFileChooser createFileChooser(String dialogTitle, int fileSelectionMode, String...filterSuffixes) {
		return createFileChooser(dialogTitle, fileSelectionMode, false, filterSuffixes);
	}

	public static JFileChooser createFileChooser(String dialogTitle, int fileSelectionMode, boolean multiSelection, String...filterSuffixes) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(dialogTitle);
		fileChooser.setFileSelectionMode(fileSelectionMode);
		fileChooser.setMultiSelectionEnabled(multiSelection);
		for (int i = filterSuffixes.length - 1; i >= 0; i--) {
			final String filterSuffix = filterSuffixes[i];
			fileChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory() || file.getName().toLowerCase().endsWith("." + filterSuffix.toLowerCase());
				}
				@Override
				public String getDescription() {
					return filterSuffix.toUpperCase() + " files";
				}
			});
		}
		return fileChooser;
	}

	public static JPanel createButtonPanel(String panelConstraints, int gap, int width, int height, JButton...buttons) {
		StringBuilder columnConstraints = new StringBuilder();
		for (int i = 0; i < buttons.length; i++) {
			if (columnConstraints.length() > 0) {
				columnConstraints.append(" ").append(gap).append(" ");
			}
			columnConstraints.append("[]");
		}
		JPanel panel = new JPanel(new MigLayout(panelConstraints, columnConstraints.toString()));
		String buttonConstraints = "w " + width + "!, h " + height + "!";
		for (JButton button : buttons) {
			panel.add(button, buttonConstraints);
		}
		return panel;
	}

	public static JPanel createLabeledComponent(String panelConstraints, JLabel label, String labelConstraints, JComponent component, String componentConstraints) {
		return createLabeledComponent(panelConstraints, label, labelConstraints, null, component, componentConstraints);
	}

	public static JPanel createLabeledComponent(String panelConstraints, JLabel label, String labelConstraints, JLabel separator, JComponent component, String componentConstraints) {
		String[] split = panelConstraints.split(Pattern.quote("|"));
		JPanel panel = new JPanel(new MigLayout(split[0], split.length > 1 ? split[1] : "", split.length > 2 ? split[2] : ""));
		panel.add(label, labelConstraints);
		if (separator != null) {
			panel.add(separator);
		}
		panel.add(component, componentConstraints);
		return panel;
	}

	public static JPanel createFileChooserPanel(String panelConstraints, JLabel label, String labelConstraints, JTextField textField, JButton button, String buttonConstraints) {
		return createFileChooserPanel(panelConstraints, label, labelConstraints, null, textField, button, buttonConstraints);
	}

	public static JPanel createFileChooserPanel(String panelConstraints, JLabel label, String labelConstraints, JLabel separator, JTextField textField, JButton button, String buttonConstraints) {
		String[] split = panelConstraints.split(Pattern.quote("|"));
		JPanel panel = new JPanel(new MigLayout(split[0], split.length > 1 ? split[1] : "", split.length > 2 ? split[2] : ""));
		panel.add(label, labelConstraints);
		if (separator != null) {
			panel.add(separator);
		}
		panel.add(textField, "growx");
		panel.add(button, buttonConstraints);
		return panel;
	}

	public static void checkVersion(int recommendedVersion) {
		String version = System.getProperty("java.version");
		for (int i = 1; i < recommendedVersion; i++) {
			if (version.startsWith("1." + i)) {
				JOptionPane.showMessageDialog(null, "Minimimum recommended Java version is 1.8, please update your Java!", "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(-1);
			}
		}
	}

	public static BufferedImage loadImage(String filename) {
		try {
			return ImageIO.read(ClassLoader.getSystemResourceAsStream(filename));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}