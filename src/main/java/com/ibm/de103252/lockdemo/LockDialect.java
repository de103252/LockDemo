package com.ibm.de103252.lockdemo;

/**
 * Everything the {@link Executor} and the lock display need to know about one
 * database product: how to list the current locks and how to identify the
 * current transaction.
 * <p>
 * The lock query and the transaction id query of one dialect belong together:
 * the id returned by {@link #transactionIdSql()} must match the transaction
 * identifier column of {@link #lockSql()}, so that the status line of a panel
 * can be correlated with the rows of the lock display.
 * <p>
 * An instance is used by a single {@link Executor} (that is, by a single
 * connection), so a dialect may keep per-connection state.
 */
public interface LockDialect {

	/**
	 * Decides whether this dialect handles a database product.
	 *
	 * @param productName the value of
	 *                    {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
	 * @return true if this dialect can be used for the product
	 */
	boolean supports(String productName);

	/**
	 * The query that lists the current locks.
	 * <p>
	 * {@link LockStateRenderer} locates the columns of the result by name (case
	 * insensitive), so a result set must provide these columns:
	 * <ul>
	 * <li>XID or LUWID: the transaction identifier,</li>
	 * <li>MODE: the lock mode, for example IS, IX, S, U or X,</li>
	 * <li>STATE: GRANT, or WAIT for a request that is waiting,</li>
	 * <li>STATUS: whether the transaction is ACTIVE or IDLE,</li>
	 * <li>TABLENAME or OBJECT: the locked table,</li>
	 * <li>LOCKNAME, or RID or PAGENUM: the locked row or page.</li>
	 * </ul>
	 * All text columns should have a bounded width, because
	 * {@code Executor#colToString} pads every value to the column display size.
	 *
	 * @return the SQL text
	 */
	String lockSql();

	/**
	 * The query that retrieves the identifier of the current transaction. The
	 * identifier is read from the second column of the first row, and it should be
	 * the same value as in the transaction identifier column of
	 * {@link #lockSql()}.
	 *
	 * @return the SQL text
	 */
	String transactionIdSql();
}
