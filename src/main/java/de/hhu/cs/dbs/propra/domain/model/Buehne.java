package de.hhu.cs.dbs.propra.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Buehne {
    private String name;
    private int sitzplatzanzahl;
    private int stehplatzanzahl;

}
