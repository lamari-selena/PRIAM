package priam.breaches.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`breach_measure`")
@IdClass(BreachMeasureId.class)
public class BreachMeasure {

    @Id
    @Column(name = "measure_id")
    private int measureId;

    @Id
    @Column(name = "breach_id")
    private int breachId;

    // Getters and setters

    public int getMeasureId() {
        return measureId;
    }

    public void setMeasureId(int measureId) {
        this.measureId = measureId;
    }

    public int getBreachId() {
        return breachId;
    }

    public void setBreachId(int breachId) {
        this.breachId = breachId;
    }
}