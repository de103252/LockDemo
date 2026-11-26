package com.ibm.de103252.lockdemo;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry of the available {@link LockDialect}s. The dialects are private
 * nested classes: callers only need {@link #forProduct}. To support another
 * database, add a nested class that implements {@link LockDialect} and add its
 * constructor to {@link #ALL}.
 */
public final class LockDialects {

	/**
	 * Factories rather than instances, because a dialect may keep state that must
	 * not be shared between connections.
	 */
	private static final List<Supplier<LockDialect>> ALL = List.of(Db2Dialect::new, DerbyDialect::new,
			H2Dialect::new);

	private LockDialects() {
	}

	/**
	 * Finds the dialect for a database product.
	 *
	 * @param productName the value of
	 *                    {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
	 * @return a new dialect instance, or empty if the product is not supported
	 */
	public static Optional<LockDialect> forProduct(String productName) {
		return ALL.stream().map(Supplier::get).filter(d -> d.supports(productName)).findFirst();
	}

	/**
	 * {@link LockDialect} for Db2 for z/OS.
	 */
	private static final class Db2Dialect implements LockDialect {

		//@formatter:off
		/**
		 * Retrieves an identifier for the current transaction without a transaction
		 * table. The first column is a constant; the second column is the part of the
		 * special register CURRENT CLIENT_CORR_TOKEN that follows its fifth period (the
		 * LUW identifier part), which is meant to match the LUWID column of
		 * {@link #LOCK_SQL}. Executed against SYSIBM.SYSDUMMYU.
		 */
		private static final String XID_SQL = """
				select 42, substr(CURRENT CLIENT_CORR_TOKEN,
				               locate_in_string(CURRENT CLIENT_CORR_TOKEN, '.', 1, 5) + 1)
				  from sysibm.sysdummyu
				""";

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
		 * bounded VARCHAR so that {@code Executor#colToString} pads it to a sensible
		 * width.
		 * <p>
		 * The LUWID column is the transaction identifier, which
		 * {@link LockStateRenderer} locates by name.
		 */
		private static final String LOCK_SQL = """
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
		//@formatter:on

		@Override
		public boolean supports(String productName) {
			return "DB2".equals(productName);
		}

		@Override
		public String lockSql() {
			return LOCK_SQL;
		}

		@Override
		public String transactionIdSql() {
			return XID_SQL;
		}
	}

	/**
	 * {@link LockDialect} for Apache Derby.
	 * <p>
	 * Derby has no function that returns the current transaction id. The dialect
	 * therefore tags its transaction id query with a number and finds the
	 * transaction in SYSCS_DIAG.TRANSACTION_TABLE by the SQL text. The number is
	 * the id of the thread that creates the dialect. {@link Executor} creates it on
	 * its own single executor thread, so every connection uses a different number.
	 */
	private static final class DerbyDialect implements LockDialect {

		//@formatter:off
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
		private static final String LOCK_SQL = """
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
		//@formatter:on

		/**
		 * The statement text itself starts with a number, so that the transaction can
		 * be found in SYSCS_DIAG.TRANSACTION_TABLE by its SQL_TEXT. The number is
		 * substituted via {@link String#formatted}; the doubled percent sign is the
		 * escaped LIKE wildcard. The second column is the XID, matching the XID column
		 * of {@link #LOCK_SQL}.
		 */
		private final String xidSql = """
				select %1$d as rnd, xid
				  from SYSCS_DIAG.TRANSACTION_TABLE
				 where sql_text like 'select %1$d%%'
				""".formatted(Thread.currentThread().getId());

		@Override
		public boolean supports(String productName) {
			return "Apache Derby".equals(productName);
		}

		@Override
		public String lockSql() {
			return LOCK_SQL;
		}

		@Override
		public String transactionIdSql() {
			return xidSql;
		}
	}

	/**
	 * {@link LockDialect} for H2 (embedded, in-memory or server mode).
	 */
	private static final class H2Dialect implements LockDialect {

		//@formatter:off
		/**
		 * H2 has no transaction table; the session id identifies the transaction and
		 * matches the XID column of {@link #LOCK_SQL}.
		 */
		private static final String XID_SQL = """
				select 42, cast(session_id() as varchar(20))
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
		 * {@code Executor#colToString} pads every value to the column display size.
		 */
		private static final String LOCK_SQL = """
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

		@Override
		public boolean supports(String productName) {
			return "H2".equals(productName);
		}

		@Override
		public String lockSql() {
			return LOCK_SQL;
		}

		@Override
		public String transactionIdSql() {
			return XID_SQL;
		}
	}
}
