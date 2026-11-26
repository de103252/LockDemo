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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.event.SwingPropertyChangeSupport;

public class Executor {
	//@formatter:off
	/**
	 * Db2: retrieves an identifier for the current transaction without a
	 * transaction table. The first column is a constant; the second column is the
	 * part of the special register CURRENT CLIENT_CORR_TOKEN that follows its fifth
	 * period (the LUW identifier part), which is meant to match the LUWID column of
	 * {@link #LOCK_SQL_DB2}. Executed against SYSIBM.SYSDUMMYU.
	 */
	private static final String getXidDb2 = """
			select 42, substr(CURRENT CLIENT_CORR_TOKEN,
			               locate_in_string(CURRENT CLIENT_CORR_TOKEN, '.', 1, 5) + 1)
			  from sysibm.sysdummyu
			""";

	/**
	 * H2 has no transaction table; the session id identifies the transaction and
	 * matches the XID column of {@link #LOCK_SQL_H2}.
	 */
	private static final String getXidH2 = """
			select 42, cast(session_id() as varchar(20))
			""";

	/**
	 * Derby: the statement text itself starts with the thread id, so that the
	 * transaction can be found in SYSCS_DIAG.TRANSACTION_TABLE by its SQL_TEXT.
	 * The id is substituted via {@link String#formatted}; the doubled percent sign
	 * is the escaped LIKE wildcard. The second column is the XID, matching the XID
	 * column of {@link #LOCK_SQL_DERBY}.
	 */
	private static final String getXidDerby = """
			select %1$d as rnd, xid
			  from SYSCS_DIAG.TRANSACTION_TABLE
			 where sql_text like 'select %1$d%%'
			""".formatted(Thread.currentThread().getId());

	/**
	 * Lock monitoring for Db2 for z/OS.
	 * <p>
	 * Reads the locks held or awaited by the current authorization id from the
	 * table function {@code blocking_threads}. The coded columns SUBTYPE, DURATION
	 * and STATE are decoded into readable text:
	 * <ul>
	 * <li>SUBTYPE: the kind of resource that is locked (page, row, table, claim,
	 * ...).</li>
	 * <li>DURATION: how long the lock is held (for example Commit, Plan or
	 * Allocation).</li>
	 * <li>STATE: the lock mode (for example IS, IX, S, U, X).</li>
	 * </ul>
	 * Rows are restricted to the current SQLID, to locks on a named object
	 * (OBJECT_QUALIFIER not null), and to user databases (system databases
	 * DSNDB* are excluded). OBJECT is the trimmed qualified name, CAST to a
	 * bounded VARCHAR so that {@link #colToString} pads it to a sensible width.
	 * <p>
	 * The LUWID column is the transaction identifier, which
	 * {@link LockStateRenderer} locates by name.
	 */
	private static final String LOCK_SQL_DB2 = """
			select ACQUIRED_TS
			     , CAST(LUWID AS CHAR(30)) LUWID
			     , SSID
			     , case SUBTYPE
			         when '00' then 'Page lock'
			         when '01' then 'Database lock'
			         when '02' then 'Page set lock'
			         when '03' then 'Data set lock (partition)'
			         when '05' then 'Index compression lock'
			         when '06' then 'Lock-specific partition'
			         when '07' then 'Page set or data set open'
			         when '08' then 'Utility I/O damage assessment'
			         when '09' then 'Page set piece locks'
			         when '0A' then 'DBET entry locks'
			         when '0D' then 'SYSLGRNG recording or GBP lock'
			         when '0E' then 'Utility serialization lock'
			         when '0F' then 'Mass delete lock for table'
			         when '10' then 'Table lock for segmented TS'
			         when '12' then 'Package lock'
			         when '18' then 'Row lock'
			         when '1F' then 'CDB P-lock'
			         when '22' then 'RLF P-lock'
			         when '27' then 'LPL or GRECP locks'
			         when '30' then 'LOB lock'
			         when '32' then 'LPL recovery lock'
			         when '36' then 'Adding partitions'
			         when '39' then 'Load DBD lock'
			         when '3A' then 'Dictionary build lock'
			         when '3B' then 'Dictionary load lock'
			         when '41' then 'Utility catalog access lock'
			         when '20' then 'WR claim'
			         when '40' then 'RR claim'
			         when '60' then 'RR, WR claim'
			         when '80' then 'CS claim'
			         when 'A0' then 'CS, WR claim'
			       end subtype
			     , case DURATION
			         when '20' then 'Manual'
			         when '21' then 'Manual+1'
			         when '40' then 'Commit'
			         when '41' then 'Commit+1'
			         when '60' then 'Allocation'
			         when '80' then 'Plan'
			         when '81' then 'Utility'
			         when 'FE' then 'Interest'
			         when 'CM' then 'Commit'
			         when 'CH' then 'Cursor hold'
			         when 'AL' then 'Allocation'
			       end duration
			     , case STATE
			         when '01' then 'US'
			         when '02' then 'IS'
			         when '03' then 'IX'
			         when '04' then 'S'
			         when '05' then 'U'
			         when '06' then 'SIX'
			         when '07' then 'NSU'
			         when '08' then 'X'
			         when '09' then 'P-IX'
			         when '0A' then 'P-IS'
			         when '0B' then 'P-SIX'
			       end state
			     , DBNAME, CAST(TRIM(OBJECT_QUALIFIER || '.' || OBJECT_NAME) AS VARCHAR(50)) OBJECT, PAGENUM_OR_RID
			  from table(blocking_threads('A*:Z*'))
			 where userid = current sqlid
			   and object_qualifier is not null
			   and dbname not like 'DSNDB%'
			""";

	/**
	 * SQL query to retrieve lock information from Apache Derby's diagnostic tables.
	 * Joins LOCK_TABLE with TRANSACTION_TABLE to show lock details along with
	 * transaction status and the SQL text that acquired each lock.
	 * <p>
	 * The join column XID identifies the transaction; locks of transactions that
	 * are no longer in the transaction table are not shown. TABLENAME is truncated
	 * to 16 characters to keep the display narrow. Rows are ordered by transaction,
	 * then table locks before row locks (other lock types last), then by table
	 * name and lock name, and finally by lock mode in descending order.
	 */
	private static final String LOCK_SQL_DERBY = """
			SELECT L.XID
			     , L.TYPE
			     , L.MODE
			     , SUBSTR(L.TABLENAME, 1, 16) as TABLENAME
			     , L.LOCKNAME
			     , L.STATE
			     , L.TABLETYPE
			     , L.LOCKCOUNT
			     , L.INDEXNAME
			     , T.STATUS
			     , T.FIRST_INSTANT
			     , T.SQL_TEXT
			  FROM SYSCS_DIAG.LOCK_TABLE L
			  JOIN SYSCS_DIAG.TRANSACTION_TABLE T
			    ON L.XID = T.XID
			 ORDER BY L.XID
			        , CASE L.TYPE
			               WHEN 'TABLE' THEN 1
			               WHEN 'ROW' THEN 2
			               ELSE 3
			          END
			        , TABLENAME
			        , LOCKNAME
			        , L.MODE DESC
			""";

	/**
	 * Lock monitoring for H2 (embedded, in-memory or server mode).
	 * <p>
	 * H2 differs from Derby and Db2 in what it exposes:
	 * <ul>
	 * <li>Only <em>table</em> locks are listed in INFORMATION_SCHEMA.LOCKS, and
	 * H2 reports a READ lock even for sessions that modify rows (row changes are
	 * handled by MVCC and have no lock entries). The mode is therefore derived:
	 * WRITE is X, otherwise IX if the session has uncommitted changes and IS if
	 * not.</li>
	 * <li>A statement waiting for a row or table lock is reported by
	 * INFORMATION_SCHEMA.SESSIONS as BLOCKED with the BLOCKER_ID of the session
	 * it waits for. Such sessions are shown as STATE = WAIT on the tables locked
	 * by their blocker, so the existing conflict highlighting applies.</li>
	 * <li>INFORMATION_SCHEMA.SESSIONS shows all sessions only to users with ADMIN
	 * rights, which is the case for the user that created the database.</li>
	 * </ul>
	 * Column names match those of the other databases because
	 * {@link LockStateRenderer} locates its columns by name.
	 * <p>
	 * All text columns are CAST to a bounded VARCHAR: H2 types string expressions
	 * (concatenation, CASE, literals) with a huge maximum length, and
	 * {@link #colToString} pads every value to the column display size.
	 */
	private static final String LOCK_SQL_H2 = """
			SELECT L.SESSION_ID AS XID
			     , CAST('TABLE' AS VARCHAR(5)) AS TYPE
			     , CAST(CASE WHEN L.LOCK_TYPE = 'WRITE' THEN 'X'
			                 WHEN S.CONTAINS_UNCOMMITTED THEN 'IX'
			                 ELSE 'IS' END AS VARCHAR(2)) AS MODE
			     , CAST(L.TABLE_SCHEMA || '.' || L.TABLE_NAME AS VARCHAR(50)) AS TABLENAME
			     , CAST('TABLE' AS VARCHAR(5)) AS LOCKNAME
			     , CAST('GRANT' AS VARCHAR(5)) AS STATE
			     , CAST(CASE WHEN S.CONTAINS_UNCOMMITTED OR S.SESSION_STATE <> 'SLEEP'
			                 THEN 'ACTIVE' ELSE 'IDLE' END AS VARCHAR(6)) AS STATUS
			     , CAST(S.EXECUTING_STATEMENT AS VARCHAR(60)) AS SQL_TEXT
			  FROM INFORMATION_SCHEMA.LOCKS L
			  JOIN INFORMATION_SCHEMA.SESSIONS S
			    ON S.SESSION_ID = L.SESSION_ID
			 UNION ALL
			 SELECT W.SESSION_ID
			     , CAST('TABLE' AS VARCHAR(5))
			     , CAST('X' AS VARCHAR(2))
			     , CAST(COALESCE(B.TABLE_SCHEMA || '.' || B.TABLE_NAME, '(unknown)') AS VARCHAR(50))
			     , CAST('TABLE' AS VARCHAR(5))
			     , CAST('WAIT' AS VARCHAR(5))
			     , CAST('ACTIVE' AS VARCHAR(6))
			     , CAST(W.EXECUTING_STATEMENT AS VARCHAR(60))
			  FROM INFORMATION_SCHEMA.SESSIONS W
			  LEFT JOIN INFORMATION_SCHEMA.LOCKS B
			    ON B.SESSION_ID = W.BLOCKER_ID
			 WHERE W.SESSION_STATE = 'BLOCKED'
			 ORDER BY XID
			        , TABLENAME
			        , MODE DESC
			""";
	//@formatter:on

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

	public static String getLocksSQL(String dbProduct) {
		switch (dbProduct) {
		case "Apache Derby":
			return LOCK_SQL_DERBY;
		case "DB2":
			return LOCK_SQL_DB2;
		case "H2":
			return LOCK_SQL_H2;
		default:
			return null;
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
	private String dbProduct;
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
			setConnection(conn);
			dbProduct = conn.getMetaData().getDatabaseProductName();
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

	public String getLockSQL() throws SQLException {
		return getLocksSQL(getConnection().getMetaData().getDatabaseProductName());
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
	 * @throws SQLException
	 */
	private String retrieveCurrentXid() {
		String xidSQL;
		switch (dbProduct) {
		case "Apache Derby":
			xidSQL = getXidDerby;
			break;
		case "DB2":
			xidSQL = getXidDb2;
			break;
		case "H2":
			xidSQL = getXidH2;
			break;
		default:
			return "(no Xid)";
		}
		try (PreparedStatement ps = getConnection().prepareStatement(xidSQL)) {
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
