package com.ibm.de103252.lockdemo;

/**
 * {@link LockDialect} for H2 (embedded, in-memory or server mode).
 */
final class H2Dialect implements LockDialect {

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
