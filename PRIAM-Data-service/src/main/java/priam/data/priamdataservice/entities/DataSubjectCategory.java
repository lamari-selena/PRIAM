package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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

    @OneToMany
    @JoinColumn(name = "data_subject_category_id")
    private Data data;
}
