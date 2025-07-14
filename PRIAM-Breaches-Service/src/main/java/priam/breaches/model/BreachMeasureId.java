package priam.breaches.model;

public class BreachMeasureId implements java.io.Serializable {
    private int measureId;
    private int breachId;

    // Constructors, equals, and hashCode methods
    public BreachMeasureId() {}

    public BreachMeasureId(int measureId, int breachId) {
        this.measureId = measureId;
        this.breachId = breachId;
    }

    // Override equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreachMeasureId that = (BreachMeasureId) o;
        return measureId == that.measureId && breachId == that.breachId;
    }

    @Override
    public int hashCode() {
        return 31 * measureId + breachId;
    }

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
