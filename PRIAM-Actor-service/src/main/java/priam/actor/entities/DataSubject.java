package priam.actor.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@Table(name = "DataSubject")
public class DataSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dataSubjectId;
    private String idRef;
    private Integer age;

    @JsonBackReference(value = "dataSubject_list")
    @ManyToOne
    @JoinColumn(name="data_subject_category_id")
    private DataSubjectCategory dataSubjectCategory;
}
