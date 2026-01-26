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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
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
import org.fife.ui.rtextarea.RTextScrollPane;

@SuppressWarnings("serial")
public class SqlPanel extends JPanel {
	private static final int AUTO_COMPLETION_DELAY_MS = 1000;
	/**
	 * @wbp.nonvisual location=85,473
	 */
	private final Executor executor = new Executor();
	private final ScriptManager scriptManager = new ScriptManager();
	private RSyntaxTextArea sql;
	private RTextScrollPane sqlScrollPane;
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

		// Create RSyntaxTextArea with SQL syntax highlighting
		sql = new RSyntaxTextArea(20, 60);
		sql.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
		sql.setCodeFoldingEnabled(true);
		sql.setAntiAliasingEnabled(true);
		sql.setToolTipText("Enter SQL text here");
		sql.setFont(new Font("Consolas", Font.PLAIN, 13));

		// Create scroll pane with line numbers
		sqlScrollPane = new RTextScrollPane(sql);
		sqlScrollPane.setLineNumbersEnabled(true);

		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.weighty = 30.0;
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 3;
		add(sqlScrollPane, gbc_scrollPane_1);

		// Setup auto-completion
		setupAutoCompletion();

		// Add SQL formatting keyboard shortcut
		setupSqlFormatting();
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
				executeButton
						.setEnabled(SqlPanel.this.getExecutor().isConnected() && notBlank(getSelectedSQLStatement()));
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
			getExecutor().connect(getUrlComboBox().getSelectedItem().toString());
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

	/*
	 * Enable or disable UI elements depending on connection status and whether a
	 * transaction is active.
	 */
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

	/**
	 * Get the ScriptManager instance.
	 *
	 * @return The script manager
	 */
	public ScriptManager getScriptManager() {
		return scriptManager;
	}

	/**
	 * Open a SQL script file and load it into the editor.
	 */
	public void openScript() {
		// Check if current content is modified
		if (scriptManager.isModified(sql.getText())) {
			int result = JOptionPane.showConfirmDialog(this,
					"Current script has unsaved changes. Do you want to save before opening a new file?",
					"Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				saveScript();
			} else if (result == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		JFileChooser fileChooser = createFileChooser();
		fileChooser.setDialogTitle("Open SQL Script");

		int returnValue = fileChooser.showOpenDialog(this);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			try {
				String content = scriptManager.loadScript(selectedFile);
				sql.setText(content);
				sql.setCaretPosition(0);
				JOptionPane.showMessageDialog(this, "Script loaded successfully: " + selectedFile.getName(), "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error loading script: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Save the current SQL script to a file. If no file is currently open, prompts
	 * for a file name.
	 */
	public void saveScript() {
		if (scriptManager.getCurrentFile() == null) {
			saveScriptAs();
		} else {
			try {
				scriptManager.save(sql.getText());
				JOptionPane.showMessageDialog(this, "Script saved successfully: " + scriptManager.getCurrentFileName(),
						"Success", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error saving script: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
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

		int returnValue = fileChooser.showSaveDialog(this);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();

			// Add .sql extension if not present
			if (!ScriptManager.isSqlFile(selectedFile)) {
				selectedFile = new File(selectedFile.getAbsolutePath() + ".sql");
			}

			// Check if file exists
			if (selectedFile.exists()) {
				int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Do you want to overwrite it?",
						"Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

				if (overwrite != JOptionPane.YES_OPTION) {
					return;
				}
			}

			try {
				scriptManager.saveScript(selectedFile, sql.getText());
				JOptionPane.showMessageDialog(this, "Script saved successfully: " + selectedFile.getName(), "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, "Error saving script: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Create a new empty script.
	 */
	public void newScript() {
		// Check if current content is modified
		if (scriptManager.isModified(sql.getText())) {
			int result = JOptionPane.showConfirmDialog(this,
					"Current script has unsaved changes. Do you want to save before creating a new file?",
					"Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

			if (result == JOptionPane.YES_OPTION) {
				saveScript();
			} else if (result == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		sql.setText("");
		scriptManager.clearCurrentFile();
	}

	/**
	 * Setup auto-completion for SQL keywords.
	 */
	private void setupAutoCompletion() {
		CompletionProvider provider = createCompletionProvider();
		AutoCompletion ac = new AutoCompletion(provider);
		ac.install(sql);
		ac.setAutoActivationEnabled(true);
		ac.setAutoActivationDelay(AUTO_COMPLETION_DELAY_MS);
	}

	/**
	 * Create completion provider with SQL keywords.
	 */
	private CompletionProvider createCompletionProvider() {
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		// SQL Keywords
		String[] keywords = { "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
				"CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON",
				"AS", "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL", "ORDER", "BY", "GROUP", "HAVING",
				"DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "UNION", "ALL", "BEGIN", "COMMIT", "ROLLBACK",
				"TRANSACTION", "WORK", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "INT",
				"INTEGER", "VARCHAR", "CHAR", "DATE", "TIME", "TIMESTAMP", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
				"BOOLEAN", "BLOB", "CLOB", "GRANT", "REVOKE", "WITH", "LOCK", "FOR", "SHARE", "NOWAIT" };

		for (String keyword : keywords) {
			provider.addCompletion(new BasicCompletion(provider, keyword));
			provider.addCompletion(new BasicCompletion(provider, keyword.toLowerCase()));
		}

		return provider;
	}

	/**
	 * Setup SQL formatting keyboard shortcut (Ctrl+Shift+F).
	 */
	private void setupSqlFormatting() {
		InputMap inputMap = sql.getInputMap();
		ActionMap actionMap = sql.getActionMap();

		KeyStroke formatKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

		inputMap.put(formatKey, "formatSQL");
		actionMap.put("formatSQL", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				formatSQL();
			}
		});
	}

	/**
	 * Format SQL text with basic indentation.
	 */
	private void formatSQL() {
		String text = sql.getText();
		if (text == null || text.trim().isEmpty()) {
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
				.replaceAll("(?m)^(WHERE|AND|OR|ON|SET|VALUES)", "  $1").trim();

		sql.setText(formatted);
		sql.setCaretPosition(0);
	}

	/**
	 * Get the SQL text area.
	 * 
	 * @return RSyntaxTextArea instance
	 */
	public RSyntaxTextArea getSqlTextArea() {
		return sql;
	}

	/**
	 * Create and configure a file chooser for SQL scripts.
	 * 
	 * @return Configured JFileChooser
	 */
	private JFileChooser createFileChooser() {
		JFileChooser fileChooser = new JFileChooser();

		// Set default directory to user's home directory
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

		// Add file filter for SQL files
		FileNameExtensionFilter filter = new FileNameExtensionFilter("SQL Scripts (*.sql, *.ddl, *.dml)",
				ScriptManager.getSqlFileExtensions());
		fileChooser.setFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(true);

		return fileChooser;
	}

	/**
	 * Shutdown the panel and clean up resources.
	 */
	public void shutdown() {
		if (executor != null) {
			executor.shutdown();
		}
	}
}