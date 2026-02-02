-- Starten Sie zunächst die "Locking Demo" GUI und verbinden Sie sich
-- auf beiden Seiten mit der Derby-Datenbank.
-- Entweder mit der voreingestellten "in-memory" Datenbank oder
-- mit einer laufenden Derby-Instanz (z.B. jdbc:derby://localhost:1527/DBIDB).

-- Demonstrate locking in the HR schema

-- SET CURRENT SCHEMA = HRL;

-- Folgende SQL in verschiedenen Isolation Levels ausprobieren
-- oder mit der WITH [ UR | CS | RS | RR ] Klausel angehängt:
SELECT * FROM EMP;
SELECT * FROM EMP FOR READ ONLY;

-- Aggregatfunktionen, Joins
SELECT avg(salary) FROM EMP;

-- Warum werden hier in Read Committed keine Row Locks gehalten?
SELECT * 
  FROM EMP
  JOIN DEPT USING (did);
  
-- Folgende SQLs links bzw. rechts eintippen und ausführen.
-- Rechte bzw. linke Seite committen.

SELECT AVG(salary) FROM EMP WITH RS;
UPDATE EMP  SET salary = salary + 200 WHERE eid = 103;

-- Experimentieren Sie mit verschiedenen Isolation Levels rechts
-- und versuchen Sie insbesondere, die verschiedenen Anomalien
-- (dirty read, non-repeatable read) nachzuvollziehen.
/* L */ INSERT INTO EMP VALUES ( 142, 'Schwartz', 50, 30);
/* R */ SELECT AVG(salary) AS "Average salary" FROM EMP WITH RR;

-- Experimentieren Sie mit Suchbereichen:
SELECT AVG(salary) FROM EMP
 WHERE EID BETWEEN 200 and 300
  WITH RS;

insert into emp(eid, name, salary) values(222, 'Doe', 4711);

-- Versuchen Sie, ein Deadlock-Szenario herbeizuführen.
-- Hier eine Möglichkeit (erst selbst probieren...):



/* 1 L */ UPDATE EMP SET salary = salary * 1.1 WHERE eid = 103;
/* 2 R */ UPDATE DEPT SET name = 'Handling' WHERE did = 50;
/* 3 L */ SELECT * FROM DEPT WHERE did = 50;
/* 4 R */ SELECT * FROM EMP WHERE eid = 103;



-- Massenupdate

-- Tut effektiv nichts, hält aber viele Locks.
-- Bei "sehr vielen" Locks kommt es zu einer Lock Escalation.
-- Wie viel "sehr viel ist, wird von der Datenbank-Konfigurationseinstellung
-- derby.locks.escalationThreshold bestimmt, die wir abfragen und auch
-- verändern können, siehe folgende Statements.
UPDATE person SET dob = dob;

-- Lock Escalation Threshold abfragen
VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(
 'derby.locks.escalationThreshold'
);

-- Lock Escalation Threshold ändern
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(
 'derby.locks.escalationThreshold',
 '1000000');







-- Klausuraufgabe aus DBMS-Implementierungen:
--
-- Wie viele Ergebniszeilen liefern die Abfragen (1) bis (3) zurück,
-- wenn beide Transaktionen in "Read Committed" (CS) laufen?
-- Wie viele Ergebniszeilen wären zurückgeliefert worden,
-- wenn die Transaktionen in UR gelaufen wären?


-- Links -------------------------------  -- Rechts --------------------------------
CREATE TABLE STAR
(ID INTEGER, Name VARCHAR(20));
COMMIT;

INSERT INTO STAR VALUES (3, 'Miley');     
                                          INSERT INTO STAR VALUES (7, 'Billie');
INSERT INTO STAR VALUES (5, 'Taylor');
                                          SELECT * FROM STAR FOR READ ONLY; -- (1)
COMMIT;
                                          DELETE FROM STAR;
SELECT * FROM STAR FOR READ ONLY; -- (2)
                                          ROLLBACK;
                                          SELECT * FROM STAR FOR READ ONLY; -- (3)
COMMIT;
