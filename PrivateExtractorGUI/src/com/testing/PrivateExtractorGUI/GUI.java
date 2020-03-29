package com.testing.PrivateExtractorGUI;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;

import com.google.gson.Gson;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;
import de.nixosoft.jlr.JLROpener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Set;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.awt.event.ActionEvent;

/*
 * I am very sorry if you have to modify this code.
 * I have not touched Java in probably 4-5 years...
 * Comments were added wherever possible, but good luck.
 */
public class GUI {
	public static ArrayList<String> log = new ArrayList<String>();
	public static Point[] imported = new Point[0];
	public static Point[] infected = new Point[0];
	public static PrivateExtractorUI ui;

	public static enum RISK {
		GREEN, YELLOW, RED
	}

	public static void main(String[] args) {
		ui = new PrivateExtractorUI();
	}

	public static void importFile(String filename) {
		Gson gson = new Gson();
		try (Reader reader = new FileReader(filename)) {
			// Convert JSON File to Java Object
			Point[] raw = gson.fromJson(reader, Point[].class);
			Point[] dedup = GUI.removeDuplicates(raw);
			Arrays.sort(dedup);
			imported = dedup;
			ui.updateStatus("Successfully imported " + raw.length + " records. (" + (raw.length - dedup.length)
					+ " duplicates)");

			GUI.fetchInfectedFile();
			GUI.generateReport();
		} catch (IOException e) {
			ui.updateStatus("Error importing file");
			GUI.generateReport();
		}

		// Reset after completion.
		log = new ArrayList<String>();
		ui.reset();
	}

	private static void importInfectedFile(String file) {
		ui.updateStatus("Attempting to import " + file + " file");

		BufferedReader csvReader = null;
		try {
			csvReader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e2) {
			ui.updateStatus("Could not open " + file + " file");
		}

		// Positional location for data in CSV
		int latitude = 7;
		int longitude = 8;
		int[] dates = new int[] { 10, 11, 12 }; // date_onset_symptoms date_admission_hospital date_confirmation

		int[] excluded = new int[] { 0, 0, 0 };
		String row = "";

		ArrayList<Point> imported = new ArrayList<Point>();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.M.yyyy");
		try {
			while ((row = csvReader.readLine()) != null) {
				String[] data = row.split(",");

				if (data[0].equals("ID")) {
					for (int i = 0; i < data.length; i++) { // Pre-flight checking.
						if (data[i].equals("latitude") && i != latitude) {
							ui.updateStatus("CSV format for latitude changed. Auto fixing");
							latitude = i;
						} else if (data[i].equals("longtitude") && i != longitude) {
							ui.updateStatus("CSV format for latitude changed. Auto fixing");
							longitude = i;
						} else if (data[i].equals("date_onset_symptoms") && i != dates[0]) {
							ui.updateStatus("CSV format for date_onset_symptoms changed. Auto fixing");
							dates[0] = i;
						} else if (data[i].equals("date_admission_hospital") && i != dates[1]) {
							ui.updateStatus("CSV format for date_admission_hospital changed. Auto fixing");
							dates[1] = i;
						} else if (data[i].equals("date_confirmation") && i != dates[2]) {
							ui.updateStatus("CSV format for date_confirmation changed. Auto fixing");
							dates[2] = i;
						}
					}
					continue;
				} // This is the header.

				// More preflight per row.
				if (data.length <= latitude) {
					excluded[0] += 1;
					continue;
				}

				if (data.length <= longitude) {
					excluded[1] += 1;
					continue;
				}

				if (data.length <= dates[0] || data.length <= dates[1] || data.length <= dates[2]) {
					excluded[2] += 1;
					continue;
				}

				double lat = 0;
				double lon = 0;
				long date = 0;

				int offset = 0;
				int target = latitude;
				boolean done = false;

				while (!done) {
					if (target + 1 >= data.length) {
						done = true;
						excluded[0] += 1;
					} else {
						try {
							lat = Double.parseDouble(data[target]);
							if (lat != 1) {
								offset = target - latitude;
								done = true;
							} else {
								target = target + 1;
							} // Weird issue around wuhan_or_not_wuhan
						} catch (NumberFormatException wtf) {
							target = target + 1;
							done = false;
						}
					}
				}

				target = longitude + offset;
				done = false;

				while (!done) {
					if (target + 1 >= data.length) {
						done = true;
						excluded[1] += 1;
					} else {
						try {
							lon = Double.parseDouble(data[target]);
							if (lon != 1) {
								offset = target - longitude;
								done = true;
							} else {
								target = target + 1;
							}
						} catch (NumberFormatException wtf) {
							target = target + 1;
							done = false;
						}
					}
				}

				if ((lat == 0 && lon == 0) || (lat == 1 && lon == 1)) { // Some weird issues (also Cambridge?)
					continue;
				}

				if (lon == lat) {
					lon = Double.parseDouble(data[longitude + offset + 1]);
				}

				String raw;
				for (int i = 0; i < dates.length; i++) {
					if (dates[i] + offset >= data.length) {
						excluded[2] += 1;
						continue;
					}
					raw = data[dates[i] + offset];
					if (!raw.equals("")) {
						try {
							date = sdf.parse(raw.split("-")[0]).getTime(); // Some dates have dashes as a separator
							continue;
						} catch (ParseException e) {
							// Do nothing.
						}
					}
				}

				if (date == 0) {
					excluded[2] += 1;
					continue;
				}

				Point temp = new Point();
				temp.setLatitude(lat);
				temp.setLongitude(lon);
				temp.setTime(date);
				imported.add(temp);
			}
		} catch (IOException e1) {
			ui.updateStatus("Error importing data from " + file);
		}

		infected = imported.toArray(new Point[imported.size()]);
		ui.updateStatus("Excluded " + excluded[0] + " for latitude issues (missing or misformatted)");
		ui.updateStatus("Excluded " + excluded[1] + " for longitude issues (missing or misformatted)");
		ui.updateStatus("Excluded " + excluded[2] + " for date issues (missing or misformatted)");
		ui.updateStatus("Imported " + infected.length + " infected records");

		try {
			csvReader.close();
		} catch (IOException e) {
			ui.updateStatus("Error closing file " + file);
		}

	}

	protected static void fetchInfectedFile() {
		// Check if the file is already existing.
		// Download source
		// Import file into Point[] infected.
		String source = "https://raw.githubusercontent.com/beoutbreakprepared/nCoV2019/master/latest_data/latestdata.csv";
		String file = "infected.csv";

		ui.updateStatus("Checking for infection data");

		if (!ui.fileExists(file)) {
			ui.updateStatus("No " + file + " file found");

			ui.updateStatus("Attempting to download (Alpha)");
			downloadFile(file, source);
		} else {
			ui.updateStatus(file + " file exists. Delete file to update it");
		}

		importInfectedFile(file);
	}

	protected static void downloadFile(String file, String url) {
		try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			byte dataBuffer[] = new byte[1024];
			int bytesRead;
			int bytesTotal = 0;
			while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
				fileOutputStream.write(dataBuffer, 0, bytesRead);
				bytesTotal += bytesRead;
			}
			ui.updateStatus("Downloaded " + bytesTotal + " bytes to " + file);
		} catch (IOException e) {
			ui.updateStatus("Could not download " + file);
		}
	}

	public static Point[] removeDuplicates(Point[] input) {
		int end = input.length;
		Set<Point> set = new HashSet<Point>();

		for (int i = 0; i < end; i++) {
			set.add(input[i]);
		}

		int size = set.size();
		Point[] output = new Point[size];

		int i = 0;
		for (Point point : set)
			output[i++] = point;

		return output;
	}

	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public static String sanitizeLatex(String in) {
		return in.replace("\\", "/").replace("_", "\\textunderscore ");
	}

	public static void generateReport() {
		ui.updateStatus("Generating contact report PDF file");

		RISK minimum = RISK.GREEN;
		Date start = new Date();
		DateFormat fileDate = new SimpleDateFormat("dd-MMM-yyyy-HHmmss");

		File template = Paths.get("latex", "template.tex").toFile();
		File temp = Paths.get("latex", "output_" + fileDate.format(start) + ".tex").toFile();
		JLRConverter converter = new JLRConverter(Paths.get("latex").toFile());

		DateFormat headerDate = new SimpleDateFormat("dd MMM yyyy");
		converter.replace("date", headerDate.format(start));

		DateFormat gps = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
		ArrayList<ArrayList<String>> points = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < imported.length; i++) {
			ArrayList<String> line = new ArrayList<String>();
			RISK[] returned = getRiskProfile(imported[i], minimum);

			if (minimum == RISK.GREEN || minimum != RISK.RED || (minimum == RISK.YELLOW && returned[1] != RISK.GREEN)) {
				minimum = returned[1];
			}
			
			if (minimum != RISK.GREEN) {
				line.add(sanitizeLatex("" + imported[i].latitude));
				line.add(sanitizeLatex("" + imported[i].longitude));
			} else {
				line.add(sanitizeLatex("Private"));
				line.add(sanitizeLatex("Private"));
			}
			
			Date result = new Date(imported[i].time);
			line.add(sanitizeLatex(gps.format(result)));

			if (returned[1] != minimum) {
				// Risk changed.
				line.add(sanitizeLatex(returned[1].name() + " (" + minimum + ")"));
			} else {
				line.add(sanitizeLatex(returned[1].name()));
			}
			points.add(line);
		}

		converter.replace("points", points);

		ArrayList<String> sanitizedLog = new ArrayList<String>();
		for (int i = 0; i < log.size(); i++) {
			sanitizedLog.add(sanitizeLatex(log.get(i)));
		}
		converter.replace("log", sanitizedLog);
		try {
			converter.parse(template, temp);
		} catch (IOException e) {
			System.out.println(e);
		}

		//JLRGenerator pdfGen = new JLRGenerator();
		//pdfGen.deleteTempFiles(true, true, true);
		//File output = Paths.get("latex", "output").toFile();
		//File runnable = Paths.get("latex", "miktex", "texmfs", "install", "miktex", "bin", "x64").toFile();
		/*
			pdfGen.generate(temp, output, runnable);
		} catch (IOException e) {
			System.out.println(e);
		} */
		
		String runnable = Paths.get("latex", "miktex", "texmfs", "install", "miktex", "bin", "x64", "pdflatex.exe").toString();
		String auxDirectory = Paths.get("latex").toString();
		String tempPath = temp.getAbsolutePath();
		
		while(ui.launchProcessAndWaitForResponse(new String[]{runnable, tempPath, "-aux-directory=" + auxDirectory, "-interaction=nonstopmode"}, "Package longtable Warning: Table widths have changed. Rerun LaTeX.")) {
			System.out.println("Running pdflatex.");
		} // There is a weird latex error with longtable... just rerun it until it goes away.
		

		File pdf = new File("output_" + fileDate.format(start) + ".pdf");
		try {
			JLROpener.open(pdf);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private static Polygon generateGeometry(Point point) {
		// Return the risk profile from the point being provided.
		int diameter = 1610; // 1 mile ~ 1069.4 meters.

		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(64); // adjustable
		shapeFactory.setCentre(new Coordinate(point.latitude, point.longitude));
		// Length in meters of 1° of latitude = always 111.32 km
		shapeFactory.setWidth(diameter / 111320d);
		// Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
		shapeFactory.setHeight(diameter / (40075000 * Math.cos(Math.toRadians(point.latitude)) / 360));
		return shapeFactory.createEllipse();
	}

	private static RISK[] getRiskProfile(Point point, RISK minimum) {
		RISK after = null;
		long HOUR_TO_MILLIS = 3600000;
		Polygon imported = generateGeometry(point);

		/*
		 * Plot each point inside or outside the radius If no points coincide, return
		 * GREEN. If points do coincide, decide on YELLOW or RED (consider using 1 day)
		 */
		for (int i = 0; i < infected.length; i++) {
			if (infected[i].getTime() <= point.getTime()) {
				if (imported.intersects(generateGeometry(infected[i]))) {
					ui.updateStatus("Found a point of risk: (" + point.latitude + "," + point.longitude + ")");

					/*
					 * RED - 3 hours YELLOW - 24 hours GREEN - More than 24 hours
					 */
					long timeDiff = point.getTime() - infected[i].getTime();
					if (timeDiff <= (3 * HOUR_TO_MILLIS)) {
						ui.updateStatus("Exposure occured within 3 hours. Risk: RED");
						after = RISK.RED;
					} else if (timeDiff < (24 * HOUR_TO_MILLIS)) {
						ui.updateStatus("Exposure occured within 24 hours. Risk: YELLOW");
						if (after != RISK.RED) {
							after = RISK.YELLOW;
						}
					} else if (timeDiff >= (24 * HOUR_TO_MILLIS)) {
						ui.updateStatus("Exposure occured after 24 hours. Risk: GREEN");
						if (after == null) {
							after = RISK.GREEN;
						}
					}
				} // Intersection.
			}
		}

		if (after == null) {
			after = RISK.GREEN;
		}

		return new RISK[] { minimum, after };
	}
}

class PrivateExtractorUI {
	private JLabel fileStatus = null;
	private JLabel androidStatus = null;
	private JLabel iosStatus = null;

	// UI Panels
	private JPanel importPanels = null;
	private JPanel filePanel = null;
	private JPanel androidPanel = null;
	private JPanel iosPanel = null;

	public PrivateExtractorUI() {
		JFrame guiFrame = new JFrame();
		// make sure the program exits when the frame closes
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		guiFrame.setTitle("PrivateExtractorGUI");
		guiFrame.setSize(500, 460);
		// This will center the JFrame in the middle of the screen
		guiFrame.setLocationRelativeTo(null);
		guiFrame.setResizable(false);
		guiFrame.setIconImage(new ImageIcon("ic_launcher.png").getImage());

		importPanels = new JPanel(new GridLayout(4, 4, 4, 4));
		Icon android_icon = new ImageIcon("android.png");
		JButton android = new JButton(android_icon);
		Icon ios_icon = new ImageIcon("apple.png");
		JButton ios = new JButton(ios_icon);
		ios.setEnabled(false); // ios is not supported yet.
		Icon file_icon = new ImageIcon("file-text.png");
		JButton file = new JButton(file_icon);
		file.setEnabled(true);
		importPanels.add(android);
		importPanels.add(ios);
		importPanels.add(file);

		filePanel = new JPanel();
		filePanel.setVisible(false);
		fileStatus = new JLabel();
		filePanel.add(fileStatus, BorderLayout.SOUTH);
		filePanel.setDropTarget(new DropTarget() {
			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>) evt.getTransferable()
							.getTransferData(DataFlavor.javaFileListFlavor);
					for (File file : droppedFiles) {
						updateStatus("Importing: " + file.getName().toString()); // Only spit out the name.
						GUI.importFile(file.toString());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		androidPanel = new JPanel();
		androidPanel.setVisible(false);
		androidStatus = new JLabel();
		androidPanel.add(androidStatus, BorderLayout.SOUTH);

		iosPanel = new JPanel();
		iosPanel.setVisible(false);
		iosStatus = new JLabel();
		iosPanel.add(iosStatus, BorderLayout.SOUTH);

		file.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				importPanels.setVisible(!importPanels.isVisible());
				androidPanel.setVisible(!androidPanel.isVisible());
				filePanel.setVisible(!filePanel.isVisible());
				updateStatus("Starting File Extraction");
				updateStatus("Drag Exported File to THIS TEXT to Begin");
			}
		});

		android.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				importPanels.setVisible(!importPanels.isVisible());
				androidPanel.setVisible(!androidPanel.isVisible());
				filePanel.setVisible(!filePanel.isVisible());

				new Thread() {
					public void run() {
						startAndroidProcess();
					}
				}.start();
			}

			private String TARGET_PACKAGE = "edu.mit.privatekit";
			private String TARGET_NAME = "PrivateKit";

			private String EXTRACTOR_PACKAGE = "com.testing.PrivateExtractor";
			private String EXTRACTOR_APK = "private-extractor-debug.apk";
			private String EXTRACTOR_NAME = "PrivateExtractor";
			private String EXTRACTOR_RUNNER = "androidx.test.runner.AndroidJUnitRunner";

			private void startAndroidProcess() {
				updateStatus("Starting Android Extraction");
				preflight();
				go();

			}

			private void go() {
				/*
				 * Execution: Push Extractor APK Run Extractor tests Pull Extractor backup
				 * Delete Extractor Turn backup into tar file Turn tar file into folder Check
				 * hot.txt file
				 * 
				 * Magic to generate output file.
				 */

				updateStatus("Executing Android Extraction");

				if (launchProcessAndWaitForResponse(new String[] { "adb", "install", EXTRACTOR_APK }, "Success")) {
					updateStatus("Installed " + EXTRACTOR_NAME);
				} else {
					updateStatus("Could Not Install " + EXTRACTOR_NAME);
				}

				if (launchProcessAndWaitForResponse(new String[] { "adb", "shell", "am", "start", "-n",
						EXTRACTOR_PACKAGE + "/" + EXTRACTOR_PACKAGE + ".MainActivity" }, "Starting:")) {
					updateStatus("Started " + EXTRACTOR_NAME);
				} else {
					updateStatus("Could Not Start " + EXTRACTOR_NAME);
				}

				updateStatus("Attempting to Extract " + TARGET_NAME);

				if (launchProcessAndWaitForResponse(new String[] { "adb", "shell", "am", "instrument", "-w", "-r", "-e",
						"debug", "false", EXTRACTOR_PACKAGE + ".test/" + EXTRACTOR_RUNNER }, "OK (1 test)")) {
					updateStatus("Extracted " + TARGET_NAME);
				} else {
					updateStatus("Could Not Extract " + TARGET_NAME);
				}

				updateStatus("Please Click 'Backup My Data' to Retrieve Data");

				if (launchProcessAndWaitForResponse(new String[] { "adb", "backup", "-noapk", EXTRACTOR_PACKAGE },
						"confirm the backup operation")) {
					updateStatus("Launched Backup");
				} else {
					updateStatus("Could Not Launch Backup");
				}

				updateStatus("Waiting For backup.ab To Be Created");

				while (!fileExists("backup.ab")) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						updateStatus("Something Went Wrong With Backups");
					}
				}

				updateStatus("backup.ab Created");

				if (launchProcessAndWaitForResponse(new String[] { "adb", "uninstall", EXTRACTOR_PACKAGE },
						"Success")) {
					updateStatus("Uninstalled " + EXTRACTOR_NAME);
				} else {
					updateStatus("Could Not uninstall " + EXTRACTOR_NAME);
				}

				if (launchProcessAndWaitForResponse(
						new String[] { "java", "-jar", "abe-all.jar", "unpack", "backup.ab", "backup.tar" }, null)) { // Hacky
																														// hacky.
					updateStatus("Extracted backup.ab (Beta)");
				}

				if (launchProcessAndWaitForResponse(new String[] { "tar", "-zxvf", "backup.tar" }, null)) {
					updateStatus("Extracted backup.tar (Beta)");
				}

				if (fileExists(Paths.get("apps", EXTRACTOR_PACKAGE, "f", "hot.txt").toString())) {
					updateStatus("Found a Data File");
				} else {
					updateStatus("No Data File. Restart Device");
				}

				GUI.importFile(Paths.get("apps", EXTRACTOR_PACKAGE, "f", "hot.txt").toString());

				fileDelete("backup.ab");
				fileDelete("backup.tar");
				fileDelete("apps");
			}

			private void preflight() {
				/*
				 * Pre-flight: Check ADB Check for Devices Check if Target App is installed
				 * Check if Extractor apk exists Check if abe-all.jar exists Check if tar exists
				 */

				updateStatus("Preflight");

				if (launchProcessAndWaitForResponse(new String[] { "adb" }, "Android Debug Bridge")) {
					updateStatus("Found Working ADB");
				} else {
					updateStatus("ADB Not Found");
				}

				if (launchProcessAndWaitForResponse(new String[] { "adb", "devices", "-l" }, "product")) {
					updateStatus("Found a Device");
				} else {
					updateStatus("No Devices Found");
				}

				if (launchProcessAndWaitForResponse(
						new String[] { "adb", "shell", "pm", "list", "packages", TARGET_PACKAGE },
						"package:" + TARGET_PACKAGE)) {
					updateStatus("Found a version of " + TARGET_NAME);
				} else {
					updateStatus(TARGET_NAME + " Not Found");
				}

				if (fileExists("private-extractor-debug.apk")) {
					updateStatus("Found a version of " + EXTRACTOR_NAME);
				} else {
					updateStatus(EXTRACTOR_NAME + " Not Found");
				}

				if (fileExists("abe-all.jar")) {
					updateStatus("Found a version of Android Backup Extractor (ABE)");
				} else {
					updateStatus("Android Backup Extractor (ABE) Not Found");
				}

				// Not sure if this is going to be localized... try it on a foreign language PC.
				if (launchProcessAndWaitForResponse(new String[] { "tar", "--help" }, "manipulate archive files")) {
					updateStatus("Found a version of tar");
				} else {
					updateStatus("tar Not Found");
				}
			}
		});

		guiFrame.add(importPanels, BorderLayout.NORTH);
		guiFrame.add(androidPanel, BorderLayout.SOUTH);
		guiFrame.add(iosPanel, BorderLayout.SOUTH);
		guiFrame.add(filePanel, BorderLayout.SOUTH);

		// make sure the JFrame is visible
		guiFrame.setVisible(true);
	}

	public void reset() {
		importPanels.setVisible(true);
		androidPanel.setVisible(false);
		filePanel.setVisible(false);
	}

	public void updateStatus(String status) {
		System.out.println(status);
		GUI.log.add(status);
		this.androidStatus.setText(status);
		this.iosStatus.setText(status);
		this.fileStatus.setText(status);
	}

	public boolean fileExists(String filename) {
		updateStatus("Checking for file: " + filename);
		File tmpFile = new File(filename);
		return tmpFile.exists();
	}

	public void fileDelete(String filename) {
		File tmpFile = new File(filename);
		if (tmpFile.isDirectory()) {
			for (File sub : tmpFile.listFiles()) {
				fileDelete(sub.toString());
			}
		}
		updateStatus("Deleting file: " + filename);
		tmpFile.delete();
	}

	public boolean launchProcessAndWaitForResponse(String[] process, String expected) {
		boolean result = false;
		ProcessBuilder build = null;
		Process p = null;
		try {
			build = new ProcessBuilder(process);
			p = build.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		InputStream is = p.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		// System.out.println("Running process");
		if (expected == null) {
			result = true;
		} // Hackity Hack.

		try {
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				if (line.contains(expected)) {
					result = true;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}
}