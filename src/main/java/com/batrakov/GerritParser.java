package com.batrakov;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeMessageInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GerritParser {

    private static String mPathToEmployeesTable = "employees.xlsx";
    private static List<Employee> mEmployees = new ArrayList<>();
    private static ArrayList<Integer> mTargetChangesId = new ArrayList<>();
    private static final JFrame mFrame = new JFrame("Gerrit Parser");
    private static Date mRequiredDate;
    private static Date mStartDate;
    private static XSSFWorkbook mOutWorkbook;
    private static XSSFSheet mOutSheet;
    private static CreationHelper mCreationHelper;
    private static ArrayList<Object> mCellElements;

    public static void startLoading(String aLogin, char[] aPassword, String aStartDate, String aRequiredDate) {

        if (checkInternetConnection()) {
            if (getEmployeesFromFile()) {
                GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
                String password = new String(aPassword);
                GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", aLogin,
                        password);
                password = null;
                for (int i = 0; i < aPassword.length; i++) {
                    aPassword[i] = 0;
                }
                GerritApi gerritApi = gerritRestApiFactory.create(authData);
                if (checkAuthData(gerritApi) && aLogin != null && aPassword.length != 0) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        try {
                            mRequiredDate = formatter.parse(aRequiredDate);
                        } catch (ParseException aException) {
                            mRequiredDate = Calendar.getInstance().getTime();
                        }

                        try {
                            mStartDate = formatter.parse(aStartDate);
                        } catch (ParseException aException) {
                            mStartDate = Calendar.getInstance().getTime();
                        }

                        int startPosition = 0;

                        boolean needToProcess = true;
                        mOutWorkbook = new XSSFWorkbook();
                        mCreationHelper = mOutWorkbook.getCreationHelper();
                        mOutSheet = mOutWorkbook.createSheet("Gerrit");
                        int rowCount = 0;
                        createHeaderRow(mOutSheet);
                        while (needToProcess) {

                            List<ChangeInfo> changes = gerritApi.changes()
                                    .query("status:merged&o=DETAILED_ACCOUNTS&o=DETAILED_LABELS&o=MESSAGES"
                                            + "&start=" + startPosition).withLimit(500)
                                    .get();
                            startPosition += 500;



                            for (ChangeInfo changeInfo : changes) {
                                Date currentDate;
                                try {
                                    currentDate = formatter.parse(changeInfo.submitted.toString().substring(0, 10));
                                } catch (ParseException aException) {
                                    currentDate = Calendar.getInstance().getTime();
                                }
                                if (currentDate.compareTo(mRequiredDate) <= 0) {

                                    if (changeInfo.topic != null && changeInfo.topic.contains("MYCO")
                                            && isMerasEmployee(changeInfo.owner.name)) {
                                        mCellElements = new ArrayList<>();

                                        Row row = mOutSheet.createRow(++rowCount);

                                        mTargetChangesId.add(changeInfo._number);

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
                                        mCellElements.add(commitName);
                                        mCellElements.add(changeInfo.owner.name);
                                        mCellElements.add(changeInfo.project);
                                        mCellElements.add(changeInfo.submitted.toString().substring(0, 10));
                                        mCellElements.add(amountOfExternalComments);
                                        mCellElements.add(minusTwoCounter);
                                        mCellElements.add(minusOneCounter);
                                        mCellElements.add(plusOneCounter);
                                        mCellElements.add(plusTwoCounter);

                                        CellStyle cellStyle = mOutWorkbook.createCellStyle();
                                        cellStyle.setDataFormat(
                                                mCreationHelper.createDataFormat().getFormat("m/d/yy h:mm"));

                                        int columnCount = 0;
                                        for (Object element : mCellElements) {
                                            Cell cell = row.createCell(columnCount);
                                            columnCount++;

                                            if (element instanceof String) {
                                                cell.setCellValue((String) element);
                                                if (cell.getColumnIndex() == 0) {
                                                    XSSFHyperlink hyperlink = (XSSFHyperlink) mCreationHelper
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

                                    if (currentDate.compareTo(mStartDate) <= 0) {
                                        needToProcess = false;
                                        break;
                                    }
                                }
                            }
                        }

                        try {
                            FileOutputStream outputStream = new FileOutputStream("Gerrit.xlsx");
                            mOutWorkbook.write(outputStream);
                            JOptionPane.showMessageDialog(mFrame, "Successfuly done.");
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
                JOptionPane.showMessageDialog(mFrame, "Employees file not found (employees.xlsx)");
            }
        } else {
            JOptionPane.showMessageDialog(mFrame, "Error. Check your internet connection.");
        }
    }

    /**
     * Fill employees list from "employees.xlsx" file
     */
    private static boolean getEmployeesFromFile() {
        try {

            System.out.println();
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

    private static boolean checkInternetConnection() {
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritAuthData.Basic authData = new GerritAuthData.Basic("https://myco01.ascom-ws.com", null, null);

        GerritApi gerritApi = gerritRestApiFactory.create(authData);

        try {
            List<ChangeInfo> changes = gerritApi.changes()
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
            List<ChangeInfo> changes = aGerritApi.changes()
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
        font.setFontHeightInPoints((short) 16);
        cellStyle.setFont(font);

        Row row = sheet.createRow(0);

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
}
