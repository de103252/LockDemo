DROP TABLE xemp;

CREATE TABLE XEMP(
ID INTEGER NOT NULL,
LASTNAME VARCHAR(50) NOT NULL,
SALARY DECIMAL(8, 2)
);

INSERT INTO xemp(ID, LASTNAME, SALARY)
VALUES 
(100, 'Meyer',   6000.00),
(200, 'Smith',   3000.00),
(201, 'Foster',  4000.00)
;

INSERT INTO XEMP (ID, LASTNAME, SALARY)
VALUES (210, 'Miller', 5000.00);

UPDATE XEMP
SET SALARY = SALARY * 1.1
WHERE LASTNAME = 'Smith';
COMMIT;
DELETE FROM XEMP WHERE ID = 201;
ROLLBACK;

SELECT SUM(SALARY) AS TOTAL_SALARY FROM XEMP;


--------

create table emp(empno integer not null, salary decimal(8, 2))
create table proj(projno integer not null, prjstdate date not null)
insert into emp values (100, 12345.67), (101, 55555.55)
insert into proj values (200, current date), (201, current date)

-- 1
select salary from emp where empno = 100 for update

--3
update proj set prjstdate = '20.10.2023' where projno = 200

--2
select prjstdate from proj where projno = 200 for update

--4
re
