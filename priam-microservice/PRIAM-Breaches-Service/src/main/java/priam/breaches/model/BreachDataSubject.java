package priam.breaches.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`breach_data_subject`")
@IdClass(BreachDataSubjectId.class)
public class BreachDataSubject {

    @Id
    @Column(name = "data_subject_id")
    private int dataSubjectId;

    @Id
    @Column(name = "breach_id")
    private int breachId;

    // Getters and setters

    public int getDataSubjectId() {
        return dataSubjectId;
    }

    public void setDataSubjectId(int dataSubjectId) {
        this.dataSubjectId = dataSubjectId;
    }

    public int getBreachId() {
        return breachId;
    }

    public void setBreachId(int breachId) {
        this.breachId = breachId;
    }
}

