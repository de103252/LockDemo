CREATE SCHEMA SAMPLE;
SET CURRENT SCHEMA = SAMPLE;


CREATE TABLE ACT (
    ACTNO   SMALLINT    NOT NULL,
    ACTKWD  CHAR(6)     NOT NULL,
    ACTDESC VARCHAR(20) NOT NULL,
    
    CONSTRAINT ACT_ACTNO PRIMARY KEY(ACTNO)
  );

CREATE TABLE DEPT (
    DEPTNO   CHAR(3)     NOT NULL,
    DEPTNAME VARCHAR(36) NOT NULL,
    MGRNO    CHAR(6)     DEFAULT NULL,
    ADMRDEPT CHAR(3)     NOT NULL,
    LOCATION CHAR(16)    DEFAULT NULL,
    
    CONSTRAINT DEPT_DEPTNO PRIMARY KEY(DEPTNO)
  );

CREATE TABLE EMP (
    EMPNO     CHAR(6)       NOT NULL,
    FIRSTNME  VARCHAR(12)   NOT NULL,
    MIDINIT   CHAR(1)       NOT NULL,
    LASTNAME  VARCHAR(15)   NOT NULL,
    WORKDEPT  CHAR(3)       DEFAULT NULL,
    PHONENO   CHAR(4)       DEFAULT NULL,
    HIREDATE  DATE          DEFAULT NULL,
    JOB       CHAR(8)       DEFAULT NULL,
    EDLEVEL   SMALLINT      DEFAULT NULL,
    SEX       CHAR(1)       DEFAULT NULL,
    BIRTHDATE DATE          DEFAULT NULL,
    SALARY    DECIMAL(9, 2) DEFAULT NULL,
    BONUS     DECIMAL(9, 2) DEFAULT NULL,
    COMM      DECIMAL(9, 2) DEFAULT NULL,
    
    CONSTRAINT EMP_EMPNO PRIMARY KEY(EMPNO)
  );

CREATE TABLE EMPPROJACT (
    EMPNO    CHAR(6)       NOT NULL,
    PROJNO   CHAR(6)       NOT NULL,
    ACTNO    SMALLINT      NOT NULL,
    EMPTIME  DECIMAL(5, 2) DEFAULT NULL,
    EMSTDATE DATE          DEFAULT NULL,
    EMENDATE DATE          DEFAULT NULL
  );
  

CREATE TABLE PROJ (
    PROJNO   CHAR(6)       NOT NULL,
    PROJNAME VARCHAR(24)   NOT NULL WITH DEFAULT 'PROJECT NAME UNDEFINED',
    DEPTNO   CHAR(3)       NOT NULL,
    RESPEMP  CHAR(6)       NOT NULL,
    PRSTAFF  DECIMAL(5, 2) DEFAULT NULL,
    PRSTDATE DATE          DEFAULT NULL,
    PRENDATE DATE          DEFAULT NULL,
    MAJPROJ  CHAR(6)       DEFAULT NULL,
    
    CONSTRAINT PROJ_PROJNO PRIMARY KEY(PROJNO)
  );
  

CREATE TABLE PROJACT (
    PROJNO   CHAR(6)       NOT NULL,
    ACTNO    SMALLINT      NOT NULL,
    ACSTAFF  DECIMAL(5, 2) DEFAULT NULL,
    ACSTDATE DATE          NOT NULL,
    ACENDATE DATE          DEFAULT NULL,
    
    CONSTRAINT PROJACT_PROJNO PRIMARY KEY(PROJNO, ACTNO, ACSTDATE)
  );
  

CREATE INDEX XDEPT2
    ON DEPT(MGRNO);

CREATE INDEX XDEPT3
    ON DEPT(ADMRDEPT);

CREATE INDEX XEMP2
    ON EMP(WORKDEPT);

CREATE INDEX XEMPPROJACT2
    ON EMPPROJACT(EMPNO);

CREATE INDEX XPROJ2
    ON PROJ(RESPEMP);

CREATE UNIQUE INDEX XACT2
    ON ACT(ACTKWD);

CREATE UNIQUE INDEX XEMPPROJACT1
    ON EMPPROJACT(PROJNO, ACTNO, EMSTDATE, EMPNO);

ALTER TABLE EMP
  ADD CONSTRAINT EMP_NUMBER 
           CHECK (PHONENO >= '0000' AND PHONENO <= '9999');

ALTER TABLE EMP
  ADD CONSTRAINT EMP_PERSON 
           CHECK (SEX = 'M' OR SEX = 'F');

ALTER TABLE DEPT
  ADD CONSTRAINT DEPT_RDD
     FOREIGN KEY(ADMRDEPT)
      REFERENCES DEPT(DEPTNO)
      ON DELETE CASCADE;

ALTER TABLE DEPT
  ADD CONSTRAINT DEPT_RDE
     FOREIGN KEY(MGRNO)
      REFERENCES EMP(EMPNO)
       ON DELETE SET NULL;

ALTER TABLE EMP
  ADD CONSTRAINT EMP_RED 
     FOREIGN KEY(WORKDEPT)
      REFERENCES DEPT(DEPTNO)
       ON DELETE SET NULL;

ALTER TABLE EMPPROJACT
  ADD CONSTRAINT EMPPROJACT_REPAE 
     FOREIGN KEY(EMPNO)
      REFERENCES EMP(EMPNO)
       ON DELETE RESTRICT;

ALTER TABLE EMPPROJACT 
  ADD CONSTRAINT EMPPROJACT_REPAPA 
     FOREIGN KEY(PROJNO, ACTNO, EMSTDATE)
      REFERENCES PROJACT(PROJNO, ACTNO, ACSTDATE)
       ON DELETE RESTRICT;

ALTER TABLE PROJ 
  ADD CONSTRAINT PROJ_DEPTNO
     FOREIGN KEY(DEPTNO)
      REFERENCES DEPT(DEPTNO)
       ON DELETE RESTRICT;

ALTER TABLE PROJ
  ADD CONSTRAINT PROJ_RESPEMP 
     FOREIGN KEY(RESPEMP)
      REFERENCES EMP(EMPNO)
    ON DELETE RESTRICT;

ALTER TABLE PROJ 
  ADD CONSTRAINT PROJ_RPP 
     FOREIGN KEY(MAJPROJ)
      REFERENCES PROJ(PROJNO)
       ON DELETE CASCADE;

ALTER TABLE PROJACT 
  ADD CONSTRAINT PROJACT_RPAA 
     FOREIGN KEY(ACTNO)
      REFERENCES ACT(ACTNO)
       ON DELETE RESTRICT;

ALTER TABLE PROJACT 
  ADD CONSTRAINT PROJACT_RPAP 
     FOREIGN KEY(PROJNO)
      REFERENCES PROJ(PROJNO)
       ON DELETE RESTRICT;

   
insert into EMP(EMPNO, FIRSTNME, MIDINIT, LASTNAME, WORKDEPT, PHONENO, HIREDATE, JOB, EDLEVEL, SEX, BIRTHDATE, SALARY, BONUS, COMM)
values ('000010', 'CHRISTINE', 'I', 'HAAS', NULL, '3978', '1965-01-01', 'PRES', 18, 'F', '1933-08-14', 52750.00, 1000.00, 4220.00)
     , ('000110', 'VINCENZO', 'G', 'LUCCHESI', NULL, '3490', '1958-05-16', 'SALESREP', 19, 'M', '1929-11-05', 46500.00, 900.00, 3720.00)
     , ('000120', 'SEAN', '', 'O''CONNELL', NULL, '2167', '1963-12-05', 'CLERK', 14, 'M', '1942-10-18', 29250.00, 600.00, 2340.00)
     , ('200010', 'DIAN', 'J', 'HEMMINGER', NULL, '3978', '1965-01-01', 'SALESREP', 18, 'F', '1933-08-14', 46500.00, 1000.00, 4220.00)
     , ('200120', 'GREG', '', 'ORLANDO', NULL, '2167', '1972-05-05', 'CLERK', 14, 'M', '1942-10-18', 29250.00, 600.00, 2340.00)
     , ('000020', 'MICHAEL', 'L', 'THOMPSON', NULL, '3476', '1973-10-10', 'MANAGER', 18, 'M', '1948-02-02', 41250.00, 800.00, 3300.00)
     , ('000030', 'SALLY', 'A', 'KWAN', NULL, '4738', '1975-04-05', 'MANAGER', 20, 'F', '1941-05-11', 38250.00, 800.00, 3060.00)
     , ('000130', 'DOLORES', 'M', 'QUINTANA', NULL, '4578', '1971-07-28', 'ANALYST', 16, 'F', '1925-09-15', 23800.00, 500.00, 1904.00)
     , ('000140', 'HEATHER', 'A', 'NICHOLLS', NULL, '1793', '1976-12-15', 'ANALYST', 18, 'F', '1946-01-19', 28420.00, 600.00, 2274.00)
     , ('200140', 'KIM', 'N', 'NATZ', NULL, '1793', '1976-12-15', 'ANALYST', 18, 'F', '1946-01-19', 28420.00, 600.00, 2274.00)
     , ('000060', 'IRVING', 'F', 'STERN', NULL, '6423', '1973-09-14', 'MANAGER', 16, 'M', '1945-07-07', 32250.00, 600.00, 2580.00)
     , ('000150', 'BRUCE', '', 'ADAMSON', NULL, '4510', '1972-02-12', 'DESIGNER', 16, 'M', '1947-05-17', 25280.00, 500.00, 2022.00)
     , ('000160', 'ELIZABETH', 'R', 'PIANKA', NULL, '3782', '1977-10-11', 'DESIGNER', 17, 'F', '1955-04-12', 22250.00, 400.00, 1780.00)
     , ('000170', 'MASATOSHI', 'J', 'YOSHIMURA', NULL, '2890', '1978-09-15', 'DESIGNER', 16, 'M', '1951-01-05', 24680.00, 500.00, 1974.00)
     , ('000180', 'MARILYN', 'S', 'SCOUTTEN', NULL, '1682', '1973-07-07', 'DESIGNER', 17, 'F', '1949-02-21', 21340.00, 500.00, 1707.00)
     , ('000190', 'JAMES', 'H', 'WALKER', NULL, '2986', '1974-07-26', 'DESIGNER', 16, 'M', '1952-06-25', 20450.00, 400.00, 1636.00)
     , ('000200', 'DAVID', '', 'BROWN', NULL, '4501', '1966-03-03', 'DESIGNER', 16, 'M', '1941-05-29', 27740.00, 600.00, 2217.00)
     , ('000210', 'WILLIAM', 'T', 'JONES', NULL, '0942', '1979-04-11', 'DESIGNER', 17, 'M', '1953-02-23', 18270.00, 400.00, 1462.00)
     , ('000220', 'JENNIFER', 'K', 'LUTZ', NULL, '0672', '1968-08-29', 'DESIGNER', 18, 'F', '1948-03-19', 29840.00, 600.00, 2387.00)
     , ('200170', 'KIYOSHI', '', 'YAMAMOTO', NULL, '2890', '1978-09-15', 'DESIGNER', 16, 'M', '1951-01-05', 24680.00, 500.00, 1974.00)
     , ('200220', 'REBA', 'K', 'JOHN', NULL, '0672', '1968-08-29', 'DESIGNER', 18, 'F', '1948-03-19', 29840.00, 600.00, 2387.00)
     , ('000070', 'EVA', 'D', 'PULASKI', NULL, '7831', '1980-09-30', 'MANAGER', 16, 'F', '1953-05-26', 36170.00, 700.00, 2893.00)
     , ('000230', 'JAMES', 'J', 'JEFFERSON', NULL, '2094', '1966-11-21', 'CLERK', 14, 'M', '1935-05-30', 22180.00, 400.00, 1774.00)
     , ('000240', 'SALVATORE', 'M', 'MARINO', NULL, '3780', '1979-12-05', 'CLERK', 17, 'M', '1954-03-31', 28760.00, 600.00, 2301.00)
     , ('000250', 'DANIEL', 'S', 'SMITH', NULL, '0961', '1969-10-30', 'CLERK', 15, 'M', '1939-11-12', 19180.00, 400.00, 1534.00)
     , ('000260', 'SYBIL', 'V', 'JOHNSON', NULL, '8953', '1975-09-11', 'CLERK', 16, 'F', '1936-10-05', 17250.00, 300.00, 1380.00)
     , ('000270', 'MARIA', 'L', 'PEREZ', NULL, '9001', '1980-09-30', 'CLERK', 15, 'F', '1953-05-26', 27380.00, 500.00, 2190.00)
     , ('200240', 'ROBERT', 'M', 'MONTEVERDE', NULL, '3780', '1979-12-05', 'CLERK', 17, 'M', '1954-03-31', 28760.00, 600.00, 2301.00)
     , ('000050', 'JOHN', 'B', 'GEYER', NULL, '6789', '1949-08-17', 'MANAGER', 16, 'M', '1925-09-15', 40175.00, 800.00, 3214.00)
     , ('000090', 'EILEEN', 'W', 'HENDERSON', NULL, '5498', '1970-08-15', 'MANAGER', 16, 'F', '1941-05-15', 29750.00, 600.00, 2380.00)
     , ('000280', 'ETHEL', 'R', 'SCHNEIDER', NULL, '8997', '1967-03-24', 'OPERATOR', 17, 'F', '1936-03-28', 26250.00, 500.00, 2100.00)
     , ('000290', 'JOHN', 'R', 'PARKER', NULL, '4502', '1980-05-30', 'OPERATOR', 12, 'M', '1946-07-09', 15340.00, 300.00, 1227.00)
     , ('000300', 'PHILIP', 'X', 'SMITH', NULL, '2095', '1972-06-19', 'OPERATOR', 14, 'M', '1936-10-27', 17750.00, 400.00, 1420.00)
     , ('000310', 'MAUDE', 'F', 'SETRIGHT', NULL, '3332', '1964-09-12', 'OPERATOR', 12, 'F', '1931-04-21', 15900.00, 300.00, 1272.00)
     , ('200280', 'EILEEN', 'R', 'SCHWARTZ', NULL, '8997', '1967-03-24', 'OPERATOR', 17, 'F', '1936-03-28', 26250.00, 500.00, 2100.00)
     , ('200310', 'MICHELLE', 'F', 'SPRINGER', NULL, '3332', '1964-09-12', 'OPERATOR', 12, 'F', '1931-04-21', 15900.00, 300.00, 1272.00)
     , ('000100', 'THEODORE', 'Q', 'SPENSER', NULL, '0972', '1980-06-19', 'MANAGER', 14, 'M', '1956-12-18', 26150.00, 500.00, 2092.00)
     , ('000320', 'RAMLAL', 'V', 'MEHTA', NULL, '9990', '1965-07-07', 'FIELDREP', 16, 'M', '1932-08-11', 19950.00, 400.00, 1596.00)
     , ('000330', 'WING', '', 'LEE', NULL, '2103', '1976-02-23', 'FIELDREP', 14, 'M', '1941-07-18', 25370.00, 500.00, 2030.00)
     , ('000340', 'JASON', 'R', 'GOUNOT', NULL, '5698', '1947-05-05', 'FIELDREP', 16, 'M', '1926-05-17', 23840.00, 500.00, 1907.00)
     , ('200330', 'HELENA', '', 'WONG', NULL, '2103', '1976-02-23', 'FIELDREP', 14, 'F', '1941-07-18', 25370.00, 500.00, 2030.00)
     , ('200340', 'ROY', 'R', 'ALONZO', NULL, '5698', '1947-05-05', 'FIELDREP', 16, 'M', '1926-05-17', 23840.00, 500.00, 1907.00)
;     

-- DEPT table
insert into DEPT(DEPTNO, DEPTNAME, MGRNO, ADMRDEPT, LOCATION)
values ('A00', 'SPIFFY COMPUTER SERVICE DIV.', '000010', 'A00', '')
     , ('B01', 'PLANNING', '000020', 'A00', '')
     , ('C01', 'INFORMATION CENTER', '000030', 'A00', '')
     , ('D01', 'DEVELOPMENT CENTER', NULL, 'A00', '')
     , ('D11', 'MANUFACTURING SYSTEMS', '000060', 'D01', '')
     , ('D21', 'ADMINISTRATION SYSTEMS', '000070', 'D01', '')
     , ('E01', 'SUPPORT SERVICES', '000050', 'A00', '')
     , ('E11', 'OPERATIONS', '000090', 'E01', '')
     , ('E21', 'SOFTWARE SUPPORT', '000100', 'E01', '')
     , ('F22', 'BRANCH OFFICE F2', NULL, 'E01', '')
     , ('G22', 'BRANCH OFFICE G2', NULL, 'E01', '')
     , ('H22', 'BRANCH OFFICE H2', NULL, 'E01', '')
     , ('I22', 'BRANCH OFFICE I2', NULL, 'E01', '')
     , ('J22', 'BRANCH OFFICE J2', NULL, 'E01', '')
;

update emp
   set workdept = 'A00'
 where empno in ('000010', '000110', '000120', '200010', '200120'); 
 
update emp
   set workdept = 'B01'
 where empno in ('000020'); 
 
update emp
   set workdept = 'C01'
 where empno in ('000030', '000130', '000140', '200140');
 
update emp
   set workdept = 'D11'
 where empno in ('000060', '000150', '000160', '000170', '000180', '000190',
                 '000200', '000210', '000220', '200170','200220');
 
update emp
   set workdept = 'D21'
 where empno in ('000070', '000230', '000240', '000250', '000260', '000270', '200240');
  
update emp
   set workdept = 'E01'
 where empno in ('000050');
 
update emp
   set workdept = 'E11'
 where empno in ('000090', '000280', '000290', '000300', '000310', '200280', '200310'); 
 
update emp
   set workdept = 'E21'
 where empno in ('000100', '000320', '000330', '000340', '200330', '200340'); 

insert into PROJ(PROJNO, PROJNAME, DEPTNO, RESPEMP, PRSTAFF, PRSTDATE, PRENDATE, MAJPROJ)
values ('AD3100', 'ADMIN SERVICES', 'D01', '000010', 6.50, '1982-01-01', '1983-02-01', NULL)
     , ('AD3110', 'GENERAL AD SYSTEMS', 'D21', '000070', 6.00, '1982-01-01', '1983-02-01', 'AD3100')
     , ('AD3111', 'PAYROLL PROGRAMMING', 'D21', '000230', 2.00, '1982-01-01', '1983-02-01', 'AD3110')
     , ('AD3112', 'PERSONNEL PROGRAMMG', 'D21', '000250', 1.00, '1982-01-01', '1983-02-01', 'AD3110')
     , ('AD3113', 'ACCOUNT.PROGRAMMING', 'D21', '000270', 2.00, '1982-01-01', '1983-02-01', 'AD3110')
     , ('IF1000', 'QUERY SERVICES', 'C01', '000030', 2.00, '1982-01-01', '1983-02-01', NULL)
     , ('IF2000', 'USER EDUCATION', 'C01', '000030', 1.00, '1982-01-01', '1983-02-01', NULL)
     , ('MA2100', 'WELD LINE AUTOMATION', 'D01', '000010', 12.00, '1982-01-01', '1983-02-01', NULL)
     , ('MA2110', 'W L PROGRAMMING', 'D11', '000060', 9.00, '1982-01-01', '1983-02-01', 'MA2100')
     , ('MA2111', 'W L PROGRAM DESIGN', 'D11', '000220', 2.00, '1982-01-01', '1982-12-01', 'MA2110')
     , ('MA2112', 'W L ROBOT DESIGN', 'D11', '000150', 3.00, '1982-01-01', '1982-12-01', 'MA2110')
     , ('MA2113', 'W L PROD CONT PROGS', 'D11', '000160', 3.00, '1982-02-15', '1982-12-01', 'MA2110')
     , ('OP1000', 'OPERATION SUPPORT', 'E01', '000050', 6.00, '1982-01-01', '1983-02-01', NULL)
     , ('OP1010', 'OPERATION', 'E11', '000090', 5.00, '1982-01-01', '1983-02-01', 'OP1000')
     , ('OP2000', 'GEN SYSTEMS SERVICES', 'E01', '000050', 5.00, '1982-01-01', '1983-02-01', NULL)
     , ('OP2010', 'SYSTEMS SUPPORT', 'E21', '000100', 4.00, '1982-01-01', '1983-02-01', 'OP2000')
     , ('OP2011', 'SCP SYSTEMS SUPPORT', 'E21', '000320', 1.00, '1982-01-01', '1983-02-01', 'OP2010')
     , ('OP2012', 'APPLICATIONS SUPPORT', 'E21', '000330', 1.00, '1982-01-01', '1983-02-01', 'OP2010')
     , ('OP2013', 'DB/DC SUPPORT', 'E21', '000340', 1.00, '1982-01-01', '1983-02-01', 'OP2010')
     , ('PL2100', 'WELD LINE PLANNING', 'B01', '000020', 1.00, '1982-01-01', '1982-09-15', 'MA2100')
;     

insert into ACT(ACTNO, ACTKWD, ACTDESC)
values (10, 'MANAGE', 'MANAGE/ADVISE')
     , (20, 'ECOST', 'ESTIMATE COST')
     , (30, 'DEFINE', 'DEFINE SPECS')
     , (40, 'LEADPR', 'LEAD PROGRAM/DESIGN')
     , (50, 'SPECS', 'WRITE SPECS')
     , (60, 'LOGIC', 'DESCRIBE LOGIC')
     , (70, 'CODE', 'CODE PROGRAMS')
     , (80, 'TEST', 'TEST PROGRAMS')
     , (90, 'ADMQS', 'ADM QUERY SYSTEM')
     , (100, 'TEACH', 'TEACH CLASSES')
     , (110, 'COURSE', 'DEVELOP COURSES')
     , (120, 'STAFF', 'PERS AND STAFFING')
     , (130, 'OPERAT', 'OPER COMPUTER SYS')
     , (140, 'MAINT', 'MAINT SOFTWARE SYS')
     , (150, 'ADMSYS', 'ADM OPERATING SYS')
     , (160, 'ADMDB', 'ADM DATA BASES')
     , (170, 'ADMDC', 'ADM DATA COMM')
     , (180, 'DOC', 'DOCUMENT')
;
      
insert into PROJACT(PROJNO, ACTNO, ACSTAFF, ACSTDATE, ACENDATE)
values ('AD3100', 10, 0.50, '1982-01-01', '1982-07-01')
     , ('AD3110', 10, 1.00, '1982-01-01', '1983-01-01')
     , ('AD3111', 60, 0.50, '1982-03-15', '1982-04-15')
     , ('AD3111', 60, 0.80, '1982-01-01', '1982-04-15')
     , ('AD3111', 70, 0.50, '1982-03-15', '1982-10-15')
     , ('AD3111', 70, 1.50, '1982-02-15', '1982-10-15')
     , ('AD3111', 80, 1.00, '1982-09-15', '1983-01-01')
     , ('AD3111', 80, 1.25, '1982-04-15', '1983-01-15')
     , ('AD3111', 180, 1.00, '1982-10-15', '1983-01-15')
     , ('AD3112', 60, 0.50, '1982-02-01', '1982-03-15')
     , ('AD3112', 60, 0.75, '1982-01-01', '1982-03-15')
     , ('AD3112', 60, 0.75, '1982-12-01', '1983-01-01')
     , ('AD3112', 60, 1.00, '1983-01-01', '1983-02-01')
     , ('AD3112', 70, 0.25, '1982-08-15', '1982-10-15')
     , ('AD3112', 70, 0.50, '1982-02-01', '1982-03-15')
     , ('AD3112', 70, 0.75, '1982-01-01', '1982-10-15')
     , ('AD3112', 70, 1.00, '1982-03-15', '1982-08-15')
     , ('AD3112', 80, 0.35, '1982-08-15', '1982-12-01')
     , ('AD3112', 80, 0.50, '1982-10-15', '1982-12-01')
     , ('AD3112', 180, 0.50, '1982-08-15', '1983-01-01')
     , ('AD3113', 60, 0.25, '1982-09-01', '1982-10-15')
     , ('AD3113', 60, 0.75, '1982-03-01', '1982-10-15')
     , ('AD3113', 60, 1.00, '1982-04-01', '1982-09-01')
     , ('AD3113', 70, 0.50, '1982-06-15', '1982-07-01')
     , ('AD3113', 70, 0.75, '1982-09-01', '1982-10-15')
     , ('AD3113', 70, 1.00, '1982-07-01', '1983-02-01')
     , ('AD3113', 70, 1.00, '1982-10-15', '1983-02-01')
     , ('AD3113', 70, 1.25, '1982-06-01', '1982-12-15')
     , ('AD3113', 80, 0.50, '1982-03-01', '1982-04-15')
     , ('AD3113', 80, 1.75, '1982-01-01', '1982-04-15')
     , ('AD3113', 180, 0.50, '1982-06-01', '1982-07-01')
     , ('AD3113', 180, 0.75, '1982-03-01', '1982-07-01')
     , ('AD3113', 180, 1.00, '1982-04-15', '1982-06-01')
     , ('IF1000', 10, 0.50, '1982-01-01', '1983-01-01')
     , ('IF1000', 10, 0.50, '1982-06-01', '1983-01-01')
     , ('IF1000', 90, 0.50, '1982-10-01', '1983-01-01')
     , ('IF1000', 90, 1.00, '1982-01-01', '1983-01-01')
     , ('IF1000', 100, 0.50, '1982-10-01', '1983-01-01')
     , ('IF2000', 10, 0.50, '1982-01-01', '1983-01-01')
     , ('IF2000', 100, 0.50, '1982-03-01', '1982-07-01')
     , ('IF2000', 100, 0.75, '1982-01-01', '1982-07-01')
     , ('IF2000', 110, 0.50, '1982-03-01', '1982-07-01')
     , ('IF2000', 110, 0.50, '1982-10-01', '1983-01-01')
     , ('MA2100', 10, 0.50, '1982-01-01', '1982-11-01')
     , ('MA2100', 20, 1.00, '1982-01-01', '1982-03-01')
     , ('MA2110', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('MA2111', 40, 1.00, '1982-01-01', '1983-02-01')
     , ('MA2111', 50, 1.00, '1982-01-01', '1982-06-01')
     , ('MA2111', 60, 1.00, '1982-06-01', '1983-02-01')
     , ('MA2111', 60, 1.00, '1982-06-15', '1983-02-01')
     , ('MA2112', 60, 2.00, '1982-01-01', '1982-07-01')
     , ('MA2112', 70, 1.00, '1982-02-01', '1982-10-01')
     , ('MA2112', 70, 1.00, '1982-06-01', '1983-02-01')
     , ('MA2112', 70, 1.50, '1982-02-15', '1983-02-01')
     , ('MA2112', 80, 1.00, '1982-10-01', '1983-10-01')
     , ('MA2112', 180, 1.00, '1982-07-01', '1983-02-01')
     , ('MA2112', 180, 1.00, '1982-07-15', '1983-02-01')
     , ('MA2113', 60, 1.00, '1982-02-15', '1982-09-01')
     , ('MA2113', 60, 1.00, '1982-07-15', '1983-02-01')
     , ('MA2113', 70, 2.00, '1982-04-01', '1983-12-15')
     , ('MA2113', 80, 0.50, '1982-10-01', '1983-02-01')
     , ('MA2113', 80, 1.00, '1982-01-01', '1983-02-01')
     , ('MA2113', 80, 1.50, '1982-09-01', '1983-02-01')
     , ('MA2113', 180, 0.50, '1982-10-01', '1983-01-01')
     , ('OP1000', 10, 0.25, '1982-01-01', '1983-02-01')
     , ('OP1010', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('OP1010', 130, 4.00, '1982-01-01', '1983-02-01')
     , ('OP2000', 50, 0.75, '1982-01-01', '1983-02-01')
     , ('OP2010', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('OP2011', 140, 0.75, '1982-01-01', '1983-02-01')
     , ('OP2011', 150, 0.25, '1982-01-01', '1983-02-01')
     , ('OP2012', 140, 0.25, '1982-01-01', '1983-02-01')
     , ('OP2012', 160, 0.75, '1982-01-01', '1983-02-01')
     , ('OP2013', 140, 0.50, '1982-01-01', '1983-02-01')
     , ('OP2013', 170, 0.50, '1982-01-01', '1983-02-01')
     , ('PL2100', 30, 1.00, '1982-01-01', '1982-09-15')
     , ('PL2100', 30, 1.00, '1982-02-01', '1982-09-01')
;

insert into EMPPROJACT(EMPNO, PROJNO, ACTNO, EMPTIME, EMSTDATE, EMENDATE)
values ('000130', 'IF1000', 90, 1.00, '1982-01-01', '1982-10-01')
     , ('000130', 'IF1000', 100, 0.50, '1982-10-01', '1983-01-01')
     , ('000140', 'IF1000', 90, 0.50, '1982-10-01', '1983-01-01')
     , ('000030', 'IF1000', 10, 0.50, '1982-06-01', '1983-01-01')
     , ('000030', 'IF2000', 10, 0.50, '1982-01-01', '1983-01-01')
     , ('000140', 'IF2000', 100, 1.00, '1982-01-01', '1982-03-01')
     , ('000140', 'IF2000', 100, 0.50, '1982-03-01', '1982-07-01')
     , ('000140', 'IF2000', 110, 0.50, '1982-03-01', '1982-07-01')
     , ('000140', 'IF2000', 110, 0.50, '1982-10-01', '1983-01-01')
     , ('000010', 'MA2100', 10, 0.50, '1982-01-01', '1982-11-01')
     , ('000110', 'MA2100', 20, 1.00, '1982-01-01', '1982-03-01')
     , ('000020', 'PL2100', 30, 1.00, '1982-01-01', '1982-09-15')
     , ('000010', 'MA2110', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('000220', 'MA2111', 40, 1.00, '1982-01-01', '1983-02-01')
     , ('000200', 'MA2111', 50, 1.00, '1982-01-01', '1982-06-15')
     , ('000200', 'MA2111', 60, 1.00, '1982-06-15', '1983-02-01')
     , ('000150', 'MA2112', 60, 1.00, '1982-01-01', '1982-07-15')
     , ('000150', 'MA2112', 180, 1.00, '1982-07-15', '1983-02-01')
     , ('000170', 'MA2112', 60, 1.00, '1982-01-01', '1983-06-01')
     , ('000170', 'MA2112', 70, 1.00, '1982-06-01', '1983-02-01')
     , ('000190', 'MA2112', 70, 1.00, '1982-02-01', '1982-10-01')
     , ('000190', 'MA2112', 80, 1.00, '1982-10-01', '1983-10-01')
     , ('000160', 'MA2113', 60, 1.00, '1982-07-15', '1983-02-01')
     , ('000170', 'MA2113', 80, 1.00, '1982-01-01', '1983-02-01')
     , ('000180', 'MA2113', 70, 1.00, '1982-04-01', '1982-06-15')
     , ('000210', 'MA2113', 80, 0.50, '1982-10-01', '1983-02-01')
     , ('000210', 'MA2113', 180, 0.50, '1982-10-01', '1983-02-01')
     , ('000010', 'AD3100', 10, 0.50, '1982-01-01', '1982-07-01')
     , ('000070', 'AD3110', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('000230', 'AD3111', 60, 1.00, '1982-01-01', '1982-03-15')
     , ('000230', 'AD3111', 60, 0.50, '1982-03-15', '1982-04-15')
     , ('000230', 'AD3111', 70, 0.50, '1982-03-15', '1982-10-15')
     , ('000230', 'AD3111', 80, 0.50, '1982-04-15', '1982-10-15')
     , ('000230', 'AD3111', 180, 1.00, '1982-10-15', '1983-01-01')
     , ('000240', 'AD3111', 70, 1.00, '1982-02-15', '1982-09-15')
     , ('000240', 'AD3111', 80, 1.00, '1982-09-15', '1983-01-01')
     , ('000250', 'AD3112', 60, 1.00, '1982-01-01', '1982-02-01')
     , ('000250', 'AD3112', 60, 0.50, '1982-02-01', '1982-03-15')
     , ('000250', 'AD3112', 60, 0.50, '1982-12-01', '1983-01-01')
     , ('000250', 'AD3112', 60, 1.00, '1983-01-01', '1983-02-01')
     , ('000250', 'AD3112', 70, 0.50, '1982-02-01', '1982-03-15')
     , ('000250', 'AD3112', 70, 1.00, '1982-03-15', '1982-08-15')
     , ('000250', 'AD3112', 70, 0.25, '1982-08-15', '1982-10-15')
     , ('000250', 'AD3112', 80, 0.25, '1982-08-15', '1982-10-15')
     , ('000250', 'AD3112', 80, 0.50, '1982-10-15', '1982-12-01')
     , ('000250', 'AD3112', 180, 0.50, '1982-08-15', '1983-01-01')
     , ('000270', 'AD3113', 60, 0.50, '1982-03-01', '1982-04-01')
     , ('000270', 'AD3113', 60, 1.00, '1982-04-01', '1982-09-01')
     , ('000270', 'AD3113', 60, 0.25, '1982-09-01', '1982-10-15')
     , ('000270', 'AD3113', 70, 0.75, '1982-09-01', '1982-10-15')
     , ('000270', 'AD3113', 70, 1.00, '1982-10-15', '1983-02-01')
     , ('000270', 'AD3113', 80, 1.00, '1982-01-01', '1982-03-01')
     , ('000270', 'AD3113', 80, 0.50, '1982-03-01', '1982-04-01')
     , ('000260', 'AD3113', 70, 0.50, '1982-06-15', '1982-07-01')
     , ('000260', 'AD3113', 70, 1.00, '1982-07-01', '1983-02-01')
     , ('000260', 'AD3113', 80, 1.00, '1982-01-01', '1982-03-01')
     , ('000260', 'AD3113', 80, 0.50, '1982-03-01', '1982-04-15')
     , ('000260', 'AD3113', 180, 0.50, '1982-03-01', '1982-04-15')
     , ('000260', 'AD3113', 180, 1.00, '1982-04-15', '1982-06-01')
     , ('000260', 'AD3113', 180, 0.50, '1982-06-01', '1982-07-01')
     , ('000050', 'OP1000', 10, 0.25, '1982-01-01', '1983-02-01')
     , ('000090', 'OP1010', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('000280', 'OP1010', 130, 1.00, '1982-01-01', '1983-02-01')
     , ('000290', 'OP1010', 130, 1.00, '1982-01-01', '1983-02-01')
     , ('000300', 'OP1010', 130, 1.00, '1982-01-01', '1983-02-01')
     , ('000310', 'OP1010', 130, 1.00, '1982-01-01', '1983-02-01')
     , ('000050', 'OP2010', 10, 0.75, '1982-01-01', '1983-02-01')
     , ('000100', 'OP2010', 10, 1.00, '1982-01-01', '1983-02-01')
     , ('000320', 'OP2011', 140, 0.75, '1982-01-01', '1983-02-01')
     , ('000320', 'OP2011', 150, 0.25, '1982-01-01', '1983-02-01')
     , ('000330', 'OP2012', 140, 0.25, '1982-01-01', '1983-02-01')
     , ('000330', 'OP2012', 160, 0.75, '1982-01-01', '1983-02-01')
     , ('000340', 'OP2013', 140, 0.50, '1982-01-01', '1983-02-01')
     , ('000340', 'OP2013', 170, 0.50, '1982-01-01', '1983-02-01')
;
   