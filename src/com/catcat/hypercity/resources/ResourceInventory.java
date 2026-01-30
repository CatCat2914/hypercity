package com.catcat.hypercity.resources;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.exception.MissingResourceTypeException;
import com.catcat.hypercity.loaders.ResourceLoader;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The ResourceInventory class represents the amount and the per second amount (mostly the amount)
 */
public class ResourceInventory implements Json.Serializable{
    private ObjectMap<ResourceDefinition, ResourceData> resources = new ObjectMap<>();

    public ResourceInventory() {
    }

    public float getAmount(ResourceDefinition resource) {
        if(!resources.containsKey(resource)){
            resources.put(resource, new ResourceData());
        }
        return resources.get(resource).amount;
    }

    public void addAmount(ResourceDefinition resource, float delta) {
        if(!resources.containsKey(resource)){
            resources.put(resource, new ResourceData());
        }
        resources.get(resource).amount = Math.max(0, resources.get(resource).amount+delta);
    }

    public float getChangeRate(ResourceDefinition resource) {
        if(!resources.containsKey(resource)){
            resources.put(resource, new ResourceData());
        }
        return (float)resources.get(resource).dxs.stream().mapToDouble(Float::doubleValue).average().orElse(0);
    }

    public void update(float deltaTime) {
        //calculate change
        for (ResourceData data : new ObjectMap.Values<>(resources)) {
            data.dxs.add((data.amount - data.amountLastTick) / deltaTime);
            if (data.dxs.size() > Gdx.graphics.getFramesPerSecond()) data.dxs.removeFirst();
            data.amountLastTick = data.amount;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceInventory{");
        for (ResourceDefinition resource : new ObjectMap.Keys<>(resources)) {
            ResourceData data = resources.get(resource);
            sb.append(resource.key)
                .append(": ")
                .append(Math.round(data.amount * 100f) / 100f) // 2 decimals
                .append(" (Δ/s: ")
                .append(Math.round(getChangeRate(resource) * 100f) / 100f)
                .append("), ");
        }
        if (!resources.isEmpty()) sb.setLength(sb.length() - 2); // remove trailing comma
        sb.append("}");
        return sb.toString();
    }

    public void transferResource(ResourceDefinition resource, ResourceInventory inventory, float amount) {
        inventory.addAmount(resource, amount);
        addAmount(resource, -amount);
    }

    @Override
    public void write(Json json) {
        float EPSILON = 5e-5f;
        json.writeArrayStart("resources");
        for (ObjectMap.Entry<ResourceDefinition, ResourceData> e : new ObjectMap.Entries<>(resources)) {
            if(e.value.amount<EPSILON) continue;
            json.writeObjectStart();
            json.writeValue("key", e.key.key, String.class);
            json.writeValue("amount", e.value.amount, Float.class);
            json.writeObjectEnd();
        }
        json.writeArrayEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        resources = new ObjectMap<>();

        JsonValue array = jsonData.get("resources");
        if (array == null) return;

        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            String key = json.readValue(
                "key", String.class, entry
            );
            ResourceData value = new ResourceData();
            value.amount = json.readValue("amount", Float.class, entry);
            try {
                resources.put(ResourceLoader.getByKey(key), value);
            }
            catch (MissingResourceTypeException e)
            {
                Gdx.app.error("GAME_DATA", e.getMessage()+", skipping resource. amount: "+value.amount);
            }
        }
    }

    //a small little class that stores how much and other misc info
    private static class ResourceData implements Json.Serializable {
        private float amount;
        private transient float amountLastTick; //for delta
        private transient final Deque<Float> dxs = new ArrayDeque<>();

        @Override
        public void write(Json json) {
            json.writeValue("amount", amount);
        }

        @Override
        public void read(Json json, JsonValue jsonData) {
            amount = json.readValue("amount", Float.class, jsonData);
        }
    }
}
