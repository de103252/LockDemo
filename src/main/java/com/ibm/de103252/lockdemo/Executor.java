package com.ibm.de103252.lockdemo;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.SMALLINT;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.event.SwingPropertyChangeSupport;

public class Executor {
    private static final String getXidDb2 = "select 42, substr(CURRENT CLIENT_CORR_TOKEN,"
            + "               locate_in_string(CURRENT CLIENT_CORR_TOKEN, '.', 1, 5) + 1) from sysibm.sysdummyu";
    //@formatter:off
    private static final String getXidDerby =
            "select " + Thread.currentThread().threadId() + " as rnd, xid" +
            "  from SYSCS_DIAG.TRANSACTION_TABLE" +
            " where sql_text like 'select " + Thread.currentThread().threadId() + "%'";
    private static final String LOCK_SQL_DB2 = ""
            + "select ACQUIRED_TS"
            + ", CAST(LUWID AS CHAR(30)) LUWID"
            + ", SSID"
            + ", case SUBTYPE"
            + "  when '00' then 'Page lock'"
            + "  when '01' then 'Database lock'"
            + "  when '02' then 'Page set lock'"
            + "  when '03' then 'Data set lock (partition)'"
            + "  when '05' then 'Index compression lock'"
            + "  when '06' then 'Lock-specific partition'"
            + "  when '07' then 'Page set or data set open'"
            + "  when '08' then 'Utility I/O damage assessment'"
            + "  when '09' then 'Page set piece locks'"
            + "  when '0A' then 'DBET entry locks'"
            + "  when '0D' then 'SYSLGRNG recording or GBP lock'"
            + "  when '0E' then 'Utility serialization lock'"
            + "  when '0F' then 'Mass delete lock for table'"
            + "  when '10' then 'Table lock for segmented TS'"
            + "  when '12' then 'Package lock'"
            + "  when '18' then 'Row lock'"
            + "  when '1F' then 'CDB P-lock'"
            + "  when '22' then 'RLF P-lock'"
            + "  when '27' then 'LPL or GRECP locks'"
            + "  when '30' then 'LOB lock'"
            + "  when '32' then 'LPL recovery lock'"
            + "  when '36' then 'Adding partitions'"
            + "  when '39' then 'Load DBD lock'"
            + "  when '3A' then 'Dictionary build lock'"
            + "  when '3B' then 'Dictionary load lock'"
            + "  when '41' then 'Utility catalog access lock'"
            + "  when '20' then 'WR claim'"
            + "  when '40' then 'RR claim'"
            + "  when '60' then 'RR, WR claim'"
            + "  when '80' then 'CS claim'"
            + "  when 'A0' then 'CS, WR claim'"
            + "  end subtype"
            + ", case DURATION"
            + "  when '20' then 'Manual'"
            + "  when '21' then 'Manual+1'"
            + "  when '40' then 'Commit'"
            + "  when '41' then 'Commit+1'"
            + "  when '60' then 'Allocation'"
            + "  when '80' then 'Plan'"
            + "  when '81' then 'Utility'"
            + "  when 'FE' then 'Interest'"
            + "  when 'CM' then 'Commit'"
            + "  when 'CH' then 'Cursor hold'"
            + "  when 'AL' then 'Allocation'"
            + "end duration"
            + ", case STATE"
            + "  when '01' then 'US'"
            + "  when '02' then 'IS'"
            + "  when '03' then 'IX'"
            + "  when '04' then 'S'"
            + "  when '05' then 'U'"
            + "  when '06' then 'SIX'"
            + "  when '07' then 'NSU'"
            + "  when '08' then 'X'"
            + "  when '09' then 'P-IX'"
            + "  when '0A' then 'P-IS'"
            + "  when '0B' then 'P-SIX'  "
            + "   end state "
            + ", DBNAME, CAST(TRIM(OBJECT_QUALIFIER || '.' || OBJECT_NAME) AS VARCHAR(50)) OBJECT, PAGENUM_OR_RID "
            + "  from table(blocking_threads('A*:Z*'))"
            + " where userid = current sqlid"
            + "   and object_qualifier is not null"
            + "   and dbname not like 'DSNDB%'"
            + ""
            + "";
    private static final String LOCK_SQL_DERBY = ""
            + "SELECT L.XID"
            + "     , L.TYPE"
            + "     , L.MODE"
            + "     , SUBSTR(L.TABLENAME, 1, 16) as TABLENAME"
            + "     , L.LOCKNAME"
            + "     , L.STATE"
            + "     , L.TABLETYPE"
            + "     , L.LOCKCOUNT"
            + "     , L.INDEXNAME"
            + "     , T.STATUS"
            + "     , T.FIRST_INSTANT"
            + "     , T.SQL_TEXT"
            + "  FROM SYSCS_DIAG.LOCK_TABLE L"
            + "  JOIN SYSCS_DIAG.TRANSACTION_TABLE T"
            + "    ON L.XID = T.XID"
            + " ORDER BY L.XID"
            + "        , CASE L.TYPE "
            + "               WHEN 'TABLE' THEN 1 "
            + "               WHEN 'ROW' THEN 2 "
            + "               ELSE 3 "
            + "          END"
            + "        , TABLENAME"
            + "        , LOCKNAME"
            + "        , L.MODE DESC"
            + "";
    //@formatter:on
    private static final List<Integer> NUMERIC_TYPES = Arrays.asList(DECIMAL, DOUBLE, FLOAT, INTEGER, SMALLINT, BIGINT);
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

    public static String getLocksSQL(String dbProduct) {
        switch (dbProduct) {
        case "Apache Derby":
            return LOCK_SQL_DERBY;
        case "DB2":
            return LOCK_SQL_DB2;
        default:
            return null;
        }
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
            int colWidth = Math.max(rsmd.getColumnDisplaySize(i), rsmd.getColumnLabel(i).length());
            return String.format("%" + align + colWidth + "s", rs.getObject(i));
        } catch (SQLException e) {
            return e.toString();
        }
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
            int colWidth = Math.max(rsmd.getColumnDisplaySize(i), rsmd.getColumnLabel(i).length());
            return String.format("%" + align + colWidth + "s", rsmd.getColumnLabel(i));
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
        return IntStream.rangeClosed(1, rs.getMetaData().getColumnCount())
                .mapToObj(i -> headerToString(rs, i))
                .collect(Collectors.joining(" "))
        + "\n";
    }

    /**
     * Return a data row for a result set.
     *
     * @param rs
     * @return
     * @throws SQLException
     */
    private static String rowToString(ResultSet rs) throws SQLException {
        return IntStream.rangeClosed(1, rs.getMetaData().getColumnCount())
                .mapToObj(i -> colToString(rs, i))
                .collect(Collectors.joining(" "))
        + "\n";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void commit() {
        ex.submit(() -> commit0());
    }

    public void connect() {
        ex.submit(() -> connect0(getUrl()));
    }

    public void disconnect() {
        try {
            connection.close();
            setConnection(null);
        } catch (SQLException e) {
            // If a tx is open, connection will not close. User must rollback first.
            appendToResult(e);
        }
    }

    public void execute(String sql) {
        ex.submit(() -> execute0(sql));
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
            try (PreparedStatement getLocks = getConnection().prepareStatement(getLockSQL())) {
                ResultSet rs = getLocks.executeQuery();
                result += headerRow(rs);
                while (rs.next()) {
                    result += rowToString(rs);
                }
            }
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

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void rollback() {
        ex.submit(() -> rollback0());
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

    public void setUrl(String url) {
        this.url = url;
    }

    public void update() {
        ex.submit(() -> update0());
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

    private void commit0() {
        endTransaction(true);
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

    private void endTransaction(boolean commit) {
        try {
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            if (resultSet != null) {
                resultSet.close();
                setResultSet(null);
            }
            if (connection != null) {
                if (commit)
                    connection.commit();
                else
                    connection.rollback();
            }
        } catch (SQLException e) {
            sqlException(e);
        } finally {
            setTransactionId(null);
        }
    }

    private void execute0(String sqls) {
        List<String> statements = split(sqls);
        for (String sql : statements) {
            executeOne(sql);
        }
    }

    private void executeOne(String sql) {
        appendToResult(String.format("--- %tT -----------------------", Calendar.getInstance()));
        appendToResult(sql);
        if (sql.matches("(?i)commit(\\s+work)?")) {
            commit();
        } else if (sql.matches("(?i)rollback(\\s+work)?")) {
            rollback();
        } else
            try {
                stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                pcs.firePropertyChange("busy", this.busy, this.busy = true);
                if (stmt.execute()) {
                    setResultSet(stmt.getResultSet());
                } else {
                    int count = stmt.getUpdateCount();
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

    private void rollback0() {
        endTransaction(false);
    }

    private List<String> split(String sqls) {
        return Arrays.asList(sqls.split(";"))
                     .stream()
                     .filter((s) -> !s.trim().isEmpty())
                     .collect(Collectors.toList());
    }

    private void sqlException(SQLException e) {
        appendToResult(e.toString() + "\n");
    }

    private void update0() {
        try {
            pcs.firePropertyChange("busy", false, true);
            getResultSet().updateObject(1, getResultSet().getObject(1));
            getResultSet().updateRow();
        } catch (SQLException e) {
            appendToResult(e);
        } finally {
            pcs.firePropertyChange("busy", true, false);
        }
    }

    protected void setTransactionId(String xid) {
        pcs.firePropertyChange("xid", this.xid, this.xid = xid);
    }
}
