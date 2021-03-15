insert into user values('burhan@burhan.de','Burhan23', 'Burhan', 'Buu'),
                       ('timboo@timboo.de','Timmer23', 'Tim', 'Tee'),
                       ('taneer@taneer.de','Taneer23', 'Taner', 'Tan'),
                       ('dardan@dardan.de','Dardaa21','Dardan','Dardan'),
                       ('mero@mero.de','EnesMe20','Enes','Meral'),
                       ('kastro@kastro.de','Kasstr22','Kass','Trooo'),
                       ('john@lennon.de','Lenno22','John','Lennon'),
                       ('paul@mccartney.de','Pauli11','Paul','Mccartney'),
                       ('veranstalterEins@taneer.de','Tanne11','Taner','Tasci'),
                       ('veranstalterZwei@felix.de','Tanne22','Felix','Sturfix');
insert into ort values('Hamburg','Deutschland'),
                      ('Berlin','Deutschland'),
                      ('Muenchen','Deutschland');
insert into festival values(1,'whatsHam',readfile('nazar.PNG'),'2019-09-25','Hamburg'),
                           (2,'whatsBer',readfile('nazar.PNG'),'2019-03-22','Berlin'),
                           (3,'whatsMun',readfile('nazar.PNG'),'2019-01-29','Muenchen'),
                           (4,'whatsMun',readfile('nazar.PNG'),'2020-02-20','Muenchen'),
                           (5,'whatsMun',readfile('nazar.PNG'),'2020-02-20','Muenchen'),
                           (6,'whatsMun',readfile('nazar.PNG'),'2020-02-04','Muenchen');
insert into besucher values('burhan@burhan.de', '2002-12-12', '+4923462344322'),
                           ('timboo@timboo.de', '2000-12-12', '+4923462932322'),
                           ('taneer@taneer.de', '2000-11-12', '+4923462345622');
insert into kuenstler values('dardan@dardan.de','Dardan'),
                            ('mero@mero.de','Mero'),
                            ('kastro@kastro.de','Kastro'),
                            ('john@lennon.de','Johnlennon'),
                            ('paul@mccartney.de','Pauli');
insert into ticket values(1,'2020-01-01',1,true,'burhan@burhan.de',1),
                         (2,'2020-01-01',55.50,false,'timboo@timboo.de',2),
                         (3,'2020-01-01',33.33,true,'taneer@taneer.de',3),
                         (4,'2000-01-11',111.11,true,'burhan@burhan.de',2),
                         (5,'2001-01-11',122.11,true,'burhan@burhan.de',3),
                         (6,'2015-05-21',79.75,true,'timboo@timboo.de',3);
insert into buehne values('Staatsoper',1675,50,1),
                         ('Riesensaal',2275,52,1),
                         ('Apollosaal',234,111,2),
                         ('Schauspielhaus',690,31,3),
                         ('Schwachmatenhaus',666,33,4),
                         ('Lausighaus',623,23,4),
                         ('Cukursahne',343,34,5),
                         ('Japsonstudio',133,7,6);
insert into band values(1,'YuyuYaraks',2001),
                       (2,'Annilhilator',2000),
                       (3,'TheGreatestGoet',2011),
                       (4,'TheNewBeatles',1966),
                       (5,'SmallYaraks',2014),
                       (6,'BigGoets',1969);

--P_ID, Uhrzeit, Dauer, Buehne.Name, Band.B_ID
insert into programmpunkt values(1,'15:30:01',30,'Staatsoper',2),
                                (2,'16:30:30',120,'Schauspielhaus',3),
                                (3,'17:30:30',120,'Apollosaal',4),
                                (4,'18:30:00',120,'Schauspielhaus',4),
                                (5,'14:00:00',120,'Staatsoper',4);

insert into genre values('Rock'),('Pop'),('HipHop'),('Techno'),('Classic');
insert into band_hat_Genre values(1,'Pop'),(2,'Rock'),(3,'HipHop'),(4,'Rock');
insert into veranstalter values('veranstalterEins@taneer.de','Taneer'),
                               ('veranstalterZwei@felix.de','Felix');
insert into veranstalter_organisiert_Fest values('veranstalterEins@taneer.de',1),
                                                ('veranstalterZwei@felix.de',2),
                                                ('veranstalterZwei@felix.de',3);
insert into band_gehoert_Kuenstler values(1,'mero@mero.de'),
                                         (2,'dardan@dardan.de'),
                                         (4,'john@lennon.de'),
                                         (4,'paul@mccartney.de');
insert into veranstalter_kooperiert_Veranstalter values('veranstalterEins@taneer.de','veranstalterZwei@felix.de');
