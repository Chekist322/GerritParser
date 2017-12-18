package com.batrakov;

import org.jdatepicker.JDatePanel;
import org.jdatepicker.JDatePicker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.prefs.Preferences;

public class RequestForm extends JFrame {

    private final String LOGIN_PREF = "login_pref";

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
        setBounds(100, 100, 520, 160);
        setVisible(true);
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

    }

    private void login() {

        String login = loginField.getText();
        if (login != null && !login.equals("")) {
            mPrefs.put(LOGIN_PREF, login);
        }

        final String since = String.valueOf(sinceDatePicker.getModel().getYear()) +
                "-" + String.valueOf(sinceDatePicker.getModel().getMonth()) +
                "-" + String.valueOf(sinceDatePicker.getModel().getDay());

        final String to = String.valueOf(toDatePicker.getModel().getYear()) +
                "-" + String.valueOf(toDatePicker.getModel().getMonth()) +
                "-" + String.valueOf(toDatePicker.getModel().getDay());

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
