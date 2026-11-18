-- H2 setup: same HR sample tables as "Scripts/Derby/00 Setup.sql".
--
-- Connect both panels to the shared in-memory database, using the first
-- H2 entry of the URL list (jdbc:h2:mem:lockdemo ...).
-- Run this script once on either side, then commit.

CREATE TABLE dept(
      did    DECIMAL(4)  CONSTRAINT dept_id_pk   PRIMARY KEY
    , name   VARCHAR(30) NOT NULL
);

CREATE TABLE emp(
      eid    DECIMAL(6)    CONSTRAINT  emp_emp_id_pk
                           PRIMARY KEY
    , name   VARCHAR(25)   NOT NULL
    , salary DECIMAL(9, 2) NOT NULL
                           CONSTRAINT emp_sal_positive
                           CHECK (SALARY >= 0)
    , did    DECIMAL(4)    CONSTRAINT emp_dept_fk
                           REFERENCES dept
);

INSERT INTO dept(did, name) VALUES
  ( 30, 'Purchasing')
, ( 50, 'Shipping')
, ( 60, 'IT')
, ( 90, 'Executive')
, ( 100, 'Finance')
;

INSERT INTO emp(eid, name, salary, did)
VALUES
  ( 100, 'King',       24000,  90)
, ( 101, 'Kochhar',    17000,  90)
, ( 103, 'Hunold',      9000,  60)
, ( 104, 'Ernst',       6000,  60)
, ( 108, 'Greenberg',  12008, 100)
, ( 114, 'Raphaely',   11000,  30)
, ( 120, 'Weiss',       8000,  50)
;

COMMIT;
