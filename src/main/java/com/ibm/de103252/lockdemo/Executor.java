package com.ibm.de103252.lockdemo;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.SMALLINT;
import static java.util.function.Predicate.not;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.event.SwingPropertyChangeSupport;

public class Executor {
	private final Logger LOGGER;
	/**
	 * A list of SQL numeric type codes used to determine column alignment when
	 * formatting result set output. Numeric columns are right-aligned; all other
	 * columns are left-aligned.
	 */
	private static final List<Integer> NUMERIC_TYPES = Arrays.asList(DECIMAL, DOUBLE, FLOAT, INTEGER, SMALLINT, BIGINT);

	public Executor(boolean left) {
		LOGGER = Logger.getLogger(left ? "L" : "R");
	}

	/**
	 * Upper limit for the padded width of a column. Some databases report huge
	 * display sizes (H2 reports 1,000,000,000 for the VARCHAR columns of
	 * INFORMATION_SCHEMA), and padding to that width would exhaust memory.
	 */
	private static final int MAX_COLUMN_WIDTH = 60;

	/**
	 * Returns the display width for a result set column, capped at
	 * {@link #MAX_COLUMN_WIDTH}. The width is the larger of the database-reported
	 * display size and the length of the column label, so that the header is never
	 * wider than the column.
	 *
	 * @param rsmd the result set metadata
	 * @param i    the 1-based column index
	 * @return the display width to use for formatting
	 * @throws SQLException if the metadata cannot be read
	 */
	private static int columnWidth(ResultSetMetaData rsmd, int i) throws SQLException {
		return Math.min(MAX_COLUMN_WIDTH, Math.max(rsmd.getColumnDisplaySize(i), rsmd.getColumnLabel(i).length()));
	}

	/**
	 * Return a string representation of the column value.
	 *
	 * @param rs
	 * @param i  Index of column
	 * @return A string representation, justified right or left depending on the
	 *         column type, and padded to the recommended display length.
	 */
	private static String colToString(ResultSet rs, int i) {
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			String align = NUMERIC_TYPES.contains(rsmd.getColumnType(i)) ? "" : "-";
			int colWidth = columnWidth(rsmd, i);
			return String.format("%" + align + colWidth + "s", rs.getObject(i));
		} catch (SQLException e) {
			return e.toString();
		}
	}

	/**
	 * Return a header row for a result set, that is, the column labels padded to
	 * the column display widths.
	 *
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static String headerRow(ResultSet rs) throws SQLException {
		return IntStream.rangeClosed(1, rs.getMetaData().getColumnCount()).mapToObj(i -> headerToString(rs, i))
				.collect(Collectors.joining(" ")) + "\n";
	}

	/**
	 * Return a string representation of the column header.
	 *
	 * @param rs
	 * @param i      Index of column
	 * @param header Column header or column value?
	 * @return A string representation, justified right or left depending on the
	 *         column type, and padded to the recommended display length.
	 */
	private static String headerToString(ResultSet rs, int i) {
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			String align = NUMERIC_TYPES.contains(rsmd.getColumnType(i)) ? "" : "-";
			int colWidth = columnWidth(rsmd, i);
			return String.format("%" + align + colWidth + "s", rsmd.getColumnLabel(i));
		} catch (SQLException e) {
			return e.toString();
		}
	}

	/**
	 * Return a data row for a result set.
	 *
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private static String rowToString(ResultSet rs) throws SQLException {
		return IntStream.rangeClosed(1, rs.getMetaData().getColumnCount()).mapToObj(i -> colToString(rs, i))
				.collect(Collectors.joining(" ")) + "\n";
	}

	private boolean busy;
	private Connection connection;
	/** The database specific SQL for the current connection, empty if unsupported. */
	private volatile Optional<LockDialect> dialect = Optional.empty();
	private final ExecutorService ex = Executors.newSingleThreadExecutor();
	private final SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this);

	private String result = "";

	private ResultSet resultSet;

	private boolean resultSetPositioned;

	private PreparedStatement stmt;

	private String url;

	private String xid;

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	private void appendToResult(SQLException e) {
		appendToResult("!!! Exception occurred: " + e);
	}

	private void appendToResult(String row) {
		if (!row.endsWith("\n")) {
			row += "\n";
		}
		if (getResult() == null) {
			setResult(row);
		} else {
			setResult(getResult() + row);
		}
	}

	private void closeResultSet() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException ignored) {
		} finally {
			setResultSet(null);
		}
	}

	private void closeStatement() {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException ignored) {
		} finally {
			stmt = null;
		}
	}

	public void commit() {
		ex.submit(() -> commit0());
	}

	private void commit0() {
		endTransaction(true);
	}

	public void connect() {
		ex.submit(() -> connect0(getUrl()));
	}

	public void connect(String url) {
		setUrl(url);
		connect();
	}

	private void connect0(String url) {
		try {
			Connection conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);
			conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
			// The dialect must be known before listeners are told about the connection:
			// they ask for the lock SQL. It is created here, on the executor thread, so
			// that a dialect that depends on the thread gets a thread of its own.
			dialect = LockDialects.forProduct(conn.getMetaData().getDatabaseProductName());
			setConnection(conn);
			Preferences prefs = Preferences.userRoot().node(LockDemo.class.getName());
			prefs.put("url", url);
		} catch (SQLException e) {
			sqlException(e);
		}
	}

	public void disconnect() {
		try {
			connection.close();
			// Closing the connection ended the transaction (if the database allows it);
			// drop the state that belonged to it before notifying listeners.
			setTransactionId(null);
			closeStatement();
			closeResultSet();
			setConnection(null);
		} catch (SQLException e) {
			// If a tx is open, connection will not close. User must rollback first.
			appendToResult(e);
		}
	}

	private void endTransaction(boolean commit) {
		try {
			pcs.firePropertyChange("busy", this.busy, this.busy = true);
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
			if (resultSet != null) {
				resultSet.close();
				setResultSet(null);
			}
			if (connection != null) {
				if (commit) {
					connection.commit();
				} else {
					connection.rollback();
				}
			}
		} catch (SQLException e) {
			sqlException(e);
		} finally {
			pcs.firePropertyChange("busy", this.busy, this.busy = false);
			setTransactionId(null);
		}
	}

	public void execute(String sql) {
		ex.submit(() -> execute0(sql));
	}

	private void execute0(String sqls) {
		List<String> statements = split(sqls);
		for (String sql : statements) {
			if (!sql.matches("\\s*--.*")) {
				executeOne(sql);
			}
		}
	}

	private void executeOne(String sql) {
		sql = sql.strip();
		appendToResult(String.format("--- %tT -----------------------", Calendar.getInstance()));
		appendToResult(sql);
		// Already running on the executor thread: end the transaction synchronously so
		// that it takes effect in script order.
		if (sql.matches("(?i)commit(\\s+work)?")) {
			commit0();
		} else if (sql.matches("(?i)rollback(\\s+work)?")) {
			rollback0();
		} else
			try {
				stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
				pcs.firePropertyChange("busy", this.busy, this.busy = true);

				if (stmt.execute()) {
					setResultSet(stmt.getResultSet());
				} else {
					int count = stmt.getUpdateCount();
					showWarnings(stmt);
					appendToResult(String.format("%d rows affected\n", count));
				}
				setTransactionId(retrieveCurrentXid());
			} catch (SQLException e) {
				sqlException(e);
				rollback0();
			} finally {
				pcs.firePropertyChange("busy", this.busy, this.busy = false);
			}
	}

	private void showWarnings(PreparedStatement stmt) throws SQLException {
		for (SQLWarning warning = stmt.getWarnings(); warning != null; warning = warning.getNextWarning()) {
			appendToResult(warning);
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public String getCurrentXid() {
		return xid;
	}

	public IsolationLevel getIsolationLevel() {
		try {
			return IsolationLevel.valueOf(getConnection().getTransactionIsolation());
		} catch (SQLException e) {
			appendToResult(e);
			return null;
		}
	}

	public String getLocks() {
		String result = "";
		if (getConnection() == null)
			return "";
		try {
			try (PreparedStatement getLocks = getConnection().prepareStatement(getLockSQL(),
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
				try (ResultSet rs = getLocks.executeQuery()) {
					result += headerRow(rs);
					while (rs.next()) {
						result += rowToString(rs);
					}
				}
			}
			getConnection().commit();
		} catch (SQLException e) {
			return "Unexpected error: " + e;
		}
		return result;
	}

	/**
	 * The query that lists the current locks.
	 *
	 * @return the SQL text, or null if the database product of the connection is
	 *         not supported
	 */
	public String getLockSQL() {
		return dialect.map(LockDialect::lockSql).orElse(null);
	}

	public String getResult() {
		return result;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public String[] getSQLKeywords() {
		return new String[] { "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
				"CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON",
				"AS", "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL", "ORDER", "BY", "GROUP", "HAVING",
				"DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX", "UNION", "ALL", "BEGIN", "COMMIT", "ROLLBACK",
				"TRANSACTION", "WORK", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK", "DEFAULT", "INT",
				"INTEGER", "VARCHAR", "CHAR", "DATE", "TIME", "TIMESTAMP", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
				"BOOLEAN", "BLOB", "CLOB", "GRANT", "REVOKE", "WITH", "LOCK", "FOR", "SHARE", "NOWAIT" };

	}

	public String getUrl() {
		return url;
	}

	public boolean isBusy() {
		return busy;
	}

	public boolean isConnected() {
		try {
			return connection != null && !connection.isClosed();
		} catch (SQLException e) {
			sqlException(e);
			return false;
		}
	}

	public boolean isInTransaction() {
		return xid != null;
	}

	public boolean isResultSetOpen() {
		try {
			return resultSet != null && !resultSet.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	public boolean isResultSetPositioned() {
		return resultSetPositioned;
	}

	public void next() {
		ex.execute(() -> next0());
	}

	private void next0() {
		try {
			pcs.firePropertyChange("busy", false, this.busy = true);
			if (getResultSet() != null) {
				if (getResultSet().next()) {
					appendToResult(rowToString(getResultSet()));
					resultSetPositioned = true;
				} else {
					appendToResult("-- End of result set -----------------------");
					closeResultSet();
				}
			}
		} catch (SQLException e) {
			sqlException(e);
		} finally {
			pcs.firePropertyChange("busy", true, this.busy = false);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	/**
	 * Retrieve an identifier (XID) for the current transaction.
	 *
	 * @return A transaction identifier, or a dummy string if a transaction
	 *         identifier could not be retrieved.
	 */
	private String retrieveCurrentXid() {
		Optional<String> xidSql = dialect.map(LockDialect::transactionIdSql);
		if (xidSql.isEmpty()) {
			return "(no Xid)";
		}
		try (PreparedStatement ps = getConnection().prepareStatement(xidSql.get())) {
			try (ResultSet rs = ps.executeQuery()) {
				rs.next();
				return rs.getString(2);
			}
		} catch (SQLException e) {
			return "(no Xid)";
		}
	}

	public void rollback() {
		ex.submit(() -> rollback0());
	}

	private void rollback0() {
		endTransaction(false);
	}

	public void setConnection(Connection connection) {
		boolean wasConnected = getConnection() != null;
		this.connection = connection;
		pcs.firePropertyChange("connected", wasConnected, connection != null);
	}

	public void setIsolationLevel(int isolationLevel) {
		if (getConnection() != null) {
			try {
				if (getConnection().getTransactionIsolation() != isolationLevel) {
					getConnection().setTransactionIsolation(isolationLevel);
				}
			} catch (SQLException e) {
				appendToResult(e);
			}
		}
	}

	public void setResult(String result) {
		if (!result.endsWith("\n")) {
			result += "\n";
		}
		pcs.firePropertyChange("result", this.result, this.result = result);
	}

	public void setResultSet(ResultSet resultSet) {
		pcs.firePropertyChange("resultSet", this.resultSet, this.resultSet = resultSet);
		resultSetPositioned = false;
		if (getResultSet() != null) {
			try {
				appendToResult(headerRow(getResultSet()));
			} catch (SQLException e) {
				appendToResult(e);
			}
		}
	}

	protected void setTransactionId(String xid) {
		pcs.firePropertyChange("xid", this.xid, this.xid = xid);
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Shutdown the executor and clean up resources. Closes database connections and
	 * terminates the executor service.
	 */
	public void shutdown() {
		// Shutdown the executor service
		ex.shutdown();
		try {
			// Wait for existing tasks to terminate
			if (!ex.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
				// Force shutdown if tasks don't complete in time
				ex.shutdownNow();
				// Wait a bit for tasks to respond to being cancelled
				if (!ex.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
					LOGGER.severe("Executor service did not terminate");
				}
			}
		} catch (InterruptedException e) {
			// Re-cancel if current thread also interrupted
			ex.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}

		// Close database connection
		if (connection != null) {
			try {
				if (!connection.isClosed()) {
					// Rollback any pending transaction
					if (isInTransaction()) {
						connection.rollback();
					}
					connection.close();
					LOGGER.info("Database connection closed");
				}
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Error closing database connection", e);
			}
		}

		// Close result set if open
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				System.err.println("Error closing result set: " + e.getMessage());
			}
		}

		// Close statement if open
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				System.err.println("Error closing statement: " + e.getMessage());
			}
		}
	}

	private List<String> split(String sqls) {
		return Arrays.asList(sqls.split(";"))
		             .stream()
					 .filter(not(String::isBlank))
					 .collect(Collectors.toList());
	}

	private void sqlException(SQLException e) {
		appendToResult(e.toString() + "\n");
	}

	public void update() {
		ex.submit(() -> update0());
	}

	private void update0() {
		try {
			pcs.firePropertyChange("busy", false, this.busy = true);
			ResultSetMetaData rsmd = getResultSet().getMetaData();
			for (int col = 1; col <= rsmd.getColumnCount(); col++) {
				// isDefinitelyWritable() is false for all columns with H2 and Derby
				if (rsmd.isWritable(col) && !rsmd.isAutoIncrement(col)) {
					getResultSet().updateObject(col, getResultSet().getObject(col));
					getResultSet().updateRow();
					break;
				}
			}
		} catch (SQLException e) {
			appendToResult(e);
		} finally {
			pcs.firePropertyChange("busy", true, this.busy = false);
		}
	}
}
