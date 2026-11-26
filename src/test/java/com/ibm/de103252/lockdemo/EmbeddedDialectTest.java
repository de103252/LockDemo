package com.ibm.de103252.lockdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Runs the lock queries of the dialects against real in-memory databases, to
 * verify that they are valid SQL, that they provide the columns that
 * {@link LockStateRenderer} looks for, and that the transaction id query returns
 * the id that appears in the lock query.
 */
class EmbeddedDialectTest {

	/** Test against one database. */
	abstract static class Base {
		/** Holds the lock. */
		Connection holder;
		/** Looks at the locks, like the lock display of the application. */
		Connection observer;
		LockDialect dialect;

		abstract String url();

		abstract String product();

		/** Update a row and keep the transaction open. */
		abstract void createTableAndLockRow() throws SQLException;

		@BeforeEach
		void connect() throws SQLException {
			holder = DriverManager.getConnection(url());
			holder.setAutoCommit(false);
			observer = DriverManager.getConnection(url());
			observer.setAutoCommit(false);
			assertEquals(product(), holder.getMetaData().getDatabaseProductName());
			dialect = LockDialects.forProduct(product()).orElseThrow();
		}

		@AfterEach
		void disconnect() throws SQLException {
			holder.rollback();
			observer.rollback();
			holder.close();
			observer.close();
		}

		@Test
		void lockQueryProvidesColumnsOfTheRenderer() throws SQLException {
			try (PreparedStatement ps = observer.prepareStatement(dialect.lockSql());
					ResultSet rs = ps.executeQuery()) {
				List<String> columns = columnNames(rs.getMetaData());
				assertTrue(columns.stream().anyMatch(c -> c.contains("XID") || c.contains("LUWID")), "XID " + columns);
				assertTrue(columns.stream().anyMatch(c -> c.contains("MODE")), "MODE " + columns);
				assertTrue(columns.contains("STATE"), "STATE " + columns);
				assertTrue(columns.contains("STATUS"), "STATUS " + columns);
				assertTrue(columns.stream().anyMatch(c -> c.contains("TABLENAME") || c.contains("OBJECT")),
						"TABLENAME " + columns);
				assertTrue(columns.stream().anyMatch(c -> c.contains("LOCKNAME")), "LOCKNAME " + columns);
			}
		}

		@Test
		void lockOfOpenTransactionIsListedUnderItsTransactionId() throws SQLException {
			createTableAndLockRow();
			String xid = transactionId(holder);
			assertFalse(xid.isBlank());

			boolean found = false;
			try (PreparedStatement ps = observer.prepareStatement(dialect.lockSql());
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					found |= xid.equals(rs.getString("XID")) && rs.getString("TABLENAME").contains("LOCKTEST");
				}
			}
			observer.commit();
			assertTrue(found, "no lock row for transaction " + xid);
		}

		String transactionId(Connection c) throws SQLException {
			try (PreparedStatement ps = c.prepareStatement(dialect.transactionIdSql());
					ResultSet rs = ps.executeQuery()) {
				assertTrue(rs.next());
				return rs.getString(2);
			}
		}

		void execute(Connection c, String sql) throws SQLException {
			try (Statement s = c.createStatement()) {
				s.execute(sql);
			}
		}
	}

	@Nested
	class H2 extends Base {
		private final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

		@Override
		String url() {
			return url;
		}

		@Override
		String product() {
			return "H2";
		}

		@Override
		void createTableAndLockRow() throws SQLException {
			execute(holder, "create table locktest (id int primary key, v int)");
			execute(holder, "insert into locktest values (1, 1)");
			holder.commit();
			execute(holder, "update locktest set v = 2 where id = 1");
		}
	}

	@Nested
	class Derby extends Base {
		private final String url = "jdbc:derby:memory:" + UUID.randomUUID() + ";create=true";

		@Override
		String url() {
			return url;
		}

		@Override
		String product() {
			return "Apache Derby";
		}

		@Override
		void createTableAndLockRow() throws SQLException {
			execute(holder, "create table locktest (id int primary key, v int)");
			execute(holder, "insert into locktest values (1, 1)");
			holder.commit();
			execute(holder, "update locktest set v = 2 where id = 1");
		}
	}

	private static List<String> columnNames(ResultSetMetaData md) throws SQLException {
		List<String> names = new ArrayList<>();
		for (int i = 1; i <= md.getColumnCount(); i++) {
			names.add(md.getColumnLabel(i).toUpperCase(Locale.ROOT));
		}
		return names;
	}
}
