package uk.org.stnickschurch.stnicksapp.data;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ListRefresh {
    public final int added;
    public final int updated;
    public final int removed;

    public ListRefresh(int added, int updated, int removed) {
        this.added = added;
        this.updated = updated;
        this.removed = removed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(added, updated, removed);
    }

    @Override
    public boolean equals(Object that_) {
        if (that_ == null || this.getClass() != that_.getClass()) {
            return false;
        }
        ListRefresh that = (ListRefresh) that_;
        return this.added == that.added
                && this.updated == that.updated
                && this.removed == that.removed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("added", added)
                .add("updated", updated)
                .add("removed", removed)
                .toString();
    }
}
