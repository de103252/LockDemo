package com.ibm.de103252.lockdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class LockDialectsTest {

	@Test
	void selectsDialectByProductName() {
		assertEquals("Db2Dialect", LockDialects.forProduct("DB2").orElseThrow().getClass().getSimpleName());
		assertEquals("DerbyDialect",
				LockDialects.forProduct("Apache Derby").orElseThrow().getClass().getSimpleName());
		assertEquals("H2Dialect", LockDialects.forProduct("H2").orElseThrow().getClass().getSimpleName());
	}

	@Test
	void unknownProductHasNoDialect() {
		assertTrue(LockDialects.forProduct("Oracle").isEmpty());
		assertTrue(LockDialects.forProduct("").isEmpty());
	}

	@Test
	void everyLookupReturnsNewInstance() {
		assertNotEquals(System.identityHashCode(LockDialects.forProduct("H2").orElseThrow()),
				System.identityHashCode(LockDialects.forProduct("H2").orElseThrow()));
	}

	/**
	 * The Derby transaction id query is tagged with the id of the thread that
	 * created the dialect, so dialects of different executors must not collide.
	 */
	@Test
	void derbyTransactionIdQueryIsTaggedPerThread() throws Exception {
		ExecutorService first = Executors.newSingleThreadExecutor();
		ExecutorService second = Executors.newSingleThreadExecutor();
		try {
			Callable<String> create = () -> LockDialects.forProduct("Apache Derby").orElseThrow().transactionIdSql();
			String sql1 = first.submit(create).get();
			String sql2 = second.submit(create).get();
			assertNotEquals(sql1, sql2);
			assertEquals(sql1, first.submit(create).get());
		} finally {
			first.shutdownNow();
			second.shutdownNow();
		}
	}

	/**
	 * {@link LockStateRenderer} locates its columns by name. The Db2 query cannot
	 * be run here, so check that it names the columns the renderer needs.
	 */
	@Test
	void db2QueryProvidesRendererColumns() {
		String sql = LockDialects.forProduct("DB2").orElseThrow().lockSql().toUpperCase();
		for (String column : new String[] { " LUWID", " STATE", " OBJECT" }) {
			assertTrue(sql.contains(column), "missing column " + column);
		}
	}
}
