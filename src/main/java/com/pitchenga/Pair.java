package com.pitchenga;

import java.util.Objects;

class Pair<L, R> {
    public final L left;
    public final R right;

    Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "{" + left + "," + right + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(left, pair.left) &&
                Objects.equals(right, pair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
