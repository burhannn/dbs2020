package de.hhu.cs.dbs.propra.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Ticket {

    private int T_ID;
    private String datum;
    private Double preis;
    private boolean VIP;
}
