package priam.breaches.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`Breach_Consequence`")
@IdClass(BreachConsequenceId.class)
public class BreachConsequence {

    @Id
    @Column(name = "consequence_id")
    private int consequenceId;

    @Id
    @Column(name = "breach_id")
    private int breachId;

    // Getters and setters

    public int getConsequenceId() {
        return consequenceId;
    }

    public void setConsequenceId(int consequenceId) {
        this.consequenceId = consequenceId;
    }

    public int getBreachId() {
        return breachId;
    }

    public void setBreachId(int breachId) {
        this.breachId = breachId;
    }
}

