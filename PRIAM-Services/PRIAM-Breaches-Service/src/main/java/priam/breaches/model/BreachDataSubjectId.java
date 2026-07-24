package priam.breaches.model;

public class BreachDataSubjectId implements java.io.Serializable {
    private int dataSubjectId;
    private int breachId;

    // Constructors, equals, and hashCode methods
    public BreachDataSubjectId() {}

    public BreachDataSubjectId(int dataSubjectId, int breachId) {
        this.dataSubjectId = dataSubjectId;
        this.breachId = breachId;
    }

    // Override equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreachDataSubjectId that = (BreachDataSubjectId) o;
        return dataSubjectId == that.dataSubjectId && breachId == that.breachId;
    }

    @Override
    public int hashCode() {
        return 31 * dataSubjectId + breachId;
    }

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
