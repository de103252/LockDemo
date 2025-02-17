package com.ibm.de103252.lockdemo;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.formdev.flatlaf.FlatIntelliJLaf;

public class LockDemo {
    private JFrame frmDatabaseLockDemo;
    private SqlPanel leftSqlPanel;
    private SqlPanel rightSqlPanel;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JSplitPane splitPane;
    private JTable lockDisplay;
    private JdbcRowSet lockRowSet;

    /**
     * Launch the application.
     */
    public static void main(String[] args) throws Exception {
        Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
        prefs.keys();
        final LockDemo window = new LockDemo();
        startEmbeddedDerby();
        EventQueue.invokeLater(new Runnable() {
            @Override
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
        prefs.flush();
    }

    private static void startEmbeddedDerby() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
    }

    private void initLockDisplay() throws SQLException {
        new Thread(() -> updateLockInfo()).start();
    }

    private void updateLockInfo() {
        while (true) {
            if (lockRowSet != null) {
                try {
                    lockRowSet.execute();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }
        }
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
        setLookAndFeel();
        frmDatabaseLockDemo = new JFrame();
        try {
            URL image = getClass().getResource("/lock.png");
            frmDatabaseLockDemo.setIconImage(
                    Toolkit.getDefaultToolkit().getImage(image));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        frmDatabaseLockDemo.setTitle("Database lock demo");
        frmDatabaseLockDemo.setBounds(100, 100, 915, 572);
        frmDatabaseLockDemo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[] { 0, 0 };
        gridBagLayout.rowHeights = new int[] { 0, 0 };
        gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        frmDatabaseLockDemo.getContentPane().setLayout(gridBagLayout);
        splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setResizeWeight(0.8);
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
        gbl_panel.columnWidths = new int[] { 0, 0, 0 };
        gbl_panel.rowHeights = new int[] { 0, 0 };
        gbl_panel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
        gbl_panel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
        panel.setLayout(gbl_panel);
        leftSqlPanel = new SqlPanel();
        leftSqlPanel.setMnemonics(false);
        GridBagConstraints gbc_leftSqlPanel = new GridBagConstraints();
        gbc_leftSqlPanel.weighty = 70.0;
        gbc_leftSqlPanel.fill = GridBagConstraints.BOTH;
        gbc_leftSqlPanel.insets = new Insets(0, 0, 0, 5);
        gbc_leftSqlPanel.gridx = 0;
        gbc_leftSqlPanel.gridy = 0;
        panel.add(leftSqlPanel, gbc_leftSqlPanel);
        rightSqlPanel = new SqlPanel();
        rightSqlPanel.setMnemonics(true);
        GridBagConstraints gbc_rightSqlPanel = new GridBagConstraints();
        gbc_rightSqlPanel.weighty = 70.0;
        gbc_rightSqlPanel.fill = GridBagConstraints.BOTH;
        gbc_rightSqlPanel.gridx = 1;
        gbc_rightSqlPanel.gridy = 0;
        panel.add(rightSqlPanel, gbc_rightSqlPanel);
        scrollPane = new JScrollPane();
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeColumnWidth(lockDisplay);
                lockDisplay.repaint();
            }
        });
        splitPane.setRightComponent(scrollPane);
        lockDisplay = new JTable();
        lockDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        lockDisplay.setFillsViewportHeight(true);
        scrollPane.setViewportView(lockDisplay);
        leftSqlPanel.getExecutor().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("connected")) {
                    connected((boolean) evt.getNewValue());
                }
            }

            private void connected(boolean connected) {
                try {
                    if (connected) {
                        lockRowSet = RowSetProvider.newFactory().createJdbcRowSet();
                        lockRowSet.setUrl(leftSqlPanel.getUrlTextField().getSelectedItem().toString());
                        lockRowSet.setCommand(leftSqlPanel.getExecutor().getLockSQL());
                        lockRowSet.execute();
                        getLockDisplay().setModel(new RowSetTableModel(lockRowSet));
                    } else {
                        lockRowSet.close();
                    }
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
    }

    public SqlPanel getLeftSqlPanel() {
        return leftSqlPanel;
    }

    public SqlPanel getRightSqlPanel() {
        return rightSqlPanel;
    }

    public JTable getLockDisplay() {
        return lockDisplay;
    }

    public static void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 15; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 1, width);
                width = Math.max(table.getColumnModel().getColumn(column).getPreferredWidth(), width);
            }
            width = Math.min(width, 300);
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }
}
