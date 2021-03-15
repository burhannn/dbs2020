package de.hhu.cs.dbs.propra.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Band {
    private int B_ID;
    private String name;
    private int gruendungsjahr;
}
