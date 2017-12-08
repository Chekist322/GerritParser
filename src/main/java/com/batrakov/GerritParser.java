package com.batrakov;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;

public class GerritParser {
	
	private static final String CODE_REVIEW = "Code-Review";
	private static String mOutCSVString = "Title^Person^Module^Date^Rate^Amount of ExtComments\n";
	private static String mCSVFilePath = "CSV/gerrit.txt";
	private static String mPathToEmployeesTable = "employees.xlsx";
	private static List<Employee> mEmployees = new ArrayList<Employee>();

	public static void main(String[] args) {

		getEmployeesFromFile();

		GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();

		GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", "seprjok",
				"DummyUser01");
		GerritApi gerritApi = gerritRestApiFactory.create(authData);
		try {
			List<ChangeInfo> changes = gerritApi.changes()
					.query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES").withLimit(200).get();
			for (ChangeInfo changeInfo : changes) {
				
				//Check changeInfo for good condition
				if (changeInfo.topic != null && changeInfo.topic.contains("MYCO")
						&& isMerasEmployee(changeInfo.owner.name)) {

					String commitURL = "\"https://myco01.ascom-ws.com/#/c/15953/\"";
					String commitName = "\"" + changeInfo.subject + "\"";
					String hyperlink = "=HYPERLINK(" + commitURL + "," + commitName + ")";
					mOutCSVString = mOutCSVString.concat(hyperlink + "^" + changeInfo.owner.name + "^"
							+ changeInfo.branch + "^" + changeInfo.submitted.toString().substring(0, 10));

					LabelInfo label = changeInfo.labels.get(CODE_REVIEW);

					for (ApprovalInfo approvalInfo : label.all) {
						// System.out.println(approvalInfo.name);
						// System.out.println(approvalInfo.value);
					}

					mOutCSVString = mOutCSVString.concat("\n");
				}
			}

			//Write data to CSV file
			try {
				File CSVFile = new File(mCSVFilePath);
				CSVFile.getParentFile().mkdirs();
				if (!CSVFile.exists()) {
					CSVFile.createNewFile();
				}
				FileOutputStream outputStream = new FileOutputStream(CSVFile);
				outputStream.write(mOutCSVString.getBytes(), 0, mOutCSVString.getBytes().length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (RestApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Fill employees list from "employees.xlsx" file
	 */
	private static void getEmployeesFromFile() {
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
					System.out.println(cell.getStringCellValue());
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

		} catch (FileNotFoundException ignore) {
		} catch (IOException ignore) {
		}
	}

	private static boolean isMerasEmployee(String aName) {
		for (Employee employee : mEmployees) {
			if (aName.equals(employee.getName())) {
				return true;
			}
		}
		return false;
	}
}
