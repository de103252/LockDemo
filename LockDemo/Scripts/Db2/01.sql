-- Demonstrate locking in the HR schema

SET CURRENT SCHEMA = HRL;

-- Folgende SQL in verschiedenen Isolation Levels ausprobieren
-- oder mit der WITH [ UR | CS | RS | RR ] Klausel angehängt:
SELECT * FROM EMPLOYEES;
SELECT * FROM EMPLOYEES FOR READ ONLY;

-- Aggregatfunktionen, Joins
SELECT avg(salary) FROM employees;

SELECT * FROM EMPLOYEES
NATURAL JOIN departments;
  
-- Folgende SQLs links bzw. rechts eintippen und ausführen.
-- Rechte bzw. linke Seite committen.
-- Experimentieren Sie mit verschiedenen Isolation Levels links.
SELECT AVG(salary) FROM employees WITH RS;
UPDATE EMPLOYEES  SET salary = salary + 200 WHERE employee_id = 103;

SELECT AVG(salary) FROM employees WITH RR;
INSERT INTO employees VALUES ( 142, 'Schwartz', 50, 8765);

-- Versuchen Sie, ein Deadlock-Szenario herbeizuführen.
-- Hier eine Möglichkeit (erst selbst probieren...):



SELECT * FROM EMPLOYEES WHERE employee_id = 103;
UPDATE DEPARTMENTS SET department_name = 'Handling' WHERE department_id = 50;

SELECT * FROM departments WHERE department_id = 50;
UPDATE EMPLOYEES SET salary = salary * 1.1 WHERE employee_id = 103;

-- Klausuraufgabe aus DBMS-Implementierungen


-- Links -------------------------------  -- Rechts --------------------------------
CREATE TABLE STAR
(ID INTEGER, Name VARCHAR(20));
              
INSERT INTO STAR VALUES (3, 'Miley');
                                          INSERT INTO STAR VALUES (7, 'Billie');
INSERT INTO STAR VALUES (5, 'Taylor');
                                          SELECT * FROM STAR; -- (1)
COMMIT;
                                          DELETE FROM STAR;
SELECT * FROM STAR; -- (2)
                                          ROLLBACK;
                                          SELECT * FROM STAR; -- (3)
COMMIT;
11 COMMIT;