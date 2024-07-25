package priam.right.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class DataSubjectCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dataSubjectCategoryId;
    @Column
    private String dataSubjectCategoryName;
    @Column
    private String locationId;
}
