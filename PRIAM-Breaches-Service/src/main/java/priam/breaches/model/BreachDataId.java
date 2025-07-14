package priam.breaches.model;

public class BreachDataId implements java.io.Serializable {
    private int dataId;
    private int breachId;

    // Constructors, equals, and hashCode methods
    public BreachDataId() {
    }

    public BreachDataId(int dataId, int breachId) {
        this.dataId = dataId;
        this.breachId = breachId;
    }

    // Override equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreachDataId that = (BreachDataId) o;
        return dataId == that.dataId && breachId == that.breachId;
    }

    @Override
    public int hashCode() {
        return 31 * dataId + breachId;
    }

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
}
