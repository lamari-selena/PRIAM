package priam.data.priamdataservice.entities.consent;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@IdClass(ConsentKey.class)
@Table(name="consent")
public class Consent {
    @Id
    private int dataSubjectId;
    @Id
    private int processingId;
}
