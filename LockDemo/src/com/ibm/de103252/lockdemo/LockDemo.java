package com.ibm.de103252.lockdemo;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JSplitPane;
import javax.swing.JPanel;

public class LockDemo {
    private JFrame frmDatabaseLockDemo;
    private SqlPanel leftSqlPanel;
    private SqlPanel rightSqlPanel;
    private JTextArea lockDisplay;
    private Executor lockExecutor;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JSplitPane splitPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) throws Exception {
        final LockDemo window = new LockDemo();
        startEmbeddedDerby();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    window.frmDatabaseLockDemo.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            window.initLockDisplay();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
        prefs.flush();
    }

    private static void startEmbeddedDerby() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
    }

    private void initLockDisplay() throws SQLException {
        lockExecutor = new Executor();
        new Thread(() -> updateLockInfo()).start();
    }

    private void updateLockInfo() {
        while (true) {
            final String locks = lockExecutor.getLocks();
            SwingUtilities.invokeLater(() -> {
                if (!getLockDisplay().getText().equals(locks)) {
                    getLockDisplay().setText(locks);
                }
            });
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }
        }
    }

    private Connection openConnection(String url) throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        conn.setAutoCommit(false);
        conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        return conn;
    }

    /**
     * Create the application.
     */
    public LockDemo() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmDatabaseLockDemo = new JFrame();
        frmDatabaseLockDemo.setTitle("Database lock demo");
        frmDatabaseLockDemo.setBounds(100, 100, 1045, 754);
        frmDatabaseLockDemo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        frmDatabaseLockDemo.getContentPane().setLayout(gridBagLayout);
        
        splitPane = new JSplitPane();
        splitPane.setResizeWeight(0.6);
        splitPane.setOneTouchExpandable(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        GridBagConstraints gbc_splitPane = new GridBagConstraints();
        gbc_splitPane.weighty = 70.0;
        gbc_splitPane.fill = GridBagConstraints.BOTH;
        gbc_splitPane.gridx = 0;
        gbc_splitPane.gridy = 0;
        frmDatabaseLockDemo.getContentPane().add(splitPane, gbc_splitPane);
        
        panel = new JPanel();
        splitPane.setLeftComponent(panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[]{0, 0, 0};
        gbl_panel.rowHeights = new int[]{0, 0};
        gbl_panel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
        gbl_panel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
        panel.setLayout(gbl_panel);
        leftSqlPanel = new SqlPanel();
        GridBagConstraints gbc_leftSqlPanel = new GridBagConstraints();
        gbc_leftSqlPanel.weighty = 70.0;
        gbc_leftSqlPanel.fill = GridBagConstraints.BOTH;
        gbc_leftSqlPanel.insets = new Insets(0, 0, 0, 5);
        gbc_leftSqlPanel.gridx = 0;
        gbc_leftSqlPanel.gridy = 0;
        panel.add(leftSqlPanel, gbc_leftSqlPanel);
        rightSqlPanel = new SqlPanel();
        GridBagConstraints gbc_rightSqlPanel = new GridBagConstraints();
        gbc_rightSqlPanel.weighty = 70.0;
        gbc_rightSqlPanel.fill = GridBagConstraints.BOTH;
        gbc_rightSqlPanel.gridx = 1;
        gbc_rightSqlPanel.gridy = 0;
        panel.add(rightSqlPanel, gbc_rightSqlPanel);
        scrollPane = new JScrollPane();
        splitPane.setRightComponent(scrollPane);
        lockDisplay = new JTextArea();
        lockDisplay.setToolTipText("Displays active locks in the database");
        scrollPane.setViewportView(lockDisplay);
        lockDisplay.setFont(new Font("Consolas", Font.PLAIN, 12));
        leftSqlPanel.getExecutor().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("connected") && ((boolean) evt.getNewValue()) == true) {
                    try {
                        lockExecutor.setConnection(LockDemo.this.openConnection(leftSqlPanel.getUrlTextField().getText()));
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public SqlPanel getLeftSqlPanel() {
        return leftSqlPanel;
    }

    public SqlPanel getRightSqlPanel() {
        return rightSqlPanel;
    }

    public JTextArea getLockDisplay() {
        return lockDisplay;
    }
}
