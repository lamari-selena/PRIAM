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
    private Integer age;

    // Actor-service's DataSubjectResponseDTO (the real payload
    // ActorRestClient.getDataSubject() deserializes) returns these two as
    // flat fields, not a nested object - the @ManyToOne field below never
    // actually got populated by Jackson (no caller ever read it either) and
    // forced every category lookup through a second, buggy Feign call
    // instead (see DataRequestServiceImpl).
    private int dataSubjectCategoryId;
    private String dataSubjectCategoryName;

    @ManyToOne
    private DataSubjectCategory dataSubjectCategory;
}
