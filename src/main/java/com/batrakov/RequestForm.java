package com.batrakov;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class RequestForm extends JFrame {

    private JTextField startDate;
    private JTextField requiredDate;
    private JButton sendRequestButton;
    private JPanel panel;
    private JLabel since;
    private JLabel to;
    private JTextField loginField;
    private JPasswordField passwordField;
    private JLabel pleaseWaitLabel;

    public RequestForm() {
        setContentPane(panel);
        setBounds(100, 100, 520, 174);
        setVisible(true);
        pleaseWaitLabel.setVisible(false);
        setUpListeners();
    }

    public static void main(String[] args) {
        new RequestForm();
    }

    private void setUpListeners() {

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        requiredDate.addKeyListener(new KeyAdapter() {

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
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GerritParser.startLoading(loginField.getText(), passwordField.getPassword(), startDate.getText(), requiredDate.getText());
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
