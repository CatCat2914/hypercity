package com.catcat.hypercity.transport;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;

import java.util.Objects;

public class Road implements Json.Serializable {
    private transient Building from;
    private transient Building to;
    private int fromID;
    private int toID;
    @SuppressWarnings("unused")
    public Road(){}
    public Road(Building from, Building to) {
        this.from = from;
        this.fromID = from.getId();
        this.to = to;
        this.toID = to.getId();
    }

    /**
     * @return 2 * (the minimum of the producer speed and the consumer speed). Multiplying by 2 clears out any excesses
     */
    public float getOverallSpeed(ResourceDefinition resource) {
        return 2.00f * Math.min(from.behavior.getResourceOutputs().get(resource, 0f), to.behavior.getResourceInputs().get(resource, 0f));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Road road = (Road)obj;
        return from == road.from && to == road.to;
    }

    @Override
    public String toString() {
        return "Road{" +
            "from=" + from +
            ", to=" + to +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @SuppressWarnings("unused")
    public Building getSourceBuilding() {
        return from;
    }

    public Building getTargetBuilding() {
        return to;
    }

    public int getToID() {
        return toID;
    }
    public int getFromID(){
        return fromID;
    }

    @Override
    public void write(Json json) {
        json.writeValue("toID", toID);
        json.writeValue("fromID", fromID);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {//the building it's coming from will let it know what the to building is by getting it from the city
        toID = json.readValue("toID", Integer.class, jsonData);
        fromID = json.readValue("fromID", Integer.class, jsonData);
    }

    /**
     * rebuild methods are called after serialization is done.
     */
    public void rebuild(City city) {
        if (to == null) {
            to = city.getBuilding(toID);
        }
        if (from == null) {
            from = city.getBuilding(fromID);
        }
    }

    public boolean isInvalid() {
        return from==null||to==null;
    }
}


