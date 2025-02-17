-- Setup
drop table emp;

create table emp like dsn81210.emp in database use 
  compress no
  ccsid ebcdic;
-- Tablespace LDS: DSNUSE.DSNDBC.USE.EMP.I0001.A001
alter table emp add version integer with default 0 implicitly hidden;

alter tablespace use.emp maxrows 5;
insert into emp select * from dsn81210.emp;

drop table proj;
create table proj like dsn81210.proj in database use compress no ccsid ebcdic;
insert into proj select * from dsn81210.proj;


select digits(decimal(rid(emp) / 256, 5))     page
     , digits(decimal(mod(rid(emp), 256), 3)) row
     , empno
     , lastname
     , firstnme
     , salary
  from emp
  with rs;
  
/*  
PAGE  ROW EMPNO  LASTNAME        FIRSTNME          SALARY
00002 001 000010 HAAS            CHRISTINE    52750.000000000
00002 002 000020 THOMPSON        MICHAEL      41250.000000000
00002 003 000030 KWAN            SALLY        38250.000000000
00002 004 000050 GEYER           JOHN         40175.000000000
00002 005 000060 STERN           IRVING       32250.000000000
00003 001 000070 PULASKI         EVA          36170.000000000
00003 002 000090 HENDERSON       EILEEN       29750.000000000
00003 003 000100 SPENSER         THEODORE     26150.000000000
00003 004 000110 LUCCHESI        VINCENZO     46500.000000000
00003 005 000120 O'CONNELL       SEAN         29250.000000000
00004 001 000130 QUINTANA        DOLORES      23800.000000000
00004 002 000140 NICHOLLS        HEATHER      28420.000000000
00004 003 000150 ADAMSON         BRUCE        25280.000000000
00004 004 000160 PIANKA          ELIZABETH    22250.000000000
00004 005 000170 YOSHIMURA       MASATOSHI    24680.000000000
00005 001 000180 SCOUTTEN        MARILYN      21340.000000000
00005 002 000190 WALKER          JAMES        20450.000000000
00005 003 000200 BROWN           DAVID        27740.000000000
00005 004 000210 JONES           WILLIAM      18270.000000000
00005 005 000220 LUTZ            JENNIFER     29840.000000000
00006 001 000230 JEFFERSON       JAMES        22180.000000000
00006 002 000240 MARINO          SALVATORE    28760.000000000
00006 003 000250 SMITH           DANIEL       19180.000000000
00006 004 000260 JOHNSON         SYBIL        17250.000000000
00006 005 000270 PEREZ           MARIA        27380.000000000
00007 001 000280 SCHNEIDER       ETHEL        26250.000000000
00007 002 000290 PARKER          JOHN         15340.000000000
00007 003 000300 SMITH           PHILIP       17750.000000000
00007 004 000310 SETRIGHT        MAUDE        15900.000000000
00007 005 000320 MEHTA           RAMLAL       19950.000000000
00008 001 000330 LEE             WING         25370.000000000
00008 002 000340 GOUNOT          JASON        23840.000000000
00008 003 200010 HEMMINGER       DIAN         46500.000000000
00008 004 200120 ORLANDO         GREG         29250.000000000
00008 005 200140 NATZ            KIM          28420.000000000
00009 001 200170 YAMAMOTO        KIYOSHI      24680.000000000
00009 002 200220 JOHN            REBA         29840.000000000
00009 003 200240 MONTEVERDE      ROBERT       28760.000000000
00009 004 200280 SCHWARTZ        EILEEN       26250.000000000
00009 005 200310 SPRINGER        MICHELLE     15900.000000000
00010 001 200330 WONG            HELENA       25370.000000000
00010 002 200340 ALONZO          ROY          23840.000000000
*/

-- High level locks taken in different scenarios
-- (lock size page or row)  
alter tablespace use.emp locksize page;
alter tablespace use.emp maxrows 5;
-- REORG!

select * from emp for update with rs
select * from emp for read only with rs

select * from emp for read only with rr
select * from emp for update with rr

select * from emp for fetch only with ur
select * from emp for update with ur

select * from emp for update with cs
select * from emp for read only with cs

select * from emp where empno between '000040' and '001000' for fetch only with rr;
create unique index iemp on emp(empno);
select * from emp where empno between '000040' and '001000' for fetch only with rr;

select * from emp where empno >= '001000' for fetch only with rr;
insert into emp(empno, firstnme, midinit, lastname)
values ('200200', 'Alfred', 'E', 'Neumann');
select digits(decimal(rid(emp) / 256, 5))     page
     , digits(decimal(mod(rid(emp), 256), 3)) row
     , emp.*
  from emp
  with cs


-- Low level locks
------------------

-- U lock vs. S lock
select * from emp where empno = '000090' for read only with rs -- obtain S lock
select * from emp where empno between '000070' and '000100' for update            -- obtain U lock
-- UPDATE WHERE CURRENT OF has to wait

--------------------------------------------------------------------------------
--- END DEMO 1 -----------------------------------------------------------------
--------------------------------------------------------------------------------

-- Deadlock scenario
-- Try with LOCKSIZE ANY/PAGE vs. LOCKSIZE ROW
alter tablespace use.emp locksize page;
select * from emp where empno = '000090';
update proj set prstdate = prstdate where projno = 'AD3110';

select * from proj where projno = 'AD3110';
update emp set salary = salary + 400 where empno = '000110';


-- Lock escalation
alter tablespace use.emp locksize row;  
alter tablespace use.emp lockmax 5;

select hex(rid(emp)) rid, empno, lastname
  from emp with rs;

-- Demonstrate SKIP LOCKED DATA
-- Try with LOCKSIZE ANY/PAGE vs. LOCKSIZE ROW
update emp set salary = salary + 10 where salary < 25000;
select empno, salary from emp for read only;
select empno, salary from emp for read only skip locked data;

-- demonstrate CONCURRENT ACCESS RESOLUTION
insert into emp(empno, firstnme, midinit, lastname)
values ('103252', 'Uli', 'B', 'Seelbach');

delete from emp where empno = '000090';
