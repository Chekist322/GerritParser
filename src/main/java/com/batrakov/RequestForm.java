package com.batrakov;

import org.jdatepicker.JDatePanel;
import org.jdatepicker.JDatePicker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.Preferences;

public class RequestForm extends JFrame {

    private final String LOGIN_PREF = "login_pref";
    private final String START_DATE_PREF_YEAR = "start_date_pref_year";
    private final String START_DATE_PREF_MONTH = "start_date_pref_month";
    private final String START_DATE_PREF_DAY = "start_date_pref_day";
    private final String TO_DATE_PREF_YEAR = "to_date_login_pref_year";
    private final String TO_DATE_PREF_MONTH = "to_date_login_pref_month";
    private final String TO_DATE_PREF_DAY = "to_date_login_pref_day";

    private JButton sendRequestButton;
    private JPanel panel;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JLabel pleaseWaitLabel;
    private JDatePicker sinceDatePicker;
    private JDatePicker toDatePicker;

    private Preferences mPrefs = Preferences.userNodeForPackage(RequestForm.class);


    public RequestForm() {
        setContentPane(panel);
        setBounds(100, 100, 520, 200);
        setVisible(true);
        setResizable(false);
        setTitle("Mera Gerrit Parser");
        pleaseWaitLabel.setVisible(false);
        setUpListeners();
    }

    public static void main(String[] args) {
        new RequestForm();
    }

    private void setUpListeners() {
        if (mPrefs.get(LOGIN_PREF, null) != null) {
            loginField.setText(mPrefs.get(LOGIN_PREF, null));
        }

        Calendar sinceCalendar = Calendar.getInstance();
        int currentMonth = sinceCalendar.get(Calendar.MONTH);
        if (currentMonth > 0) {
            sinceCalendar.set(Calendar.MONTH, currentMonth-1);
        } else {
            sinceCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        }
        sinceDatePicker.getModel().setYear(sinceCalendar.get(Calendar.YEAR));
        sinceDatePicker.getModel().setMonth(sinceCalendar.get(Calendar.MONTH));
        sinceDatePicker.getModel().setDay(sinceCalendar.get(Calendar.DAY_OF_MONTH));

        sinceDatePicker.setFormattedTextField(sinceCalendar);
        toDatePicker.setFormattedTextField(Calendar.getInstance());

        int year;
        int month;
        int day;
        if (mPrefs.get(START_DATE_PREF_YEAR, null) != null) {
            year = Integer.parseInt(mPrefs.get(START_DATE_PREF_YEAR, null));
            month = Integer.parseInt(mPrefs.get(START_DATE_PREF_MONTH, null));
            day = Integer.parseInt(mPrefs.get(START_DATE_PREF_DAY, null));
            sinceDatePicker.getModel().setYear(year);
            sinceDatePicker.getModel().setMonth(month);
            sinceDatePicker.getModel().setDay(day);
            sinceDatePicker.setFormattedTextField(year, month, day);
        }

        if (mPrefs.get(TO_DATE_PREF_YEAR, null) != null) {
            year = Integer.parseInt(mPrefs.get(TO_DATE_PREF_YEAR, null));
            month = Integer.parseInt(mPrefs.get(TO_DATE_PREF_MONTH, null));
            day = Integer.parseInt(mPrefs.get(TO_DATE_PREF_DAY, null));
            toDatePicker.getModel().setYear(year);
            toDatePicker.getModel().setMonth(month);
            toDatePicker.getModel().setDay(day);
            toDatePicker.setFormattedTextField(year, month, day);
        }

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        passwordField.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onPreLoading();
                    login();
                }
            }
        });

        sendRequestButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onPreLoading();
                login();
            }
        });

        if (!GerritParser.getEmployeesFromFile()) {
            GerritParser.createEmployeesEmptyFile();
        }
    }

    private void login() {

        String login = loginField.getText();
        if (login != null && !login.equals("")) {
            mPrefs.put(LOGIN_PREF, login);
        }

        mPrefs.put(START_DATE_PREF_YEAR, String.valueOf(sinceDatePicker.getModel().getYear()));
        mPrefs.put(START_DATE_PREF_MONTH, String.valueOf(sinceDatePicker.getModel().getMonth()));
        mPrefs.put(START_DATE_PREF_DAY, String.valueOf(sinceDatePicker.getModel().getDay()));
        mPrefs.put(TO_DATE_PREF_YEAR, String.valueOf(toDatePicker.getModel().getYear()));
        mPrefs.put(TO_DATE_PREF_MONTH, String.valueOf(toDatePicker.getModel().getMonth()));
        mPrefs.put(TO_DATE_PREF_DAY, String.valueOf(toDatePicker.getModel().getDay()));

        final String since = String.valueOf(sinceDatePicker.getModel().getYear()) +
                "-" + String.valueOf(sinceDatePicker.getModel().getMonth()) +
                "-" + String.valueOf(sinceDatePicker.getModel().getDay());

        final String to = String.valueOf(toDatePicker.getModel().getYear()) +
                "-" + String.valueOf(toDatePicker.getModel().getMonth()) +
                "-" + String.valueOf(toDatePicker.getModel().getDay());

        System.out.println(since);


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GerritParser.startLoading(loginField.getText(), passwordField.getPassword(), since, to);
                onPostLoading();
            }
        });
        thread.start();

    }

    private void onPreLoading() {
        pleaseWaitLabel.setVisible(true);
        sendRequestButton.setEnabled(false);
    }

    private void onPostLoading() {
        pleaseWaitLabel.setVisible(false);
        sendRequestButton.setEnabled(true);
    }
}
