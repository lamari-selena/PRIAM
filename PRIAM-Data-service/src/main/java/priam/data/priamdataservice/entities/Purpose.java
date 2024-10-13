package priam.data.priamdataservice.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;
import priam.data.priamdataservice.enums.PurposeType;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purpose")
public class Purpose {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int purposeId;
    @Column(nullable = false)
    private String purposeDescription;
    @Enumerated(EnumType.STRING)
    private PurposeType purposeType;
/*	@JsonIgnore
    @JoinColumn(name = "processing_id", nullable = false)
	@ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	private Processing processing;*/

    @ManyToOne
    @JoinColumn(name = "processing_id", nullable = false)  // Ceci crée la clé étrangère
    private Processing processing;
}
