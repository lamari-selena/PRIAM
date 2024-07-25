package priam.data.priamdataservice.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import priam.data.priamdataservice.enums.Source;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@ToString
@Table(name = "data")
public class Data {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dataId;
    @Column
    private String dataName;
    @Column
    private Source source;
    @Column
    private String sourceDetails;
    @Column
    private int dataConservationDuration;
    @Column
    private boolean isPersonal;
    @Column
    private boolean isPortable;
    @Column
    private boolean isPrimaryKey;

    @ToString.Exclude
    @JsonBackReference(value = "data_list")
    @ManyToOne
    @JoinColumn(name = "data_type_id")
    private DataType dataType;

    @Transient
    private DataSubjectCategory dataSubjectCategory;
    @Column
    private int dataSubjectCategoryId;

    @JsonBackReference(value = "personal_data_category")
    @ManyToOne
    @JoinColumn(name = "personal_data_category_id")
    private PersonalDataCategory personalDataCategory;
}
