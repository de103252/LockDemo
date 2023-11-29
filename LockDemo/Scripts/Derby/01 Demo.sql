-- Starten Sie zunächst die "Locking Demo" GUI und verbinden Sie sich
-- auf beiden Seiten mit der Derby-Datenbank.
-- Entweder mit der voreingestellten "in-memory" Datenbank oder
-- mit einer laufenden Derby-Instanz (z.B. jdbc:derby://localhost:1527/DBIDB).

SET CURRENT SCHEMA = HRL;

-- Folgende SQL in verschiedenen Isolation Levels ausprobieren
-- oder mit der WITH [ UR | CS | RS | RR ] Klausel angehängt:
SELECT * FROM EMPLOYEES;
SELECT * FROM EMPLOYEES FOR READ ONLY;

SELECT * FROM EMPLOYEES
 WHERE EMPLOYEE_ID BETWEEN 120 AND 130
  WITH rr;
  
