package de.hhu.cs.dbs.propra.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Festival {

    private int F_ID;
    private String name;
    private byte[] bild;
    private String datum;

}
