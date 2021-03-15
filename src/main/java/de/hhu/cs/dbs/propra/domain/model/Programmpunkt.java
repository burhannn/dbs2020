package de.hhu.cs.dbs.propra.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Programmpunkt {
    private int P_ID;
    private String Uhrzeit;
    private int dauer;


}
