-- Starten Sie zunächst die "Locking Demo" GUI und verbinden Sie sich
-- auf beiden Seiten mit der Derby-Datenbank.
-- Entweder mit der voreingestellten "in-memory" Datenbank oder
-- mit einer laufenden Derby-Instanz (z.B. jdbc:derby://localhost:1527/DBIDB).

-- Demonstrate locking in the HR schema

SET CURRENT SCHEMA = HRL;

-- Folgende SQL in verschiedenen Isolation Levels ausprobieren
-- oder mit der WITH [ UR | CS | RS | RR ] Klausel angehängt:
SELECT * FROM EMP;
SELECT * FROM EMP FOR READ ONLY;

-- Aggregatfunktionen, Joins
SELECT avg(salary) FROM EMP;

SELECT * FROM EMP
NATURAL JOIN DEPT;
  
-- Folgende SQLs links bzw. rechts eintippen und ausführen.
-- Rechte bzw. linke Seite committen.

SELECT AVG(salary) FROM EMP WITH RS;
UPDATE EMP  SET salary = salary + 200 WHERE eid = 103;

-- Experimentieren Sie mit verschiedenen Isolation Levels links.
SELECT AVG(salary) FROM EMP WITH RR;
INSERT INTO EMP VALUES ( 142, 'Schwartz', 50, 8765);

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







-- Klausuraufgabe aus DBMS-Implementierungen


-- Links -------------------------------  -- Rechts --------------------------------
CREATE TABLE STAR
(ID INTEGER, Name VARCHAR(20));
              
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
