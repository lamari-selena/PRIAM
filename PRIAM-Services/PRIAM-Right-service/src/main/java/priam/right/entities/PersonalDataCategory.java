package priam.right.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class PersonalDataCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int personalDataCategoryId;
    @Column
    private String personalDataCategoryName;
}
