import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.ExtractText;
import org.apache.pdfbox.io.IOUtils;

import com.csvreader.CsvWriter;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

/**
 * PDF to CSV file converter GUI program.
 * 
 * @author Miklos Kalozi
 */
@SuppressWarnings("serial")
public class PdfToCsvConverter extends JFrame implements ActionListener {
	private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");
	private static final int VID_COLUMN = 0, YEAR_COLUMN = 1, MAKE_COLUMN = 2, MODEL_COLUMN = 3;
	private static final int TRIM_COLUMN = 4, VIN_COLUMN = 5, VALUE_COLUMN = 6;
	
	private JTextField pdfFileField = new JTextField(70), csvFileField = new JTextField(70);
	private JButton pdfFileChooserButton = GUIUtils.createButton("Browse", this), csvFileChooserButton = GUIUtils.createButton("Browse", this);
	private JButton convertButton = GUIUtils.createButton("Convert", this);
	private JFileChooser pdfFileChooser = GUIUtils.createFileChooser("PDF file", JFileChooser.FILES_ONLY, "pdf");
	private JFileChooser csvFileChooser = GUIUtils.createFileChooser("CSV file", JFileChooser.FILES_ONLY, "csv", "txt");
	private JProgressBar progressBar = new JProgressBar();
	private JTextArea log = new JTextArea();
	{
		convertButton.setFont(convertButton.getFont().deriveFont(Font.BOLD, 16));
		log.setEditable(false);
		log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		
//		pdfFileChooser.setCurrentDirectory(new File("files/input"));
//		csvFileChooser.setCurrentDirectory(new File("files/output"));
	}
	private boolean isCancelled;
	
	public PdfToCsvConverter() {
		super("PDF to CSV Converter");
		setLayout(new MigLayout("insets 2 2 2 2, center, wrap 1", "[grow]"));
		add(GUIUtils.createFileChooserPanel("insets 0 0 0 0 | [] [grow] []", GUIUtils.createLabel("PDF file"), "w 38!", pdfFileField, pdfFileChooserButton, ""), "grow");
		add(GUIUtils.createFileChooserPanel("insets 0 0 0 0 | [] [grow] []", GUIUtils.createLabel("CSV file"), "w 38!", csvFileField, csvFileChooserButton, ""), "grow");
		add(convertButton, "center, w 100!, h 60!");
		add(progressBar, "center, growx");
		add(new JScrollPane(log), "center, w 100%, h 100%");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == pdfFileChooserButton) {
			if (pdfFileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
				pdfFileField.setText(pdfFileChooser.getSelectedFile().getPath());
			}
		} else if (e.getSource() == csvFileChooserButton) {
			if (csvFileChooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
				File file = csvFileChooser.getSelectedFile();
				String filename = file.getPath();
				if (!file.exists() || file.isDirectory()) {
					String name = filename.toLowerCase();
					if (!name.endsWith(".csv") && !name.endsWith(".txt")) {
						String desc = csvFileChooser.getFileFilter().getDescription().toLowerCase();
						if (desc.contains("csv")) {
							filename += ".csv";
						} else if (desc.contains("txt")) {
							filename += ".txt";
						}
					}
				}
				csvFileField.setText(filename);
			}
		} else if (e.getSource() == convertButton) {
			if (convertButton.getText().equals("Cancel")) {
				isCancelled = true;
				enableFields(true);
				return;
			}
			final File pdfFile = new File(pdfFileField.getText().trim());
			if (!pdfFile.exists() || pdfFile.isDirectory()) {
				JOptionPane.showMessageDialog(this, "File '" + pdfFile.getPath() + "' does not exists!", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			new Thread() {
				public void run() {
					try {
						enableFields(false);
						File csvFile = new File(csvFileField.getText().trim());
						if (csvFile.getParentFile() != null) {
							FileUtils.forceMkdir(csvFile.getParentFile());
						}
						progressBar.setIndeterminate(true);
						convertButton.setText("Cancel");
						logInfo("Reading file '" + pdfFile.getPath() + "' started");
						List<String> pages = listPages(pdfFile);
						logInfo("Reading file finished");
						logInfo("Writing file '" + csvFile.getPath() + "' started");
						CsvWriter writer = new CsvWriter(new FileWriter(csvFile), ',');
						writer.writeRecord(new String[] {"VID", "Year", "Make", "Model", "Trim", "VIN", "Value"});
						String[] record = new String[7];
						int recordCount = 0;
						for (String page : pages) {
							if (isCancelled) {
								isCancelled = false;
								String msg = "Process has been cancelled";
								logWarn(msg);
								JOptionPane.showMessageDialog(PdfToCsvConverter.this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
								enableFields(true);
								return;
							}
							BufferedReader reader = new BufferedReader(new StringReader(page));
							for (String line = reader.readLine(); line != null; line = reader.readLine()) {
								if (isCancelled) {
									isCancelled = false;
									String msg = "Process has been cancelled";
									logWarn(msg);
									JOptionPane.showMessageDialog(PdfToCsvConverter.this, msg, "Warning", JOptionPane.WARNING_MESSAGE);
									enableFields(true);
									return;
								}
								if (line.startsWith("AUTOMOBILES")) {
									record[MAKE_COLUMN] = clean(line.substring("AUTOMOBILES".length()));
								} else if (line.startsWith("LIGHT DUTY TRUCKS")) {
									record[MAKE_COLUMN] = clean(line.substring("LIGHT DUTY TRUCKS".length()));
								} else if (line.startsWith("MOTORCYCLES")) {
									record[MAKE_COLUMN] = clean(line.substring("MOTORCYCLES".length()));
								} else if (line.startsWith("-")) {
									reader.readLine();
									reader.readLine();
									String model = clean(reader.readLine());
									if (model.endsWith("continued")) {
										model = model.substring(0, model.length() - "continued".length()).trim();
									}
									record[MODEL_COLUMN] = model;
								} else if (line.startsWith("_")) {
									record[MODEL_COLUMN] = clean(reader.readLine());
								} else {
									String year = clean(line.substring(0, 2));
									if (year.length() > 0) {
										try {
											Integer.parseInt(year);
											record[YEAR_COLUMN] = year;
										} catch (Exception e) {
											record[MODEL_COLUMN] = line;
											continue;
										}
									}
									record[TRIM_COLUMN] = clean(line.substring(3, 19));
									record[VIN_COLUMN] = clean(line.substring(19, 30));
									record[VID_COLUMN] = clean(line.substring(30, 36));
									record[VALUE_COLUMN] = clean(line.substring(36));
									writer.writeRecord(record);
									recordCount++;
								}
							}
							reader.close();
						}
						writer.flush();
						writer.close();
						logInfo("Writing file finished, " + recordCount + " record" + (recordCount <= 1 ? " has" : "s have")+ " been written");
						JOptionPane.showMessageDialog(
								PdfToCsvConverter.this, 
								"File '" + csvFile.getPath() + "' has been created, " + recordCount + " record" + (recordCount <= 1 ? " has" : "s have")+ " been written.", 
								"Info", 
								JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception e) {
						e.printStackTrace();
						String msg = "Error converting file '" + pdfFile.getPath() + "': " + e.getLocalizedMessage();
						logError(msg);
						JOptionPane.showMessageDialog(PdfToCsvConverter.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
					} finally {
						progressBar.setIndeterminate(false);
						convertButton.setText("Convert");
						enableFields(true);
					}
				}
			}.start();
		}
	}
	
	private String clean(String text) {
		int i = 0, j = text.length() - 1;
//		while (i < text.length() && (Character.isWhitespace(text.charAt(i)) || text.charAt(i) == '-')) {
//			i++;
//		}
//		while (j > i && (Character.isWhitespace(text.charAt(j)) || text.charAt(j) == '-')) {
//			j--;
//		}
		return text.substring(i, j + 1).trim();
	}
	
	private void enableFields(boolean b) {
		pdfFileField.setEnabled(b);
		csvFileField.setEnabled(b);
		pdfFileChooserButton.setEnabled(b);
		csvFileChooserButton.setEnabled(b);
	}
	
	private void logInfo(String text) {
		log("[INFO] " + text);
	}
	
	private void logError(String text) {
		log("[ERROR] " + text);
	}
	
	private void logWarn(String text) {
		log("[WARNING] " + text);
	}
	
	private void log(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String logText = LOG_DATE_FORMAT.format(new Date()) + " " + text;
				if (log.getText().length() > 0) {
					log.append("\n");
				}
				log.append(logText);
				log.setCaretPosition(log.getText().length() - logText.length());
			}
		});
	}
	
	private static List<String> listPages(File pdfFile) throws Exception {
		File temp = File.createTempFile(Long.toString(System.nanoTime()), ".txt");
		BufferedReader reader = null;
		try {
			ExtractText.main(new String[] {"-nonSeq", pdfFile.getPath(), temp.getPath()});
			List<String> pages = new ArrayList<String>();
			StringBuilder page = new StringBuilder();
			reader = new BufferedReader(new FileReader(temp));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				int nextPage = pages.size() + 1;
				if (line.contains("2017 TAVT ASSESSMENT MANUAL")) {
					for (line = reader.readLine(); line != null && !line.matches("\\s*" + (nextPage + 1) + "\\s*"); line = reader.readLine()) {
						if (line.trim().length() > 0) {
							page.append("\n").append(line);
						}
					}
					pages.add(page.toString().trim());
					page.delete(0, page.length());
				}
			}
			return pages;
		} finally {
			IOUtils.closeQuietly(reader);
			FileUtils.deleteQuietly(temp);
		}
	}
	
	private static void createAndShowGUI() {
		JFrame frame = new PdfToCsvConverter();
		frame.setIconImage(GUIUtils.loadImage("icon.png"));
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.setSize(600, 300);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	public static void main(String[] args) {
		GUIUtils.checkVersion(8);
		try {
			UIManager.setLookAndFeel(new WindowsLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			JOptionPane.showMessageDialog(null, "Error setting Windows Look And Feel: " + e.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		createAndShowGUI();
	}

}
