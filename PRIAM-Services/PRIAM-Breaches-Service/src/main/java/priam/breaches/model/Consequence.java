package priam.breaches.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`consequence`")
public class Consequence {

    @Id
    @Column(name = "consequence_id")
    private int consequenceId;

    @Column(name = "consequence_description")
    private String consequenceDescription;

    // Getters and setters

    public int getConsequenceId() {
        return consequenceId;
    }

    public void setConsequenceId(int consequenceId) {
        this.consequenceId = consequenceId;
    }

    public String getConsequenceDescription() {
        return consequenceDescription;
    }

    public void setConsequenceDescription(String consequenceDescription) {
        this.consequenceDescription = consequenceDescription;
    }
}
