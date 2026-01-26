package com.ibm.de103252.lockdemo;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class LockDemo {
    // Timing constants
    private static final int LOCK_UPDATE_INTERVAL_MS = 250;
    
    private JFrame frmDatabaseLockDemo;
    private SqlPanel leftSqlPanel;
    private SqlPanel rightSqlPanel;
    private JScrollPane scrollPane;
    private JPanel panel;
    private JSplitPane splitPane;
    private JTable lockDisplay;
    private JdbcRowSet lockRowSet;
    private volatile boolean running = true;

    /**
     * Launch the application.
     */
    public static void main(String[] args) throws Exception {
        Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
        prefs.keys();
        final LockDemo window = new LockDemo();
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down LockDemo...");
            window.shutdown();
        }));
        
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
            System.err.println("Failed to initialize lock display: " + e.getMessage());
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
        while (running) {
            if (lockRowSet != null) {
                try {
                    lockRowSet.execute();
                } catch (SQLException e) {
                    System.err.println("Error updating lock info: " + e.getMessage());
                }
            }
            try {
                Thread.sleep(LOCK_UPDATE_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Shutdown the application and clean up resources.
     */
    public void shutdown() {
        running = false;
        
        // Close lock rowset
        if (lockRowSet != null) {
            try {
                lockRowSet.close();
                System.out.println("Lock rowset closed");
            } catch (SQLException e) {
                System.err.println("Error closing lock rowset: " + e.getMessage());
            }
        }
        
        // Shutdown executors and close connections
        if (leftSqlPanel != null) {
            leftSqlPanel.shutdown();
        }
        if (rightSqlPanel != null) {
            rightSqlPanel.shutdown();
        }
        
        System.out.println("LockDemo shutdown complete");
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
        
        // Create menu bar
        createMenuBar();
        
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
        
        lockDisplay = new JTable();
        lockDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        lockDisplay.setFillsViewportHeight(true);
        scrollPane.setViewportView(lockDisplay);
        splitPane.setRightComponent(scrollPane);
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
                        lockRowSet.setUrl(leftSqlPanel.getUrlComboBox().getSelectedItem().toString());
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
        Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
        String theme = prefs.get("theme", "IntelliJ");
        applyTheme(theme);
    }
    
    /**
     * Apply a theme to the application.
     *
     * @param themeName The name of the theme: "Light", "Dark", or "IntelliJ"
     */
    private void applyTheme(String themeName) {
        try {
            switch (themeName) {
                case "Light":
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    break;
                case "Dark":
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    break;
                case "IntelliJ":
                default:
                    UIManager.setLookAndFeel(new FlatIntelliJLaf());
                    break;
            }
            
            // Update all components if frame exists
            if (frmDatabaseLockDemo != null) {
                SwingUtilities.updateComponentTreeUI(frmDatabaseLockDemo);
                frmDatabaseLockDemo.pack();
            }
            
            // Save preference
            Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
            prefs.put("theme", themeName);
            prefs.flush();
        } catch (Exception ex) {
            System.err.println("Failed to apply theme: " + themeName);
            ex.printStackTrace();
        }
    }
    
    /**
     * Create and configure the menu bar with File menu for script operations.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu for left panel
        JMenu fileMenuLeft = new JMenu("File (Left)");
        fileMenuLeft.setMnemonic(KeyEvent.VK_F);
        
        JMenuItem newItemLeft = new JMenuItem("New");
        newItemLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newItemLeft.addActionListener(e -> leftSqlPanel.newScript());
        fileMenuLeft.add(newItemLeft);
        
        JMenuItem openItemLeft = new JMenuItem("Open...");
        openItemLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItemLeft.addActionListener(e -> leftSqlPanel.openScript());
        fileMenuLeft.add(openItemLeft);
        
        JMenuItem saveItemLeft = new JMenuItem("Save");
        saveItemLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItemLeft.addActionListener(e -> leftSqlPanel.saveScript());
        fileMenuLeft.add(saveItemLeft);
        
        JMenuItem saveAsItemLeft = new JMenuItem("Save As...");
        saveAsItemLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItemLeft.addActionListener(e -> leftSqlPanel.saveScriptAs());
        fileMenuLeft.add(saveAsItemLeft);
        
        menuBar.add(fileMenuLeft);
        
        // File menu for right panel
        JMenu fileMenuRight = new JMenu("File (Right)");
        fileMenuRight.setMnemonic(KeyEvent.VK_R);
        
        JMenuItem newItemRight = new JMenuItem("New");
        newItemRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        newItemRight.addActionListener(e -> rightSqlPanel.newScript());
        fileMenuRight.add(newItemRight);
        
        JMenuItem openItemRight = new JMenuItem("Open...");
        openItemRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        openItemRight.addActionListener(e -> rightSqlPanel.openScript());
        fileMenuRight.add(openItemRight);
        
        JMenuItem saveItemRight = new JMenuItem("Save");
        saveItemRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
        saveItemRight.addActionListener(e -> rightSqlPanel.saveScript());
        fileMenuRight.add(saveItemRight);
        
        JMenuItem saveAsItemRight = new JMenuItem("Save As...");
        saveAsItemRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItemRight.addActionListener(e -> rightSqlPanel.saveScriptAs());
        fileMenuRight.add(saveAsItemRight);
        
        menuBar.add(fileMenuRight);
        
        // View menu for theme selection
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        
        JMenu themeMenu = new JMenu("Theme");
        themeMenu.setMnemonic(KeyEvent.VK_T);
        
        ButtonGroup themeGroup = new ButtonGroup();
        Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
        String currentTheme = prefs.get("theme", "IntelliJ");
        
        JRadioButtonMenuItem lightTheme = new JRadioButtonMenuItem("Light");
        lightTheme.setSelected("Light".equals(currentTheme));
        lightTheme.addActionListener(e -> applyTheme("Light"));
        themeGroup.add(lightTheme);
        themeMenu.add(lightTheme);
        
        JRadioButtonMenuItem intellijTheme = new JRadioButtonMenuItem("IntelliJ");
        intellijTheme.setSelected("IntelliJ".equals(currentTheme));
        intellijTheme.addActionListener(e -> applyTheme("IntelliJ"));
        themeGroup.add(intellijTheme);
        themeMenu.add(intellijTheme);
        
        JRadioButtonMenuItem darkTheme = new JRadioButtonMenuItem("Dark");
        darkTheme.setSelected("Dark".equals(currentTheme));
        darkTheme.addActionListener(e -> applyTheme("Dark"));
        themeGroup.add(darkTheme);
        themeMenu.add(darkTheme);
        
        viewMenu.add(themeMenu);
        menuBar.add(viewMenu);
        
        frmDatabaseLockDemo.setJMenuBar(menuBar);
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
