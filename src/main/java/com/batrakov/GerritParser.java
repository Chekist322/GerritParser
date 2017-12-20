package com.batrakov;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class GerritParser {

    private static List<Employee> mEmployees = new ArrayList<>();
    private static final JFrame mFrame = new JFrame("Gerrit Parser");

    public static void startLoading(String aLogin, char[] aPassword, String aStartDate, String aRequiredDate) {

        if (checkInternetConnection()) {
            if (!getEmployeesFromFile()) {
                createEmployeesEmptyFile();
            }
            GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
            String password = new String(aPassword);
            GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", aLogin,
                    password);
            for (int i = 0; i < aPassword.length; i++) {
                aPassword[i] = 0;
            }
            GerritApi gerritApi = gerritRestApiFactory.create(authData);
            if (checkAuthData(gerritApi) && aLogin != null && aPassword.length != 0) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    Date requiredDate;
                    try {
                        requiredDate = formatter.parse(aRequiredDate);
                    } catch (ParseException aException) {
                        requiredDate = Calendar.getInstance().getTime();
                    }

                    Date startDate;
                    try {
                        startDate = formatter.parse(aStartDate);
                    } catch (ParseException aException) {
                        startDate = Calendar.getInstance().getTime();
                    }

                    int startPosition = 0;

                    boolean needToProcess = true;
                    XSSFWorkbook outWorkbook = new XSSFWorkbook();
                    CreationHelper creationHelper = outWorkbook.getCreationHelper();
                    XSSFSheet outSheet = outWorkbook.createSheet("Gerrit");
                    int rowCount = 1;
                    createHeaderRow(outSheet);
                    while (needToProcess) {

                        List<ChangeInfo> changes = gerritApi.changes()
                                .query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES"
                                        + "&start=" + startPosition).withLimit(500)
                                .get();
                        startPosition += 500;


                        for (ChangeInfo changeInfo : changes) {
                            Date currentDate;
                            try {
                                currentDate = formatter.parse(changeInfo.updated.toString().substring(0, 10));
                            } catch (ParseException aException) {
                                currentDate = Calendar.getInstance().getTime();
                            }
                            if (currentDate.compareTo(requiredDate) <= 0) {

                                if (changeInfo.topic != null && changeInfo.topic.contains("MYCO")
                                        && isMerasEmployee(changeInfo.owner.name)) {
                                    ArrayList<Object> cellElements = new ArrayList<>();

                                    Row row = outSheet.createRow(++rowCount);

                                    String commitURL = "https://myco01.ascom-ws.com/#/c/" + changeInfo._number + "/";
                                    String commitName = changeInfo.subject;

                                    ArrayList<ChangeMessageInfo> messages = new ArrayList<>(
                                            changeInfo.messages);

                                    int amountOfExternalComments = 0;
                                    int minusTwoCounter = 0;
                                    int minusOneCounter = 0;
                                    int plusOneCounter = 0;
                                    int plusTwoCounter = 0;
                                    String strAmountOfExternalComments;
                                    for (ChangeMessageInfo changeMessageInfo : messages) {
                                        if (changeMessageInfo.author != null
                                                && !isMerasEmployee(changeMessageInfo.author.name)
                                                && !isJenkins(changeMessageInfo.author.name)) {
                                            if (changeMessageInfo.message.contains("comments")) {
                                                strAmountOfExternalComments = changeMessageInfo.message.substring(
                                                        changeMessageInfo.message.indexOf("comments") - 2,
                                                        changeMessageInfo.message.indexOf("comments") - 1);
                                                try {
                                                    amountOfExternalComments += Integer
                                                            .parseInt(strAmountOfExternalComments);
                                                } catch (NumberFormatException e) {
                                                    amountOfExternalComments = 0;
                                                }

                                            }
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
                                        }
                                    }
                                    cellElements.add(commitName);
                                    cellElements.add(changeInfo.owner.name);
                                    cellElements.add(changeInfo.project);
                                    cellElements.add(changeInfo.submitted.toString().substring(0, 10));
                                    cellElements.add(amountOfExternalComments);
                                    cellElements.add(minusTwoCounter);
                                    cellElements.add(minusOneCounter);
                                    cellElements.add(plusOneCounter);
                                    cellElements.add(plusTwoCounter);

                                    CellStyle cellStyle = outWorkbook.createCellStyle();
                                    cellStyle.setDataFormat(
                                            creationHelper.createDataFormat().getFormat("m/d/yy h:mm"));

                                    int columnCount = 0;
                                    for (Object element : cellElements) {
                                        Cell cell = row.createCell(columnCount);
                                        columnCount++;

                                        if (element instanceof String) {
                                            cell.setCellValue((String) element);
                                            if (cell.getColumnIndex() == 0) {
                                                XSSFHyperlink hyperlink = (XSSFHyperlink) creationHelper
                                                        .createHyperlink(HyperlinkType.URL);
                                                hyperlink.setAddress(commitURL);
                                                cell.setHyperlink(hyperlink);
                                            }
                                            if (cell.getColumnIndex() == 3) {
                                                cell.setCellStyle(cellStyle);
                                            }
                                        } else if (element instanceof Integer) {
                                            cell.setCellValue((Integer) element);
                                        }
                                    }
                                }

                                if (currentDate.compareTo(startDate) <= 0) {
                                    needToProcess = false;
                                    break;
                                }
                            }
                        }
                    }

                    try {
                        String gerritParseFilePath = "gerrit_" + aStartDate + "_" + aRequiredDate + ".xlsx";
                        FileOutputStream outputStream = new FileOutputStream(gerritParseFilePath);
                        outWorkbook.write(outputStream);
                        int openChoice = JOptionPane.showConfirmDialog(mFrame, "Successfully done. Do you want to open created table?", "Done", JOptionPane.YES_NO_OPTION);
                        switch (openChoice) {
                            case JOptionPane.YES_OPTION:
                                openCreatedFile(gerritParseFilePath);
                                break;
                            case JOptionPane.NO_OPTION:
                                break;
                        }
                    } catch (IOException ignore) {
                    }


                } catch (RestApiException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(mFrame,
                            "Error. Check your login, password or internet connection.");
                }

            } else {
                JOptionPane.showMessageDialog(mFrame, "Error. Wrong login or password.");
            }
        } else {
            JOptionPane.showMessageDialog(mFrame, "Error. Check your internet connection.");
        }
    }

    private static void openCreatedFile(String aFileName) {
        try {
            File file = new File(aFileName);
            Desktop.getDesktop().open(file);
        } catch (IOException aE) {
            aE.printStackTrace();
        }
    }

    /**
     * Fill employees list from "employees.xlsx" file
     */
    public static boolean getEmployeesFromFile() {
        try {
            System.out.println();
            String pathToEmployeesTable = "employees.xlsx";
            FileInputStream inputStream = new FileInputStream(pathToEmployeesTable);
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
            aException.printStackTrace();
            return false;
        } catch (IOException ignore) {
            ignore.printStackTrace();
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
        return aName.equals("Jenkins");
    }

    private static boolean checkInternetConnection() {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", null, null);

        GerritApi gerritApi = gerritRestApiFactory.create(authData);

        try {
            gerritApi.changes()
                    .query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES").withLimit(1)
                    .get();
        } catch (RestApiException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean checkAuthData(GerritApi aGerritApi) {
        try {
            aGerritApi.changes()
                    .query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES").get();
        } catch (RestApiException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void createHeaderRow(Sheet sheet) {

        CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);

        Row zeroRow = sheet.createRow(0);
        sheet.addMergedRegion(new CellRangeAddress(0,0,5,8));

        Cell cellZeroRow = zeroRow.createCell(5);
        cellZeroRow.setCellStyle(cellStyle);
        cellZeroRow.setCellValue("External comments");

        Row row = sheet.createRow(1);

        Cell cellTitle = row.createCell(0);
        cellTitle.setCellStyle(cellStyle);
        cellTitle.setCellValue("Title");

        Cell cellAuthor = row.createCell(1);
        cellAuthor.setCellStyle(cellStyle);
        cellAuthor.setCellValue("Person");

        Cell cellModule = row.createCell(2);
        cellModule.setCellStyle(cellStyle);
        cellModule.setCellValue("Module");

        Cell cellDate = row.createCell(3);
        cellDate.setCellStyle(cellStyle);
        cellDate.setCellValue("Date");

        Cell cellAmount = row.createCell(4);
        cellAmount.setCellStyle(cellStyle);
        cellAmount.setCellValue("Amount of external comments");

        Cell cellMinusTwo = row.createCell(5);
        cellMinusTwo.setCellStyle(cellStyle);
        cellMinusTwo.setCellValue("-2");

        Cell cellMinusOne = row.createCell(6);
        cellMinusOne.setCellStyle(cellStyle);
        cellMinusOne.setCellValue("-1");

        Cell cellPlusOne = row.createCell(7);
        cellPlusOne.setCellStyle(cellStyle);
        cellPlusOne.setCellValue("+1");

        Cell cellPlusTwo = row.createCell(8);
        cellPlusTwo.setCellStyle(cellStyle);
        cellPlusTwo.setCellValue("+2");
    }

    public static void createEmployeesEmptyFile() {
        XSSFWorkbook employeesWorkbook = new XSSFWorkbook();
        XSSFSheet employeesSheet = employeesWorkbook.createSheet("employees");

        CellStyle cellStyle = employeesSheet.getWorkbook().createCellStyle();
        Font font = employeesSheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        cellStyle.setFont(font);

        Row row = employeesSheet.createRow(0);

        Cell cellTitle = row.createCell(0);
        cellTitle.setCellStyle(cellStyle);
        cellTitle.setCellValue("Employee");

        Cell cellAuthor = row.createCell(1);
        cellAuthor.setCellStyle(cellStyle);
        cellAuthor.setCellValue("Team");

        try {
            FileOutputStream outputStream = new FileOutputStream("employees.xlsx");
            employeesWorkbook.write(outputStream);
            JOptionPane.showMessageDialog(mFrame, "Created new empty \"employees.xlsx\" file");
        } catch (IOException aE) {
            aE.printStackTrace();
        }
    }
}
