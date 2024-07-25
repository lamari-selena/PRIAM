package priam.actor.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Representative")
public class Representative {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int representativeId;
    private String representativeName;
    @ManyToOne
    private Address representativeAddress;
    private String representativePhone;
    private String representativeEmail;
    @ManyToOne
    private Country country;
}
