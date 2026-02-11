package com.ibm.de103252.lockdemo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class LockDemo {
	private static final Logger LOGGER;

	private static final int LOCK_UPDATE_INTERVAL_MS = 250;

	private JFrame frmDatabaseLockDemo;
	private SqlPanel leftSqlPanel;
	private SqlPanel rightSqlPanel;
	private JTable lockDisplay;
	private JdbcRowSet lockRowSet;
	private volatile boolean running = true;

    static {
        String path = LockDemo.class.getClassLoader()
                                    .getResource("logging.properties")
                                    .getFile();
        System.setProperty("java.util.logging.config.file", path);
    	System.out.println("Initializing logger: " + path);
        LOGGER = Logger.getLogger(LockDemo.class.getName());
    }

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws Exception {
		Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
		prefs.keys();
		final LockDemo window = new LockDemo();

		// Add shutdown hook for cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Shutting down LockDemo...");
			window.shutdown();
		}));

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
			LOGGER.log(Level.SEVERE, "Failed to initialize lock display", e);
			e.printStackTrace();
		}
		prefs.flush();
	}

	private static void startEmbeddedDerby() throws Exception {
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		Class.forName("org.apache.derby.jdbc.ClientDriver");
	}

	private void initLockDisplay() throws SQLException {
		new Thread(() -> updateLockInfo()).start();
	}

	private void updateLockInfo() {
		while (running) {
			if (lockRowSet != null) {
				try {
					lockRowSet.execute();
					// Analyze locks for conflict highlighting after each update
					if (lockDisplay != null) {
						SwingUtilities.invokeLater(() -> {
							lockDisplay.repaint();
						});
					}
				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, "Error updating lock info", e);
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
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Error closing lock rowset", e);
			}
		}

		// Shutdown executors and close connections
		if (leftSqlPanel != null) {
			leftSqlPanel.shutdown();
		}
		if (rightSqlPanel != null) {
			rightSqlPanel.shutdown();
		}

		LOGGER.info("LockDemo shutdown complete");
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
			frmDatabaseLockDemo.setIconImage(Toolkit.getDefaultToolkit().getImage(image));
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
		JSplitPane splitPane = new JSplitPane();
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
		JPanel panel = new JPanel();
		splitPane.setLeftComponent(panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0 };
		gbl_panel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);
		leftSqlPanel = new SqlPanel(true);
		GridBagConstraints gbc_leftSqlPanel = new GridBagConstraints();
		gbc_leftSqlPanel.weighty = 70.0;
		gbc_leftSqlPanel.fill = GridBagConstraints.BOTH;
		gbc_leftSqlPanel.insets = new Insets(0, 0, 0, 5);
		gbc_leftSqlPanel.gridx = 0;
		gbc_leftSqlPanel.gridy = 0;
		panel.add(leftSqlPanel, gbc_leftSqlPanel);
		rightSqlPanel = new SqlPanel(false);
		GridBagConstraints gbc_rightSqlPanel = new GridBagConstraints();
		gbc_rightSqlPanel.weighty = 70.0;
		gbc_rightSqlPanel.fill = GridBagConstraints.BOTH;
		gbc_rightSqlPanel.gridx = 1;
		gbc_rightSqlPanel.gridy = 0;
		panel.add(rightSqlPanel, gbc_rightSqlPanel);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resizeColumnWidth(lockDisplay);
				lockDisplay.repaint();
			}
		});
		
		// Prevent vertical resizing beyond content size
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setMinimumSize(new java.awt.Dimension(0, 100));

		// Create a panel to hold the legend and lock display
		JPanel lockPanel = new JPanel(new BorderLayout());

		if (false) {
			lockPanel.add(createLegendPanel(), BorderLayout.NORTH);
		}

		lockDisplay = new JTable();
		lockDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		lockDisplay.setFillsViewportHeight(true);
		lockDisplay.setFocusable(false);
		
		// Prevent table from growing beyond its preferred size
		lockDisplay.addPropertyChangeListener("model", evt -> {
			// Update preferred size based on content
			int rowHeight = lockDisplay.getRowHeight();
			int rowCount = lockDisplay.getRowCount();
			int headerHeight = lockDisplay.getTableHeader() != null ? lockDisplay.getTableHeader().getPreferredSize().height : 0;
			int preferredHeight = (rowCount * rowHeight) + headerHeight;
			
			// Set maximum height to prevent excessive vertical growth
			int maxHeight = Math.min(preferredHeight, 400);
			scrollPane.setPreferredSize(new java.awt.Dimension(scrollPane.getPreferredSize().width, maxHeight));
			scrollPane.revalidate();
		});

		// Set up lock highlighting
		lockDisplay.setDefaultRenderer(Object.class, new LockStateRenderer());

		scrollPane.setViewportView(lockDisplay);
		lockPanel.add(scrollPane, BorderLayout.CENTER);

		splitPane.setRightComponent(lockPanel);
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
						lockDisplay.setModel(new RowSetTableModel(lockRowSet));
					} else {
						lockRowSet.close();
						lockRowSet = null;
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	private JPanel createLegendPanel() {
		// Create legend panel
		JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		legendPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Add legend items
		legendPanel.add(createLegendItem("Conflict", LockStateRenderer.CONFLICT_COLOR));
		legendPanel.add(createLegendItem("Exclusive", LockStateRenderer.EXCLUSIVE_COLOR));
		legendPanel.add(createLegendItem("Shared", LockStateRenderer.SHARED_COLOR));
		legendPanel.add(createLegendItem("Waiting", LockStateRenderer.WAITING_COLOR));
		return legendPanel;
	}

	private void setLookAndFeel() {
		Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
		try {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
		}
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
				break;
			case "Dark":
				break;
			case "IntelliJ":
			default:
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
			LOGGER.log(Level.INFO, "Failed to apply theme: " + themeName, ex);
		}
	}

	/**
	 * Create and configure the menu bar with File menu for script operations.
	 */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		AbstractAction exitAction = new AbstractAction("Exit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				frmDatabaseLockDemo.setVisible(false);
				frmDatabaseLockDemo.dispose();
				System.exit(0);
			}
		};

		// File menu for left panel
		JMenu fileMenuLeft = new JMenu("File (Left)");
		fileMenuLeft.setMnemonic(KeyEvent.VK_L);
		fileMenuLeft.setDisplayedMnemonicIndex(6);

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
		saveAsItemLeft.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		saveAsItemLeft.addActionListener(e -> leftSqlPanel.saveScriptAs());
		fileMenuLeft.add(saveAsItemLeft);

		fileMenuLeft.add(exitAction);

		menuBar.add(fileMenuLeft);

		// File menu for right panel
		JMenu fileMenuRight = new JMenu("File (Right)");
		fileMenuRight.setMnemonic(KeyEvent.VK_R);

		JMenuItem newItemRight = new JMenuItem("New");
		newItemRight.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		newItemRight.addActionListener(e -> rightSqlPanel.newScript());
		fileMenuRight.add(newItemRight);

		JMenuItem openItemRight = new JMenuItem("Open...");
		openItemRight.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		openItemRight.addActionListener(e -> rightSqlPanel.openScript());
		fileMenuRight.add(openItemRight);

		JMenuItem saveItemRight = new JMenuItem("Save");
		saveItemRight.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));
		saveItemRight.addActionListener(e -> rightSqlPanel.saveScript());
		fileMenuRight.add(saveItemRight);

		JMenuItem saveAsItemRight = new JMenuItem("Save As...");
		saveAsItemRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		saveAsItemRight.addActionListener(e -> rightSqlPanel.saveScriptAs());
		fileMenuRight.add(saveAsItemRight);

		fileMenuRight.add(exitAction);

		menuBar.add(fileMenuRight);

		// View menu for theme selection
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);

		JMenu themeMenu = new JMenu("Theme");
		themeMenu.setMnemonic(KeyEvent.VK_T);

		ButtonGroup themeGroup = new ButtonGroup();
		for (LookAndFeelInfo laf: UIManager.getInstalledLookAndFeels()) {
			JRadioButtonMenuItem lafItem = new JRadioButtonMenuItem(laf.getName());
			lafItem.setAction(new AbstractAction(laf.getClassName()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						UIManager.setLookAndFeel((LookAndFeel) Class.forName((String) getValue(AbstractAction.NAME)).newInstance());
						SwingUtilities.updateComponentTreeUI(frmDatabaseLockDemo);
					} catch (IllegalAccessException | InstantiationException | ClassNotFoundException
							| UnsupportedLookAndFeelException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
			themeGroup.add(lafItem);
			themeMenu.add(lafItem);
			
		}
		menuBar.add(themeMenu);
		
		Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());

		frmDatabaseLockDemo.setJMenuBar(menuBar);
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

	/**
	 * Create a legend item showing a color and label.
	 */
	private JLabel createLegendItem(String text, Color color) {
		JLabel label = new JLabel("  " + text + "  ");
		label.setOpaque(true);
		label.setBackground(color);
		label.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		return label;
	}
}
