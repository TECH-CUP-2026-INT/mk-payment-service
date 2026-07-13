package co.edu.escuelaing.techcup.payment.service;

import java.util.Objects;
import java.util.UUID;

public abstract class AggregateRoot {

    protected UUID id;

    protected AggregateRoot(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AggregateRoot that = (AggregateRoot) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
