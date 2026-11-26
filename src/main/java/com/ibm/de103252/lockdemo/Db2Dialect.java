package com.ibm.de103252.lockdemo;

/**
 * {@link LockDialect} for Db2 for z/OS.
 */
final class Db2Dialect implements LockDialect {

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
