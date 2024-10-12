package priam.right.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Id;
import javax.persistence.ManyToOne;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSubject {
    @Id
    private int dataSubjectId;
    private String idRef;
    private String username;
    private String password;
    private int age;

    @ManyToOne
    private DataSubjectCategory dataSubjectCategory;
}
