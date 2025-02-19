package com.ibm.de103252.lockdemo;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

@SuppressWarnings("serial")
public class SqlPanel extends JPanel {
    /**
     * @wbp.nonvisual location=85,473
     */
    private final Executor executor = new Executor();
    private JTextArea sql;
    private JComboBox<String> urlComboBox;
    private JButton connectButton;
    private JLabel busy;
    private JButton nextButton;
    private JButton rollbackButton;
    private JButton updateButton;
    private JButton executeButton;
    private JButton commitButton;
    private JPanel isolationPanel;
    private JLabel isolationLabel;
    private JComboBox<IsolationLevel> isolationLevel;

    public Executor getExecutor() {
        return executor;
    }

    /**
     * Create the panel.
     */
    public SqlPanel() {
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 600, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 27, 7, 113, 33, 0, 0, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        setLayout(gridBagLayout);
        JPanel panel = new JPanel();
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.insets = new Insets(0, 0, 5, 0);
        gbc_panel.gridx = 0;
        gbc_panel.gridy = 0;
        add(panel, gbc_panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[] { 201, 86, 0 };
        gbl_panel.rowHeights = new int[] { 23, 0 };
        gbl_panel.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
        gbl_panel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        panel.setLayout(gbl_panel);
        urlComboBox = new JComboBox<String>();
        urlComboBox.setEditable(true);
        GridBagConstraints gbc_urlTextField = new GridBagConstraints();
        gbc_urlTextField.weightx = 1.0;
        gbc_urlTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_urlTextField.anchor = GridBagConstraints.WEST;
        gbc_urlTextField.insets = new Insets(0, 0, 0, 5);
        gbc_urlTextField.gridx = 0;
        gbc_urlTextField.gridy = 0;
        panel.add(urlComboBox, gbc_urlTextField);
        connectButton = new JButton("Connect");
        connectButton.setToolTipText("Connect to the data source");
        connectButton.setEnabled(false);
        GridBagConstraints gbc_connectButton = new GridBagConstraints();
        gbc_connectButton.anchor = GridBagConstraints.NORTHWEST;
        gbc_connectButton.gridx = 1;
        gbc_connectButton.gridy = 0;
        panel.add(connectButton, gbc_connectButton);
        isolationPanel = new JPanel();
        GridBagConstraints gbc_isolationPanel = new GridBagConstraints();
        gbc_isolationPanel.insets = new Insets(0, 0, 5, 0);
        gbc_isolationPanel.fill = GridBagConstraints.BOTH;
        gbc_isolationPanel.gridx = 0;
        gbc_isolationPanel.gridy = 2;
        add(isolationPanel, gbc_isolationPanel);
        isolationLabel = new JLabel("Isolation");
        isolationLabel.setDisplayedMnemonic('I');
        isolationPanel.add(isolationLabel);
        isolationLevel = new JComboBox<IsolationLevel>();
        isolationLabel.setLabelFor(isolationLevel);
        isolationLevel.setToolTipText("Select isolation level for next transaction");
        isolationLevel.setModel(new DefaultComboBoxModel<IsolationLevel>(IsolationLevel.values()));
        isolationLevel.setSelectedIndex(IsolationLevel.ReadCommitted.ordinal());
        isolationPanel.add(isolationLevel);
        JScrollPane scrollPane_1 = new JScrollPane();
        GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
        gbc_scrollPane_1.weighty = 30.0;
        gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane_1.gridx = 0;
        gbc_scrollPane_1.gridy = 3;
        add(scrollPane_1, gbc_scrollPane_1);
        sql = new JTextArea();
        sql.setToolTipText("Enter SQL text here");
        scrollPane_1.setViewportView(sql);
        sql.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.weighty = 120.0;
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 4;
        add(scrollPane, gbc_scrollPane);
        JTextArea result = new JTextArea();
        result.setToolTipText("Displays SQL results");
        scrollPane.setViewportView(result);
        result.setFont(new Font("Consolas", Font.PLAIN, 13));
        JPanel buttonPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) buttonPanel.getLayout();
        flowLayout.setVgap(3);
        GridBagConstraints gbc_buttonPanel = new GridBagConstraints();
        gbc_buttonPanel.insets = new Insets(0, 0, 5, 0);
        gbc_buttonPanel.anchor = GridBagConstraints.SOUTH;
        gbc_buttonPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_buttonPanel.gridx = 0;
        gbc_buttonPanel.gridy = 5;
        add(buttonPanel, gbc_buttonPanel);
        executeButton = new JButton("Execute");
        executeButton.setToolTipText("Execute selected SQL statement");
        executeButton.setEnabled(false);
        executeButton.addActionListener(e -> {
            IsolationLevel selectedIsolation = (IsolationLevel) getIsolationLevel().getSelectedItem();
            getExecutor().setIsolationLevel(selectedIsolation.getIsolation());
            getExecutor().execute(getSelectedSQLStatement());
            updateControls();
        });
        buttonPanel.add(executeButton);
        nextButton = new JButton("Next");
        nextButton.setToolTipText("Move result set to next row");
        nextButton.setEnabled(false);
        buttonPanel.add(nextButton);
        commitButton = new JButton("Commit");
        commitButton.setToolTipText("Commit the current transaction");
        commitButton.addActionListener(e -> executor.commit());
        updateButton = new JButton("Update");
        updateButton.setToolTipText("Update row that the current result set is positioned on");
        updateButton.setEnabled(false);
        buttonPanel.add(updateButton);
        commitButton.setEnabled(executor.isInTransaction());
        buttonPanel.add(commitButton);
        rollbackButton = new JButton("Rollback");
        rollbackButton.setToolTipText("Roll the current transaction back");
        rollbackButton.addActionListener(e -> executor.rollback());
        rollbackButton.setEnabled(executor.isInTransaction());
        buttonPanel.add(rollbackButton);
        busy = new JLabel("Disconnected");
        busy.setOpaque(true);
        busy.setBackground(Color.LIGHT_GRAY);
        busy.setHorizontalAlignment(SwingConstants.CENTER);
        GridBagConstraints gbc_busy = new GridBagConstraints();
        gbc_busy.insets = new Insets(0, 5, 5, 5);
        gbc_busy.fill = GridBagConstraints.HORIZONTAL;
        gbc_busy.gridx = 0;
        gbc_busy.gridy = 1;
        add(busy, gbc_busy);
        nextButton.addActionListener(e -> {
            executor.next();
            updateControls();
        });
        executor.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                case "result":
                    result.setText(evt.getNewValue().toString());
                    result.setCaretPosition(result.getText().length());
                    break;
                }
                updateControls();
            }
        });
        sql.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                executeButton.setEnabled(
                        SqlPanel.this.getExecutor().isConnected() && notBlank(getSelectedSQLStatement()));
            }
        });
        updateButton.addActionListener(e -> {
            updateButton.setEnabled(false);
            getExecutor().update();
        });
        connectButton.addActionListener(e -> connectOrDisconnect());
        initControls();
        updateControls();
    }

    private void connectOrDisconnect() {
        if (getExecutor().isConnected()) {
            getExecutor().disconnect();
        } else {
            getExecutor().setUrl(getUrlComboBox().getSelectedItem().toString());
            getExecutor().connect();
        }
    }

    private void initControls() {
        if (true) {
            Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
            DefaultComboBoxModel<String> cbm = new DefaultComboBoxModel<>();
            try {
                for (String key : prefs.keys()) {
                    if (key.startsWith("url.")) {
                        cbm.addElement(prefs.get(key, ""));
                    }
                }
            } catch (BackingStoreException e) {
            }
            if (cbm.getSize() == 0) {
                cbm.addElement("jdbc:derby:memory:derbyDB;create=true");
                cbm.addElement("jdbc:derby:derbyDB;create=true");
                cbm.addElement("jdbc:derby://localhost:1527/DBIDB");
            }
            getUrlComboBox().setModel(cbm);
        }
    }

    private void updateControls() {
        boolean isBusy = getExecutor().isBusy();
        boolean isInTransaction = getExecutor().isInTransaction();
        boolean isConnected = getExecutor().isConnected();
        if (isBusy) {
            getUrlComboBox().setEnabled(false);
            getBusy().setBackground(Color.RED);
            getBusy().setText("Busy: " + getExecutor().getCurrentXid());
            getNextButton().setEnabled(false);
            getExecuteButton().setEnabled(false);
            getCommitButton().setEnabled(false);
            getRollbackButton().setEnabled(false);
            getUpdateButton().setEnabled(false);
            getIsolationLevel().setEnabled(false);
            getConnectButton().setEnabled(false);
        } else if (isInTransaction) {
            getUrlComboBox().setEnabled(false);
            getBusy().setBackground(Color.YELLOW);
            getBusy().setText("In Tx: " + getExecutor().getCurrentXid());
            getNextButton().setEnabled(getExecutor().isResultSetOpen());
            getUpdateButton().setEnabled(getExecutor().isResultSetPositioned());
            getExecuteButton().setEnabled(notBlank(getSelectedSQLStatement()));
            getCommitButton().setEnabled(true);
            getRollbackButton().setEnabled(true);
            getIsolationLevel().setEnabled(false);
            getConnectButton().setEnabled(true);
        } else if (isConnected) {
            getUrlComboBox().setEnabled(false);
            getBusy().setBackground(Color.GREEN);
            getBusy().setText("Connected");
            getCommitButton().setEnabled(false);
            getRollbackButton().setEnabled(false);
            getNextButton().setEnabled(false);
            getUpdateButton().setEnabled(false);
            getExecuteButton().setEnabled(notBlank(getSelectedSQLStatement()));
            getIsolationLevel().setSelectedItem(getExecutor().getIsolationLevel());
            getIsolationLevel().setEnabled(true);
            getConnectButton().setText("Disconnect");
            getConnectButton().setEnabled(true);
        } else {
            getUrlComboBox().setEnabled(true);
            getBusy().setBackground(Color.GRAY);
            getBusy().setText("Disconnected");
            getCommitButton().setEnabled(false);
            getRollbackButton().setEnabled(false);
            getNextButton().setEnabled(false);
            getExecuteButton().setEnabled(false);
            getIsolationLevel().setEnabled(false);
            getConnectButton().setText("Connect");
            getConnectButton().setEnabled(notBlank(getUrlComboBox().getSelectedItem().toString()));
        }
    }

    private String getSelectedSQLStatement() {
        // Pattern p = Pattern.compile("('[^']*'|.)*;");
        if (notBlank(getSql().getSelectedText())) {
            return getSql().getSelectedText();
        }
        return getSql().getText();
        // p.matcher(getSql().getText()).toMatchResult().
    }

    private static boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }

    public JTextArea getSql() {
        return sql;
    }

    public JButton getConnectButton() {
        return connectButton;
    }

    public JComboBox<String> getUrlComboBox() {
        return urlComboBox;
    }

    public JLabel getBusy() {
        return busy;
    }

    protected JButton getNextButton() {
        return nextButton;
    }

    protected JButton getRollbackButton() {
        return rollbackButton;
    }

    protected JButton getUpdateButton() {
        return updateButton;
    }

    protected JButton getExecuteButton() {
        return executeButton;
    }

    protected JButton getCommitButton() {
        return commitButton;
    }

    protected JComboBox<IsolationLevel> getIsolationLevel() {
        return isolationLevel;
    }

    void setMnemonics(boolean right) {
        getExecuteButton().setMnemonic(right ? '6' : '1');
        getNextButton().setMnemonic(right ? '7' : '2');
        getUpdateButton().setMnemonic(right ? '8' : '3');
        getCommitButton().setMnemonic(right ? '9' : '4');
        getRollbackButton().setMnemonic(right ? '0' : '5');
    }
}
