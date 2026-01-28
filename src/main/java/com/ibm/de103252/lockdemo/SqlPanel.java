package com.ibm.de103252.lockdemo;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * SQL Panel component for executing SQL statements and managing database connections.
 * Provides a rich text editor with syntax highlighting, auto-completion, and transaction management.
 * 
 * <p>Features:
 * <ul>
 *   <li>SQL syntax highlighting and code folding</li>
 *   <li>Auto-completion for SQL keywords</li>
 *   <li>Transaction isolation level management</li>
 *   <li>Script loading and saving</li>
 *   <li>SQL formatting (Ctrl+Shift+F)</li>
 * </ul>
 * 
 * @author IBM
 * @version 2.0
 */
@SuppressWarnings("serial")
public class SqlPanel extends JPanel {
	
	// Constants
	private static final Logger LOGGER = Logger.getLogger(SqlPanel.class.getName());
	private static final int AUTO_COMPLETION_DELAY_MS = 1000;
	private static final String FONT_NAME = "Consolas";
	private static final int FONT_SIZE = 13;
	private static final int SQL_EDITOR_ROWS = 20;
	private static final int SQL_EDITOR_COLS = 60;
	private static final String PREF_NODE_NAME = LockDemo.class.getName();
	private static final String PREF_LAST_USED_URL = "lastUsedUrl";
	private static final String PREF_URL_PREFIX = "url.";
	
	// Status colors
	private static final Color COLOR_DISCONNECTED = Color.GRAY;
	private static final Color COLOR_CONNECTED = Color.GREEN;
	private static final Color COLOR_IN_TRANSACTION = Color.YELLOW;
	private static final Color COLOR_BUSY = Color.RED;
	
	// Default JDBC URLs
	private static final String[] DEFAULT_URLS = {
		"jdbc:derby:memory:derbyDB;create=true",
		"jdbc:derby:derbyDB;create=true",
		"jdbc:derby://localhost:1527/DBIDB"
	};
	
	static {
		FoldParserManager.get().addFoldParserMapping(
			SyntaxConstants.SYNTAX_STYLE_SQL, 
			new SqlFoldParser(true)
		);
	}

	// Instance fields
	private final Executor executor = new Executor();
	private final ScriptManager scriptManager = new ScriptManager();
	private final boolean isLeftPanel;
	
	// UI Components
	private RSyntaxTextArea sqlEditor;
	private JTextArea resultArea;
	private JComboBox<String> urlComboBox;
	private JComboBox<IsolationLevel> isolationLevelComboBox;
	private JLabel statusLabel;
	
	// Actions
	private Action executeAction;
	private Action nextAction;
	private Action commitAction;
	private Action updateAction;
	private Action rollbackAction;
	private Action connectAction;

	/**
	 * Abstract base class for SQL-related actions with logging support.
	 */
	private abstract class SqlAction extends AbstractAction {
		public SqlAction(String name) {
			super(name);
		}

		protected void logAction(String message, Object... args) {
			String formattedMessage = String.format(message, args);
			LOGGER.info(formattedMessage);
		}
	}

	/**
	 * Creates a new SQL Panel.
	 * 
	 * @param left true if this is the left panel (affects keyboard shortcuts)
	 */
	public SqlPanel(boolean left) {
		this.isLeftPanel = left;
		initializeComponents();
		setupLayout();
		setupListeners();
		initializeControls();
		updateControlStates();
	}

	/**
	 * Initialize all UI components.
	 */
	private void initializeComponents() {
		createSqlEditor();
		createResultArea();
		createUrlComboBox();
		createIsolationLevelComboBox();
		createStatusLabel();
		createActions();
	}

	/**
	 * Create and configure the SQL editor.
	 */
	private void createSqlEditor() {
		sqlEditor = new RSyntaxTextArea(SQL_EDITOR_ROWS, SQL_EDITOR_COLS);
		sqlEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		sqlEditor.setCodeFoldingEnabled(true);
		sqlEditor.setAntiAliasingEnabled(true);
		sqlEditor.setToolTipText("Enter SQL text here");
		sqlEditor.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
		setupSqlFormatting();
	}

	/**
	 * Create and configure the result display area.
	 */
	private void createResultArea() {
		resultArea = new JTextArea();
		resultArea.setToolTipText("Displays SQL results");
		resultArea.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
		resultArea.setEditable(false);
	}

	/**
	 * Create and configure the URL combo box.
	 */
	private void createUrlComboBox() {
		urlComboBox = new JComboBox<>();
		urlComboBox.setEditable(true);
	}

	/**
	 * Create and configure the isolation level combo box.
	 */
	private void createIsolationLevelComboBox() {
		isolationLevelComboBox = new JComboBox<>();
		isolationLevelComboBox.setToolTipText("Select isolation level for next transaction");
		isolationLevelComboBox.setModel(new DefaultComboBoxModel<>(IsolationLevel.values()));
		isolationLevelComboBox.setSelectedIndex(IsolationLevel.ReadCommitted.ordinal());
	}

	/**
	 * Create and configure the status label.
	 */
	private void createStatusLabel() {
		statusLabel = new JLabel("Disconnected");
		statusLabel.setOpaque(true);
		statusLabel.setBackground(COLOR_DISCONNECTED);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
	}

	/**
	 * Create all action objects.
	 */
	private void createActions() {
		createConnectAction();
		createExecuteAction();
		createNextAction();
		createCommitAction();
		createUpdateAction();
		createRollbackAction();
	}

	/**
	 * Create the connect/disconnect action.
	 */
	private void createConnectAction() {
		connectAction = new SqlAction("Connect") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String action = executor.isConnected() ? "Disconnect" : "Connect";
				String url = getSelectedUrl();
				logAction("%s %s", action, url != null ? "to " + url : "");
				connectOrDisconnect();
			}
		};
	}

	/**
	 * Create the execute SQL action.
	 */
	private void createExecuteAction() {
		executeAction = new SqlAction("Execute") {
			@Override
			public void actionPerformed(ActionEvent e) {
				IsolationLevel selectedIsolation = getSelectedIsolationLevel();
				String sql = getSelectedSQLStatement();
				
				if (selectedIsolation == null || sql == null) {
					return;
				}
				
				logAction("Run SQL statement in isolation level %s:%n%s", 
						selectedIsolation, sql);
				executor.setIsolationLevel(selectedIsolation.getIsolation());
				executor.execute(sql);
				updateControlStates();
			}
		};
	}

	/**
	 * Create the next row action.
	 */
	private void createNextAction() {
		nextAction = new SqlAction("Next") {
			@Override
			public void actionPerformed(ActionEvent e) {
				logAction("Next");
				executor.next();
				updateControlStates();
			}
		};
	}

	/**
	 * Create the commit action.
	 */
	private void createCommitAction() {
		commitAction = new SqlAction("Commit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				logAction("Commit");
				executor.commit();
			}
		};
	}

	/**
	 * Create the update row action.
	 */
	private void createUpdateAction() {
		updateAction = new SqlAction("Update") {
			@Override
			public void actionPerformed(ActionEvent e) {
				logAction("Update current row");
				updateAction.setEnabled(false);
				executor.update();
			}
		};
	}

	/**
	 * Create the rollback action.
	 */
	private void createRollbackAction() {
		rollbackAction = new SqlAction("Rollback") {
			@Override
			public void actionPerformed(ActionEvent e) {
				logAction("Rollback");
				executor.rollback();
			}
		};
	}

	/**
	 * Setup the panel layout and add all components.
	 */
	private void setupLayout() {
		setLayout(createMainLayout());
		
		addConnectionPanel();
		addStatusLabel();
		addIsolationPanel();
		addSqlEditorPanel();
		addResultPanel();
		addButtonPanel();
	}

	/**
	 * Create the main GridBagLayout.
	 */
	private GridBagLayout createMainLayout() {
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 600, 0 };
		layout.rowHeights = new int[] { 0, 27, 7, 113, 33, 0, 0, 0 };
		layout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		layout.rowWeights = new double[] { 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		return layout;
	}

	/**
	 * Add the connection panel with URL combo box and connect button.
	 */
	private void addConnectionPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 201, 86, 0 };
		layout.rowHeights = new int[] { 23, 0 };
		layout.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		layout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel.setLayout(layout);
		
		// Add URL combo box
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(urlComboBox, gbc);
		
		// Add connect button
		JButton connectButton = new JButton(connectAction);
		connectButton.setToolTipText("Connect to the data source");
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridx = 1;
		gbc.gridy = 0;
		panel.add(connectButton, gbc);
		
		// Add panel to main layout
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(panel, gbc);
	}

	/**
	 * Add the status label to the layout.
	 */
	private void addStatusLabel() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 1;
		add(statusLabel, gbc);
	}

	/**
	 * Add the isolation level selection panel.
	 */
	private void addIsolationPanel() {
		JPanel panel = new JPanel();
		
		JLabel label = new JLabel("Isolation");
		label.setDisplayedMnemonic('I');
		label.setLabelFor(isolationLevelComboBox);
		panel.add(label);
		panel.add(isolationLevelComboBox);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 2;
		add(panel, gbc);
	}

	/**
	 * Add the SQL editor panel with scroll pane.
	 */
	private void addSqlEditorPanel() {
		RTextScrollPane scrollPane = new RTextScrollPane(sqlEditor);
		scrollPane.setLineNumbersEnabled(true);
		scrollPane.setFoldIndicatorEnabled(true);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weighty = 30.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 3;
		add(scrollPane, gbc);
	}

	/**
	 * Add the result display panel.
	 */
	private void addResultPanel() {
		JScrollPane scrollPane = new JScrollPane(resultArea);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weighty = 120.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 4;
		add(scrollPane, gbc);
	}

	/**
	 * Add the button panel with all action buttons.
	 */
	private void addButtonPanel() {
		JPanel panel = new JPanel();
		FlowLayout layout = (FlowLayout) panel.getLayout();
		layout.setVgap(3);
		
		// Add buttons with mnemonics
		panel.add(createButton(executeAction, "Execute selected SQL statement", 
				isLeftPanel ? KeyEvent.VK_1 : KeyEvent.VK_6));
		panel.add(createButton(nextAction, "Move result set to next row", 
				isLeftPanel ? KeyEvent.VK_2 : KeyEvent.VK_7));
		panel.add(createButton(commitAction, "Commit the current transaction", 
				isLeftPanel ? KeyEvent.VK_3 : KeyEvent.VK_8));
		panel.add(createButton(updateAction, "Update row that the current result set is positioned on", 
				isLeftPanel ? KeyEvent.VK_4 : KeyEvent.VK_9));
		panel.add(createButton(rollbackAction, "Roll the current transaction back", 
				isLeftPanel ? KeyEvent.VK_5 : KeyEvent.VK_0));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 5;
		add(panel, gbc);
	}

	/**
	 * Create a button with tooltip and mnemonic.
	 */
	private JButton createButton(Action action, String tooltip, int mnemonic) {
		JButton button = new JButton(action);
		button.setToolTipText(tooltip);
		button.setMnemonic(mnemonic);
		return button;
	}

	/**
	 * Setup all event listeners.
	 */
	private void setupListeners() {
		setupExecutorListener();
		setupCaretListener();
	}

	/**
	 * Setup the executor property change listener.
	 */
	private void setupExecutorListener() {
		executor.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("result".equals(evt.getPropertyName())) {
					updateResultArea(evt.getNewValue());
				}
				updateControlStates();
			}
		});
	}

	/**
	 * Setup the caret listener for the SQL editor.
	 */
	private void setupCaretListener() {
		sqlEditor.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				boolean hasSelection = isNotBlank(getSelectedSQLStatement());
				executeAction.setEnabled(executor.isConnected() && hasSelection);
			}
		});
	}

	/**
	 * Update the result area with new content.
	 */
	private void updateResultArea(Object newValue) {
		if (newValue != null) {
			String text = newValue.toString();
			resultArea.setText(text);
			resultArea.setCaretPosition(text.length());
		}
	}

	/**
	 * Connect or disconnect based on current state.
	 */
	private void connectOrDisconnect() {
		if (executor.isConnected()) {
			executor.disconnect();
		} else {
			String url = getSelectedUrl();
			if (url != null) {
				executor.connect(url);
				setupAutoCompletion();
				saveUrlToPreferences(url);
			}
		}
	}

	/**
	 * Save the URL to preferences.
	 */
	private void saveUrlToPreferences(String url) {
		try {
			updatePreferences(url);
		} catch (BackingStoreException e) {
			LOGGER.log(Level.WARNING, "Failed to save URL to preferences", e);
		}
	}

	/**
	 * Update preferences with the selected URL.
	 */
	private void updatePreferences(String selectedUrl) throws BackingStoreException {
		Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
		
		// Store the most recently used URL
		prefs.put(PREF_LAST_USED_URL, selectedUrl);
		
		// Check if this URL already exists in the list
		if (!urlExistsInPreferences(prefs, selectedUrl)) {
			addUrlToPreferences(prefs, selectedUrl);
		}
		
		prefs.flush();
	}

	/**
	 * Check if a URL exists in preferences.
	 */
	private boolean urlExistsInPreferences(Preferences prefs, String url) throws BackingStoreException {
		for (String key : prefs.keys()) {
			if (key.startsWith(PREF_URL_PREFIX)) {
				String storedUrl = prefs.get(key, "");
				if (storedUrl.equals(url)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add a new URL to preferences.
	 */
	private void addUrlToPreferences(Preferences prefs, String url) throws BackingStoreException {
		int maxIndex = findMaxUrlIndex(prefs);
		prefs.put(PREF_URL_PREFIX + (maxIndex + 1), url);
	}

	/**
	 * Find the maximum URL index in preferences.
	 */
	private int findMaxUrlIndex(Preferences prefs) throws BackingStoreException {
		int maxIndex = 0;
		for (String key : prefs.keys()) {
			if (key.startsWith(PREF_URL_PREFIX)) {
				try {
					int index = Integer.parseInt(key.substring(PREF_URL_PREFIX.length()));
					maxIndex = Math.max(maxIndex, index);
				} catch (NumberFormatException e) {
					LOGGER.log(Level.FINE, "Ignoring malformed preference key: " + key, e);
				}
			}
		}
		return maxIndex;
	}

	/**
	 * Create completion provider with SQL keywords.
	 */
	private CompletionProvider createCompletionProvider() {
		DefaultCompletionProvider provider = new DefaultCompletionProvider();
		
		String[] keywords = executor.getSQLKeywords();
		for (String keyword : keywords) {
			provider.addCompletion(new BasicCompletion(provider, keyword.toLowerCase()));
		}
		
		return provider;
	}

	/**
	 * Create and configure a file chooser for SQL scripts.
	 * 
	 * @return Configured JFileChooser
	 */
	private JFileChooser createFileChooser() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(ScriptManager.getLastDirectory());
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
			"SQL Scripts (*.sql, *.ddl, *.dml)",
			ScriptManager.getSqlFileExtensions()
		);
		fileChooser.setFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(true);
		
		return fileChooser;
	}

	/**
	 * Format SQL text with basic indentation.
	 */
	private void formatSQL() {
		String text = sqlEditor.getText();
		if (!isNotBlank(text)) {
			return;
		}

		// Basic SQL formatting
		String formatted = text
			// Add newlines before major keywords
			.replaceAll("(?i)\\s+(SELECT|FROM|WHERE|JOIN|INNER|LEFT|RIGHT|OUTER|ON|GROUP BY|ORDER BY|HAVING|UNION)",
					"\n$1")
			.replaceAll("(?i)\\s+(INSERT|INTO|VALUES|UPDATE|SET|DELETE)", "\n$1")
			.replaceAll("(?i)\\s+(BEGIN|COMMIT|ROLLBACK)", "\n$1")
			// Clean up multiple spaces
			.replaceAll("\\s+", " ")
			// Clean up multiple newlines
			.replaceAll("\\n+", "\n")
			// Trim each line
			.replaceAll("(?m)^\\s+|\\s+$", "")
			// Add indentation for certain keywords
			.replaceAll("(?m)^(WHERE|AND|OR|ON|SET|VALUES)", "  $1")
			.trim();

		sqlEditor.setText(formatted);
		sqlEditor.setCaretPosition(0);
	}

	/**
	 * Get the selected SQL statement from the editor.
	 */
	private String getSelectedSQLStatement() {
		return sqlEditor.getSelectedText();
	}

	/**
	 * Get the selected URL from the combo box.
	 */
	private String getSelectedUrl() {
		Object selected = urlComboBox.getSelectedItem();
		return selected != null ? selected.toString() : null;
	}

	/**
	 * Get the selected isolation level.
	 */
	private IsolationLevel getSelectedIsolationLevel() {
		return (IsolationLevel) isolationLevelComboBox.getSelectedItem();
	}

	/**
	 * Initialize controls with saved preferences.
	 */
	private void initializeControls() {
		loadUrlsFromPreferences();
	}

	/**
	 * Load URLs from preferences into the combo box.
	 */
	private void loadUrlsFromPreferences() {
		Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		String lastUsedUrl = null;
		
		try {
			lastUsedUrl = prefs.get(PREF_LAST_USED_URL, null);
			
			// Load all saved URLs
			for (String key : prefs.keys()) {
				if (key.startsWith(PREF_URL_PREFIX)) {
					model.addElement(prefs.get(key, ""));
				}
			}
		} catch (BackingStoreException e) {
			LOGGER.log(Level.WARNING, "Failed to load URLs from preferences", e);
		}
		
		// Add default URLs if none saved
		if (model.getSize() == 0) {
			for (String url : DEFAULT_URLS) {
				model.addElement(url);
			}
		}
		
		urlComboBox.setModel(model);
		
		// Select the last used URL if it exists
		selectLastUsedUrl(model, lastUsedUrl);
	}

	/**
	 * Select the last used URL in the combo box.
	 */
	private void selectLastUsedUrl(DefaultComboBoxModel<String> model, String lastUsedUrl) {
		if (lastUsedUrl != null) {
			for (int i = 0; i < model.getSize(); i++) {
				if (lastUsedUrl.equals(model.getElementAt(i))) {
					urlComboBox.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	/**
	 * Create a new empty script.
	 */
	public void newScript() {
		if (!checkUnsavedChanges("creating a new file")) {
			return;
		}
		
		sqlEditor.setText("");
		scriptManager.clearCurrentFile();
	}

	/**
	 * Open a SQL script file and load it into the editor.
	 */
	public void openScript() {
		if (!checkUnsavedChanges("opening a new file")) {
			return;
		}
		
		JFileChooser fileChooser = createFileChooser();
		fileChooser.setDialogTitle("Open SQL Script");
		
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			loadScriptFile(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Load a script file into the editor.
	 */
	private void loadScriptFile(File file) {
		try {
			String content = scriptManager.loadScript(file);
			sqlEditor.setText(content);
			sqlEditor.setCaretPosition(0);
		} catch (IOException e) {
			showErrorDialog("Error loading script: " + e.getMessage());
			LOGGER.log(Level.SEVERE, "Failed to load script file: " + file, e);
		}
	}

	/**
	 * Save the current SQL script to a file.
	 */
	public void saveScript() {
		if (scriptManager.getCurrentFile() == null) {
			saveScriptAs();
		} else {
			saveCurrentScript();
		}
	}

	/**
	 * Save the current script to its file.
	 */
	private void saveCurrentScript() {
		try {
			scriptManager.save(sqlEditor.getText());
			showInfoDialog("Script saved successfully: " + scriptManager.getCurrentFileName());
		} catch (IOException e) {
			showErrorDialog("Error saving script: " + e.getMessage());
			LOGGER.log(Level.SEVERE, "Failed to save script", e);
		}
	}

	/**
	 * Save the current SQL script to a new file.
	 */
	public void saveScriptAs() {
		JFileChooser fileChooser = createFileChooser();
		fileChooser.setDialogTitle("Save SQL Script As");
		
		// Suggest a default name if current file exists
		if (scriptManager.getCurrentFile() != null) {
			fileChooser.setSelectedFile(scriptManager.getCurrentFile());
		}
		
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			saveScriptToFile(fileChooser.getSelectedFile());
		}
	}

	/**
	 * Save the script to a specific file.
	 */
	private void saveScriptToFile(File file) {
		// Add .sql extension if not present
		if (!ScriptManager.isSqlFile(file)) {
			file = new File(file.getAbsolutePath() + ".sql");
		}
		
		// Check if file exists
		if (file.exists() && !confirmOverwrite()) {
			return;
		}
		
		try {
			scriptManager.saveScript(file, sqlEditor.getText());
			showInfoDialog("Script saved successfully: " + file.getName());
		} catch (IOException e) {
			showErrorDialog("Error saving script: " + e.getMessage());
			LOGGER.log(Level.SEVERE, "Failed to save script to file: " + file, e);
		}
	}

	/**
	 * Check for unsaved changes and prompt user.
	 * 
	 * @param action The action being performed (e.g., "opening a new file")
	 * @return true if it's safe to proceed, false if user cancelled
	 */
	private boolean checkUnsavedChanges(String action) {
		if (!scriptManager.isModified(sqlEditor.getText())) {
			return true;
		}
		
		int result = JOptionPane.showConfirmDialog(
			this,
			"Current script has unsaved changes. Do you want to save before " + action + "?",
			"Unsaved Changes",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		
		if (result == JOptionPane.YES_OPTION) {
			saveScript();
			return true;
		}
		
		return result != JOptionPane.CANCEL_OPTION;
	}

	/**
	 * Confirm file overwrite.
	 */
	private boolean confirmOverwrite() {
		int result = JOptionPane.showConfirmDialog(
			this,
			"File already exists. Do you want to overwrite it?",
			"Confirm Overwrite",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		return result == JOptionPane.YES_OPTION;
	}

	/**
	 * Show an error dialog.
	 */
	private void showErrorDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Show an info dialog.
	 */
	private void showInfoDialog(String message) {
		JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Setup auto-completion for SQL keywords.
	 */
	private void setupAutoCompletion() {
		CompletionProvider provider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(provider);
		ac.install(sqlEditor);
		ac.setAutoActivationEnabled(true);
		ac.setAutoActivationDelay(AUTO_COMPLETION_DELAY_MS);
	}

	/**
	 * Setup SQL formatting keyboard shortcut (Ctrl+Shift+F).
	 */
	private void setupSqlFormatting() {
		InputMap inputMap = sqlEditor.getInputMap();
		ActionMap actionMap = sqlEditor.getActionMap();
		
		KeyStroke formatKey = KeyStroke.getKeyStroke(
			KeyEvent.VK_F, 
			KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK
		);
		
		inputMap.put(formatKey, "formatSQL");
		actionMap.put("formatSQL", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				formatSQL();
			}
		});
	}

	/**
	 * Shutdown the panel and clean up resources.
	 */
	public void shutdown() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	/**
	 * Update UI control states based on connection and transaction status.
	 */
	private void updateControlStates() {
		ConnectionState state = determineConnectionState();
		applyConnectionState(state);
	}

	/**
	 * Determine the current connection state.
	 */
	private ConnectionState determineConnectionState() {
		if (executor.isBusy()) {
			return ConnectionState.BUSY;
		} else if (executor.isInTransaction()) {
			return ConnectionState.IN_TRANSACTION;
		} else if (executor.isConnected()) {
			return ConnectionState.CONNECTED;
		} else {
			return ConnectionState.DISCONNECTED;
		}
	}

	/**
	 * Apply the connection state to UI controls.
	 */
	private void applyConnectionState(ConnectionState state) {
		switch (state) {
			case BUSY:
				applyBusyState();
				break;
			case IN_TRANSACTION:
				applyInTransactionState();
				break;
			case CONNECTED:
				applyConnectedState();
				break;
			case DISCONNECTED:
				applyDisconnectedState();
				break;
		}
	}

	/**
	 * Apply busy state to controls.
	 */
	private void applyBusyState() {
		urlComboBox.setEnabled(false);
		statusLabel.setBackground(COLOR_BUSY);
		statusLabel.setText("Busy: " + executor.getCurrentXid());
		setAllActionsEnabled(false);
		isolationLevelComboBox.setEnabled(false);
	}

	/**
	 * Apply in-transaction state to controls.
	 */
	private void applyInTransactionState() {
		urlComboBox.setEnabled(false);
		statusLabel.setBackground(COLOR_IN_TRANSACTION);
		statusLabel.setText("In Tx: " + executor.getCurrentXid());
		
		nextAction.setEnabled(executor.isResultSetOpen());
		updateAction.setEnabled(executor.isResultSetPositioned());
		executeAction.setEnabled(isNotBlank(getSelectedSQLStatement()));
		commitAction.setEnabled(true);
		rollbackAction.setEnabled(true);
		connectAction.setEnabled(true);
		isolationLevelComboBox.setEnabled(false);
	}

	/**
	 * Apply connected state to controls.
	 */
	private void applyConnectedState() {
		urlComboBox.setEnabled(false);
		statusLabel.setBackground(COLOR_CONNECTED);
		statusLabel.setText("Connected");
		
		commitAction.setEnabled(false);
		rollbackAction.setEnabled(false);
		nextAction.setEnabled(false);
		updateAction.setEnabled(false);
		executeAction.setEnabled(isNotBlank(getSelectedSQLStatement()));
		
		isolationLevelComboBox.setSelectedItem(executor.getIsolationLevel());
		isolationLevelComboBox.setEnabled(true);
		
		connectAction.putValue(Action.NAME, "Disconnect");
		connectAction.setEnabled(true);
	}

	/**
	 * Apply disconnected state to controls.
	 */
	private void applyDisconnectedState() {
		urlComboBox.setEnabled(true);
		statusLabel.setBackground(COLOR_DISCONNECTED);
		statusLabel.setText("Disconnected");
		
		commitAction.setEnabled(false);
		rollbackAction.setEnabled(false);
		nextAction.setEnabled(false);
		executeAction.setEnabled(false);
		updateAction.setEnabled(false);
		isolationLevelComboBox.setEnabled(false);
		
		connectAction.putValue(Action.NAME, "Connect");
		connectAction.setEnabled(isNotBlank(getSelectedUrl()));
	}

	/**
	 * Enable or disable all actions.
	 */
	private void setAllActionsEnabled(boolean enabled) {
		nextAction.setEnabled(enabled);
		executeAction.setEnabled(enabled);
		commitAction.setEnabled(enabled);
		rollbackAction.setEnabled(enabled);
		updateAction.setEnabled(enabled);
		connectAction.setEnabled(enabled);
	}

	/**
	 * Check if a string is not blank.
	 */
	private static boolean isNotBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

	// Public getters for external access
	
	public Executor getExecutor() {
		return executor;
	}

	public ScriptManager getScriptManager() {
		return scriptManager;
	}

	public RSyntaxTextArea getSqlTextArea() {
		return sqlEditor;
	}

	public JComboBox<String> getUrlComboBox() {
		return urlComboBox;
	}

	public JComboBox<IsolationLevel> getIsolationLevel() {
		return isolationLevelComboBox;
	}

	public JLabel getBusy() {
		return statusLabel;
	}

	/**
	 * Enum representing connection states.
	 */
	private enum ConnectionState {
		DISCONNECTED,
		CONNECTED,
		IN_TRANSACTION,
		BUSY
	}
}

// Made with Bob
