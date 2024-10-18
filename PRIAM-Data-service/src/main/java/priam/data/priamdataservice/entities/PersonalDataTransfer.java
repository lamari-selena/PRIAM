package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import priam.data.priamdataservice.entities.model.SecondaryActor;

import javax.persistence.*;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@lombok.Data
@Table(name = "personal_data_transfer")
public class PersonalDataTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private int personalDataTransferId;

    @OneToOne
    @JoinColumn(name="processing_id")
    private Processing processing;

    @ManyToMany
    @JoinTable(name = "personal_data_transfer_data",
            joinColumns = @JoinColumn(name = "personal_data_transfer_id"),
            inverseJoinColumns = @JoinColumn(name = "data_id"))
    private List<Data> data;

    /*@ManyToMany
    @JoinTable(name = "personal_data_transfer_secondary_actor",
            joinColumns = @JoinColumn(name = "personal_data_transfer_id"),
            inverseJoinColumns = @JoinColumn(name = "secondary_actor_id"))*/
    @Transient
    private List<SecondaryActor> secondaryActors;
}
