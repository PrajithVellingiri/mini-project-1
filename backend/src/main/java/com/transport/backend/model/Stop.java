package com.transport.backend.model;

import java.util.Objects;

/**
 * Represents a transit station, stop, or depot in the network graph.
 */
public class Stop {

    private final String id;
    private final String name;

    public Stop(String id, String name) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Stop ID must not be null or blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stop name must not be null or blank");
        }
        this.id = id.trim();
        this.name = name.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return Objects.equals(id, stop.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Stop{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
