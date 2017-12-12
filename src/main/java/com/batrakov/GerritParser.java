package com.batrakov;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.naming.LinkRef;
import javax.naming.RefAddr;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ReviewerInfo;
import com.google.gerrit.extensions.common.ApprovalInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;

public class GerritParser {

	private static final String CODE_REVIEW = "Code-Review";

	private static final Object[] FILE_HEADER = { "Title", "Person", "Module", "Date", "Amount of external comments",
			"-2", "-1", "+1", "+2" };

	private static String mCSVFilePath = "CSV/gerrit.csv";
	private static String mPathToEmployeesTable = "employees.xlsx";
	private static List<Employee> mEmployees = new ArrayList<Employee>();
	private static ArrayList<Integer> mTargetChangesId = new ArrayList<Integer>();
	private static final JFrame mFrame = new JFrame("Gerrit Parser");
	private static int mAmountOfCommits = 200;

	public static void main(String[] args) throws IOException {
		FileWriter fileWriter = null;

		CSVPrinter csvFilePrinter = null;
		// Create the CSVFormat object with "\n" as a record delimiter

		CSVFormat csvFileFormat = CSVFormat.EXCEL.withRecordSeparator("\n").withQuote(null).withDelimiter('\t');

		String inputAmountOfCommits = JOptionPane.showInputDialog("Enter amount of commits:");
		try {
			mAmountOfCommits = Integer.parseInt(inputAmountOfCommits);
		} catch (NumberFormatException aException) {
			mAmountOfCommits = 200;
		}

		if (getEmployeesFromFile()) {
			Thread dialogThread = new Thread((new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mFrame, "processing...");
				}
			}));
			dialogThread.start();

			// initialize FileWriter object
			fileWriter = new FileWriter(mCSVFilePath);
			// initialize CSVPrinter object
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			// Create CSV file header
			csvFilePrinter.printRecord(FILE_HEADER);

			GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();

			GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", "seprjok",
					"DummyUser01");
			GerritApi gerritApi = gerritRestApiFactory.create(authData);
			try {
				List<ChangeInfo> changes = gerritApi.changes()
						.query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES")
						.withLimit(mAmountOfCommits).get();
				if (mAmountOfCommits > 500) {
					int startPosition = 500;
					while (mAmountOfCommits > 500) {
						changes.addAll(gerritApi.changes()
								.query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES" + "&start="
										+ startPosition)
								.get());
						startPosition += 500;
						mAmountOfCommits -= 500;
					}
					System.out.println(changes.size());
				}
				for (ChangeInfo changeInfo : changes) {
					List<Comparable> changeRecord = new ArrayList<Comparable>();
					// Check changeInfo for good condition
					if (changeInfo.topic != null && changeInfo.topic.contains("MYCO")
							&& isMerasEmployee(changeInfo.owner.name)) {
						mTargetChangesId.add(changeInfo._number);

						String commitURL = "\"https://myco01.ascom-ws.com/#/c/" + changeInfo._number + "/\"";
						String commitName = changeInfo.subject;
						commitName = commitName.replace("\"", "");
						commitName = "\"" + commitName + "\"";

						String hyperlink = "=HYPERLINK(" + commitURL + "," + commitName + ")";

						changeRecord.add(hyperlink);
						changeRecord.add(changeInfo.owner.name);
						changeRecord.add(changeInfo.project);
						changeRecord.add(changeInfo.submitted.toString().substring(0, 10));

						ArrayList<ChangeMessageInfo> messages = new ArrayList<ChangeMessageInfo>(changeInfo.messages);

						int amountOfExternalComments = 0;
						int minusTwoCounter = 0;
						int minusOneCounter = 0;
						int plusOneCounter = 0;
						int plusTwoCounter = 0;
						String strAmountOfExternalComments;
						for (ChangeMessageInfo changeMessageInfo : messages) {
							if (changeMessageInfo.author != null && !isMerasEmployee(changeMessageInfo.author.name)
									&& !isJenkins(changeMessageInfo.author.name)) {
								if (changeMessageInfo.message.contains("comments")) {
									strAmountOfExternalComments = changeMessageInfo.message.substring(
											changeMessageInfo.message.indexOf("comments") - 2,
											changeMessageInfo.message.indexOf("comments") - 1);
									try {
										amountOfExternalComments += Integer.parseInt(strAmountOfExternalComments);
									} catch (NumberFormatException e) {
										amountOfExternalComments = 0;
									}
									
								}
								System.out.println(changeMessageInfo.message);
								if (changeMessageInfo.message.contains("-2")) {
									minusTwoCounter++;
								}
								if (changeMessageInfo.message.contains("-1")) {
									minusOneCounter++;
								}
								if (changeMessageInfo.message.contains("+1")) {
									plusOneCounter++;
								}
								if (changeMessageInfo.message.contains("+2")) {
									plusTwoCounter++;
								}
								System.out.println(minusTwoCounter);
								System.out.println(minusOneCounter);
								System.out.println(plusOneCounter);
								System.out.println(plusTwoCounter);
							}
						}
						changeRecord.add(amountOfExternalComments);
						changeRecord.add(minusTwoCounter);
						changeRecord.add(minusOneCounter);
						changeRecord.add(plusOneCounter);
						changeRecord.add(plusTwoCounter);
						csvFilePrinter.printRecord(changeRecord);
					}
				}

				// Write data to CSV file
				try {

					File CSVFile = new File(mCSVFilePath);
					CSVFile.getParentFile().mkdirs();
					if (!CSVFile.exists()) {
						CSVFile.createNewFile();
					}
					fileWriter.flush();
					fileWriter.close();
					csvFilePrinter.close();
					dialogThread.interrupt();
					JOptionPane.showMessageDialog(mFrame, "Successfuly done.");
				} catch (IOException ignore) {
				}

			} catch (RestApiException e) {
				dialogThread.interrupt();
				JOptionPane.showMessageDialog(mFrame, "Error. Check your internet connection.");
				e.printStackTrace();
			}
		} else {
			JOptionPane.showMessageDialog(mFrame, "Employees file not found (employees.xlsx)");
		}
	}

	/**
	 * Fill employees list from "employees.xlsx" file
	 */
	private static boolean getEmployeesFromFile() {
		try {
			FileInputStream inputStream = new FileInputStream(mPathToEmployeesTable);
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet firstSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = firstSheet.iterator();
			iterator.next();
			while (iterator.hasNext()) {
				Row nextRow = iterator.next();
				Iterator<Cell> cellIterator = nextRow.cellIterator();
				Employee employee = new Employee();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					int column = cell.getAddress().getColumn();
					if (column == 0) {
						employee.setName(cell.getStringCellValue());
					} else if (column == 1) {
						employee.setTeam(cell.getStringCellValue());
					}
				}
				mEmployees.add(employee);
			}

			workbook.close();
			inputStream.close();

		} catch (FileNotFoundException aException) {
			return false;
		} catch (IOException ignore) {
		}
		return true;
	}

	private static boolean isMerasEmployee(String aName) {
		for (Employee employee : mEmployees) {
			if (aName.equals(employee.getName())) {
				return true;
			}
		}
		return false;
	}

	private static boolean isJenkins(String aName) {
		if (aName.equals("Jenkins")) {
			return true;
		}
		return false;
	}
}
