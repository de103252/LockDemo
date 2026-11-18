-- H2 locking demo.
--
-- Connect both panels to the same in-memory database, using the first
-- H2 entry of the URL list (jdbc:h2:mem:lockdemo ...).
-- Run "00 Setup.sql" first.
--
-- H2 uses MVCC, so readers do not block writers. Only table-level locks appear
-- in the lock display (S/X/IS/IX derived from H2's INFORMATION_SCHEMA.LOCKS and
-- SESSIONS). A blocked statement is shown as a WAIT row (orange).
-- H2 has no WITH UR/CS/RS/RR clause, so set the isolation level in the GUI.

-- 1. Dirty read? Try Read uncommitted vs. Read committed on the right side.
/* L */ UPDATE emp SET salary = salary + 200 WHERE eid = 103;
/* R */ SELECT salary FROM emp WHERE eid = 103;
/* L */ ROLLBACK;
/* R */ ROLLBACK;

-- 2. Write/write conflict: the right side waits until the left side ends
-- (or until LOCK_TIMEOUT expires, here 60 seconds).
/* L */ UPDATE emp SET salary = salary + 200 WHERE eid = 103;
/* R */ UPDATE emp SET salary = salary + 300 WHERE eid = 103;
/* L */ COMMIT;
/* R */ COMMIT;

-- 3. Deadlock: H2 detects it and aborts one of the two statements.
/* 1 L */ UPDATE emp  SET salary = salary * 1.1 WHERE eid = 103;
/* 2 R */ UPDATE dept SET name = 'Handling' WHERE did = 50;
/* 3 L */ UPDATE dept SET name = 'Handling 2' WHERE did = 50;
/* 4 R */ UPDATE emp  SET salary = salary * 1.1 WHERE eid = 103;
