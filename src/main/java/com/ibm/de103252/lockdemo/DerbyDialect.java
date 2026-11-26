package com.ibm.de103252.lockdemo;

/**
 * {@link LockDialect} for Apache Derby.
 * <p>
 * Derby has no function that returns the current transaction id. The dialect
 * therefore tags its transaction id query with a number and finds the
 * transaction in SYSCS_DIAG.TRANSACTION_TABLE by the SQL text. The number is
 * the id of the thread that creates the dialect. {@link Executor} creates it on
 * its own single executor thread, so every connection uses a different number.
 */
final class DerbyDialect implements LockDialect {

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
