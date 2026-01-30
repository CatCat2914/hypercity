package com.catcat.hypercity.definitions.recipe;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Objects;

/**
 * 1 recipe has the inputs and the outputs for a selected recipe.
 * In a factory for example, 1 log -> 3 wood. This would be a recipe and each recipe is going to be included in an Array in the behavior and can be selected
 */
public class Recipe implements Json.Serializable {
    private String key;
    private String name;
    private float powerConsumption; //or production if negative
    private int maxWorkers;
    private Array<RecipeEntry> inputs;
    private Array<RecipeEntry> outputs;

    public Recipe() {
        inputs = new Array<>();
        outputs = new Array<>();
    }

    public Recipe(String key, String name, float powerConsumption, int maxWorkers, Array<RecipeEntry> inputs, Array<RecipeEntry> outputs) {
        this.key = key;
        this.name = name;
        this.powerConsumption = powerConsumption;
        this.maxWorkers = maxWorkers;

        if (inputs == null || outputs == null) {
            this.inputs = new Array<>();
            this.outputs = new Array<>();
        } else {
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe)o;
        return Objects.equals(key, recipe.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "Recipe{" +
            "name='" + name + '\'' +
            ", powerConsumption=" + powerConsumption +
            ", maxWorkers=" + maxWorkers +
            ", inputs=" + inputs +
            ", outputs=" + outputs +
            '}';
    }

    public int getWorkers() {
        return maxWorkers;
    }

    public float getPowerConsumption() {
        return powerConsumption;
    }

    public String getName() {
        return name;
    }

    public Array<RecipeEntry> getInputs() {
        return new Array<>(inputs);
    }

    public Array<RecipeEntry> getOutputs() {
        return new Array<>(outputs);
    }

    /**
     * recipes aren't written I don't think
     */
    @Override
    public void write(Json json) {
        json.writeValue("key", key);
        json.writeValue("name", name);
        json.writeValue("powerConsumption", powerConsumption);
        json.writeValue("maxWorkers", maxWorkers);
        json.writeValue("inputs", inputs, Array.class, RecipeEntry.class);
        json.writeValue("outputs", outputs, Array.class, RecipeEntry.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {
        key = json.readValue("key", String.class, jsonData);
        name = json.readValue("name", String.class, jsonData);
        powerConsumption = json.readValue("powerConsumption", Float.class, jsonData);
        maxWorkers = json.readValue("maxWorkers", Integer.class, jsonData);

        // Parse inputs
        inputs = new Array<>();
        JsonValue inputsJson = jsonData.get("inputs");
        if (inputsJson != null) {
            for (JsonValue val = inputsJson.child; val != null; val = val.next) {
                RecipeEntry entry = new RecipeEntry();
                entry.resource = val.getString("resource", null);
                entry.tag = val.getString("tag", null);
                entry.attribute = val.getString("attribute", null);
                entry.value = val.getFloat("value", 0f);
                inputs.add(entry);
            }
        }

        // Parse outputs
        outputs = new Array<>();
        JsonValue outputsJson = jsonData.get("outputs");
        if (outputsJson != null) {
            for (JsonValue val = outputsJson.child; val != null; val = val.next) {
                RecipeEntry entry = new RecipeEntry();
                entry.resource = val.getString("resource", null);
                entry.tag = val.getString("tag", null);
                entry.attribute = val.getString("attribute", null);
                entry.value = val.getFloat("value", 0f);
                outputs.add(entry);
            }
        }
    }

    public String getKey() {
        return key;
    }
}

