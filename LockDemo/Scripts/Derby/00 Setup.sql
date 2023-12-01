CREATE SCHEMA HRL;
SET CURRENT SCHEMA = HRL;


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
  ( 10, 'Administration')
, ( 20, 'Marketing')
, ( 30, 'Purchasing')
, ( 40, 'Human Resources')
, ( 50, 'Shipping')
, ( 60, 'IT')
, ( 70, 'Public Relations')
, ( 80, 'Sales')
, ( 90, 'Executive')
, ( 100, 'Finance')
, ( 110, 'Accounting')
, ( 120, 'Treasury')
, ( 130, 'Corporate Tax')
, ( 140, 'Control And Credit')
, ( 150, 'Shareholder Services')
, ( 160, 'Benefits')
, ( 170, 'Manufacturing')
, ( 180, 'Construction')
, ( 190, 'Contracting')
, ( 200, 'Operations')
, ( 210, 'IT Support')
, ( 220, 'NOC')
, ( 230, 'IT Helpdesk')
, ( 240, 'Government Sales')
, ( 250, 'Retail Sales')
, ( 260, 'Recruiting')
, ( 270, 'Payroll')
;

INSERT INTO emp(eid, name, salary, did)
VALUES
  ( 100, 'King',       24000,  90)
, ( 101, 'Kochhar',    17000,  90)
, ( 102, 'De Haan',    17000,  90)
, ( 103, 'Hunold',      9000,  60)
, ( 104, 'Ernst',       6000,  60)
, ( 105, 'Austin',      4800,  60)
, ( 106, 'Pataballa',   4800,  60)
, ( 107, 'Lorentz',     4200,  60)
, ( 108, 'Greenberg',  12008, 100)
, ( 109, 'Faviet',      9000, 100)
, ( 110, 'Chen',        8200, 100)
, ( 111, 'Sciarra',     7700, 100)
, ( 112, 'Urman',       7800, 100)
, ( 113, 'Popp',        6900, 100)
, ( 114, 'Raphaely',   11000,  30)
, ( 115, 'Khoo',        3100,  30)
, ( 116, 'Baida',       2900,  30)
, ( 117, 'Tobias',      2800,  30)
, ( 118, 'Himuro',      2600,  30)
, ( 119, 'Colmenares',  2500,  30)
, ( 120, 'Weiss',       8000,  50)