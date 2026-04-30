package priam.breaches.model;

public class BreachConsequenceId implements java.io.Serializable {
    private int consequenceId;
    private int breachId;

    // Constructors, equals, and hashCode methods
    public BreachConsequenceId() {
    }

    public BreachConsequenceId(int consequenceId, int breachId) {
        this.consequenceId = consequenceId;
        this.breachId = breachId;
    }

    // Override equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreachConsequenceId that = (BreachConsequenceId) o;
        return consequenceId == that.consequenceId && breachId == that.breachId;
    }

    @Override
    public int hashCode() {
        return 31 * consequenceId + breachId;
    }

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
