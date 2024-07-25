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
    private int age;

    @JsonBackReference(value = "dataSubject_list")
    @ManyToOne
    private DataSubjectCategory dataSubjectCategory;
}
