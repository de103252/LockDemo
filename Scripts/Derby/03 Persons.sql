-- Links, einmal in READ COMMITTED 
-- und einmal in REPEATABLE READ oder SERIALIZABLE:
select lastname, house
  from person
 where zipcode = 96190
   and lastname = 'Rothe'
   for read ONLY;
 
-- Rechts ausführen und committen:
update person
   set house = '77'
 where zipcode = 96190
   and lastname = 'Rothe'
;

-- Weitere Beispiele, ebenfalls in verschiedenen Isolation Levels testen:

-- Links:
select *
  from person
 where zipcode = 96190
   and year(dob) <= 1950
   FOR READ only
;

-- Rechts:
delete from person 
 where FIRSTNAME = 'Heinz' 
   and LASTNAME = 'Testmann'

insert into person(FIRSTNAME, LASTNAME, DOB, ZIPCODE)
values('Heinz', 'Testmann', '1950-01-01', 96190);