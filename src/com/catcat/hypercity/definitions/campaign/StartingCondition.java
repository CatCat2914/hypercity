package com.catcat.hypercity.definitions.campaign;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.loaders.ResourceLoader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StartingCondition implements Json.Serializable {
    private float startingElectricity;
    private ObjectMap<ResourceDefinition, Float> startingResources = new ObjectMap<>();

    /**
     * Creates a new starting condition with 250 electricity
     */
    public StartingCondition() {
        this(250f);
    }

    /**
     * Creates a new starting condition with the specified electricity
     *
     * @param startingElectricity The amount of electricity to start with
     */
    public StartingCondition(float startingElectricity) {
        this.startingElectricity = Math.max(0f, startingElectricity); //so that no softlock
    }

    //method chaining ftw
    @SuppressWarnings("unused")
    public StartingCondition setResource(ResourceDefinition resource, float amount) {
        startingResources.put(resource, amount);
        return this;
    }

    public float getStartingElectricity() {
        return startingElectricity;
    }

    public Map<ResourceDefinition, Float> getStartingResources() {
        Map<ResourceDefinition, Float> map = new HashMap<>();
        //noinspection GDXJavaUnsafeIterator
        for (ObjectMap.Entry<ResourceDefinition, Float> entry : startingResources.entries()) {
            map.put(ResourceLoader.getByKey(entry.key.key), entry.value);
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public void write(Json json) {
        json.writeValue("startingElectricity", startingElectricity);
        json.writeValue("startingResources", startingResources, ObjectMap.class, Float.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        startingElectricity = json.readValue("startingElectricity", Float.class, jsonData);
        //noinspection unchecked
        startingResources = json.readValue("startingResources", ObjectMap.class, jsonData);
    }
}
