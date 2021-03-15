PRAGMA auto_vacuum = 1;
PRAGMA encoding = "UTF-8";
PRAGMA foreign_keys = 1;
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA case_sensitive_like = true;



CREATE TABLE IF NOT EXISTS Ort(
  Name VARCHAR(30) PRIMARY KEY NOT NULL COLLATE NOCASE CHECK(Name NOT GLOB '*[^a-zA-Z]*'  AND Name NOT GLOB '*[^ -~]*'  AND LENGTH(Name) > 0),
  Land VARCHAR(30) NOT NULL CHECK(Land NOT GLOB '*[^a-zA-Z]*'  AND LENGTH(Land) > 0)
);


CREATE TABLE IF NOT EXISTS Festival(
  F_ID INT PRIMARY KEY NOT NULL CHECK(F_ID >= 0),
  Name VARCHAR(30) NOT NULL CHECK(Name NOT GLOB '*[^ -~]*' AND Name NOT GLOB '*[^a-zA-Z]*'  AND LENGTH(Name) > 0),
  Bild BLOB NOT NULL CHECK(Hex(Bild) LIKE '89504E470D0A1A0A%'),
  Datum DATE NOT NULL CHECK(Datum is date(Datum)),
  Ortname VARCHAR(30) NOT NULL,
  FOREIGN KEY(Ortname) REFERENCES Ort(Name) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Ticket(
  Ticket_ID INTEGER PRIMARY KEY NOT NULL CHECK(Ticket_ID >= 0),
  Datum DATE NOT NULL DEFAULT (DATE('now')) CHECK(Datum IS DATE(Datum)),
  Preis FLOAT NOT NULL CHECK(Preis >= 0.00 AND (Preis GLOB '*.[0-9][0-9]' OR Preis GLOB '*.[0-9]' OR Preis GLOB '[0-9]')),
  VIP_Vermerk BOOLEAN NOT NULL,
  Besucher_Email VARCHAR(50) NOT NULL,
  Festival_ID INTEGER NOT NULL,
  FOREIGN KEY(Besucher_Email) REFERENCES Besucher(UserEmail) ON UPDATE CASCADE ON DELETE CASCADE
  FOREIGN KEY(Festival_ID) REFERENCES Festival(F_ID)
);

CREATE TABLE IF NOT EXISTS Buehne(
  Name VARCHAR(30) PRIMARY KEY NOT NULL COLLATE NOCASE CHECK(Name NOT GLOB '*[^a-zA-Z]*' AND Name NOT GLOB '*[^ -~]*' AND LENGTH(Name) > 0),
  Sitzplatzanzahl INT NOT NULL CHECK (Sitzplatzanzahl >= 0),
  Stehplatzanzahl INT NOT NULL CHECK (Stehplatzanzahl >= 0),
  F_ID INT NOT NULL,
  FOREIGN KEY(F_ID) REFERENCES Festival(F_ID)
);

CREATE TABLE IF NOT EXISTS Programmpunkt(
  P_ID INT PRIMARY KEY NOT NULL CHECK(P_ID >= 0),
  Uhrzeit TIME NOT NULL CHECK(Uhrzeit is TIME(Uhrzeit)),
  Dauer INT NOT NULL CHECK(Dauer IN (15,30,45,60,75,90,120)),
  Buehne_name VARCHAR(30) NOT NULL COLLATE NOCASE CHECK(Buehne_name NOT GLOB '*[^ -~]*' AND LENGTH(Buehne_name) > 0),
  B_ID INT NOT NULL,
  FOREIGN KEY(Buehne_name) REFERENCES Buehne(Name) ON DELETE CASCADE ON UPDATE CASCADE
  FOREIGN KEY(B_ID) REFERENCES Band(B_ID)
);

CREATE TABLE IF NOT EXISTS Band(
  B_ID INT PRIMARY KEY NOT NULL CHECK(B_ID >= 0),
  Name VARCHAR(30) NOT NULL COLLATE NOCASE CHECK(Name NOT GLOB '*[^ -~]*' AND LENGTH(Name)>0),
  Gruendungsjahr INT NOT NULL CHECK(Gruendungsjahr BETWEEN 1001 AND 3003)
);

CREATE TABLE IF NOT EXISTS Genre(
  Name VARCHAR(30) PRIMARY KEY NOT NULL COLLATE NOCASE CHECK(Name NOT GLOB '*[^a-zA-Z]*' AND Name NOT GLOB '*[^ -~]*' AND LENGTH(Name) > 0)
);

CREATE TABLE IF NOT EXISTS Kuenstler(
  UserEmail VARCHAR(50) PRIMARY KEY NOT NULL,
  Kuenstlername VARCHAR(30) CHECK(Kuenstlername NOT GLOB '*[^ -~]*'),
  FOREIGN KEY(UserEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Besucher(
  UserEmail VARCHAR(50) PRIMARY KEY NOT NULL,
  Geburtsdatum DATE NOT NULL,
  Telefonnummer VARCHAR COLLATE NOCASE CHECK(Telefonnummer GLOB '+49[0-9]*' AND Telefonnummer NOT GLOB '*[^0-9]' AND LENGTH(Telefonnummer) > 4),
  FOREIGN KEY(UserEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Veranstalter(
  UserEmail VARCHAR(50) PRIMARY KEY NOT NULL,
  Name VARCHAR(30) NOT NULL COLLATE NOCASE CHECK(Name NOT GLOB '*[^a-zA-Z]*'  AND Name NOT GLOB '*[^ -~]*' AND LENGTH(Name) > 0),
  FOREIGN KEY(UserEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Veranstalter_organisiert_Fest(
  VeranstalterEmail VARCHAR(50) NOT NULL,
  F_ID INT NOT NULL,
  PRIMARY KEY(F_ID, VeranstalterEmail),
  FOREIGN KEY(VeranstalterEmail) REFERENCES User(Email) ON DELETE CASCADE ON UPDATE CASCADE
  FOREIGN KEY(F_ID) REFERENCES Festival(F_ID)
);

CREATE TABLE IF NOT EXISTS Band_hat_Genre(
  B_ID INT NOT NULL,
  GenreName VARCHAR(30) NOT NULL,
  PRIMARY KEY(B_ID, GenreName),
  FOREIGN KEY(B_ID) REFERENCES Band(B_ID)
  FOREIGN KEY(GenreName) REFERENCES Genre(Name) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Band_gehoert_Kuenstler(
  B_ID INT NOT NULL,
  KuenstlerEmail VARCHAR(50) NOT NULL,
  PRIMARY KEY(B_ID, KuenstlerEmail),
  FOREIGN KEY(B_ID) REFERENCES Band(B_ID)
  FOREIGN KEY(KuenstlerEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Veranstalter_kooperiert_Veranstalter(
  ErsterVeranstalterEmail VARCHAR(50) NOT NULL CHECK(ErsterVeranstalterEmail != ZweiterVeranstalterEmail),
  ZweiterVeranstalterEmail VARCHAR(50) NOT NULL CHECK(ErsterVeranstalterEmail != ZweiterVeranstalterEmail),
  PRIMARY KEY(ErsterVeranstalterEmail, ZweiterVeranstalterEmail),
  FOREIGN KEY(ErsterVeranstalterEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
  FOREIGN KEY(ZweiterVeranstalterEmail) REFERENCES User(Email) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS User(
  Email VARCHAR(50) PRIMARY KEY NOT NULL COLLATE NOCASE CHECK(Email GLOB '*?@*?.?*' AND LENGTH(Email) > 0 AND
  SUBSTR(Email, 1, INSTR(Email, '@') - 1) NOT GLOB '*[^a-zA-Z]*' AND SUBSTR(Email, INSTR(Email, '@') + 1,
  INSTR(Email, '.') - INSTR(Email, '@') - 1) NOT GLOB '*[^a-zA-Z]*'AND SUBSTR(Email, INSTR(Email, '.') + 1,
  LENGTH(Email) - INSTR(Email, '.')) NOT GLOB '*[^a-zA-Z]*' AND Email LIKE '%@%.%'
  AND LENGTH(Email) - LENGTH(REPLACE(Email, '@', '')) = 1 AND LENGTH(Email) - LENGTH(REPLACE(Email, '.', '')) = 1),
  Passwort VARCHAR(30) NOT NULL COLLATE NOCASE CHECK( Passwort GLOB '*[A-Z]*' AND Passwort GLOB '*[0-9]*[0-9]*' AND Passwort NOT GLOB '*[^ -~]*' AND LENGTH(Passwort) > 5),
  Vorname VARCHAR(30) NOT NULL COLLATE NOCASE CHECK( Vorname GLOB '*[A-Za-z]*' AND Vorname NOT GLOB '*[^ -~]*' AND LENGTH(Vorname) > 0),
  Nachname VARCHAR(30) NOT NULL COLLATE NOCASE CHECK( Nachname GLOB '*[A-Za-z]*'AND Nachname NOT GLOB '*[^ -~]*' AND LENGTH(Nachname) > 0)
);

--Beim Kaufen eines Tickets wird das aktuelle Datum gesetzt. EXAKT wie in Aufgabe verlangt!
/*CREATE TRIGGER IF NOT EXISTS insertDate AFTER INSERT ON Ticket
BEGIN
UPDATE Ticket SET Datum = date('now')
WHERE Ticket_ID = new.Ticket_ID;
END;
*/
--Ein Besucher kann höchstens ein Ticket für ein spezifisches Festival besitzen.
CREATE TRIGGER IF NOT EXISTS besucher_besitzt_ein_Ticket_pro_Fest BEFORE INSERT ON Ticket
BEGIN
	SELECT
	CASE
	 WHEN
			(
					SELECT Besucher_Email
					FROM Ticket t
          WHERE t.Besucher_Email = new.Besucher_Email
          AND t.Datum = new.Datum
          AND t.Festival_ID = new.Festival_ID
			) >1
		THEN
			RAISE (ABORT, 'Ein Besucher kann höchstens ein Ticket für ein spezifisches Festival besitzen.')
	END;
END;
