package de.hhu.cs.dbs.propra.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Besucher {
    private String email;
    private String geburtsdatum;
    private String telefonnummer;
}
