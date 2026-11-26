package com.ibm.de103252.lockdemo;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry of the available {@link LockDialect}s. To support another database,
 * implement {@link LockDialect} and add its constructor to {@link #ALL}.
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
}
