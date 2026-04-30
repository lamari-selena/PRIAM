package priam.breaches.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`breach_data`")
@IdClass(BreachDataId.class)
public class BreachData {

    @Id
    @Column(name = "data_id")
    private int dataId;

    @Id
    @Column(name = "breach_id")
    private int breachId;

    @Column(name = "nb_records")
    private int nbRecords;

    // Getters and setters

    public int getDataId() {
        return dataId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public int getBreachId() {
        return breachId;
    }

    public void setBreachId(int breachId) {
        this.breachId = breachId;
    }

    public int getNbRecords() {
        return nbRecords;
    }

    public void setNbRecords(int nbRecords) {
        this.nbRecords = nbRecords;
    }
}

