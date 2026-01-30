package com.catcat.hypercity.definitions.recipe;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.loaders.ResourceLoader;

public class RecipeEntry implements Json.Serializable {
    // TODO: 1/28/26 this will represent an item or a tag or a tag + attribute combo. It will replace the string in the maps of the recipe.
    public String resource;
    public String tag;
    public String attribute;
    public float value;

    @Override
    public void write(Json json) {
        json.writeValue("resource", resource);
        json.writeValue("tag", tag);
        json.writeValue("attribute", attribute);
        json.writeValue("value", value);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        resource = json.readValue("resource", String.class, jsonData);
        tag = json.readValue("tag", String.class, jsonData);
        attribute = json.readValue("attribute", String.class, jsonData);
        value = json.readValue("value", Float.class, jsonData);

        boolean hasResource = resource != null;
        boolean hasTag = tag != null;
        boolean hasAttribute = attribute != null;

        // invalid if resource coexists with anything else, or tag missing while attribute exists
        if (hasResource && (hasTag || hasAttribute) || (hasAttribute && !hasTag)) {
            throw new IllegalArgumentException(
                "RecipeEntry must define exactly one of: resource OR tag OR tag+attribute"
            );
        }
    }

    @Override
    public String toString() {
        return "RecipeEntry{" +
            "resource='" + resource + '\'' +
            ", tag='" + tag + '\'' +
            ", attribute='" + attribute + '\'' +
            ", value=" + value +
            '}';
    }

    public Array<ResourceDefinition> getMatchingResources() {
        Array<ResourceDefinition> matches = new Array<>();
        if (resource != null) {
            matches.add(ResourceLoader.getByKey(resource));
        } else if (tag != null) {
            matches.addAll(ResourceLoader.getResourcesWithTag(tag));
            if (matches.size == 0) {
                throw new RuntimeException("No resources found for tag: " + tag);
            }
        } else {
            throw new IllegalStateException("RecipeEntry has neither resource nor tag");
        }
        return matches;
    }
    /**
     * Returns how much this resource contributes toward satisfying
     * one unit of this RecipeEntry.
     * - If attribute == null: each unit contributes 1
     * - If attribute != null: contribution = resource.attribute
     */
    public float getPerUnitValue(ResourceDefinition res) {
        if (attribute == null) {
            // unit-based recipe
            return 1f;
        }
        // value-based recipe
        return res.getAttribute(attribute);
    }
    public ResourceDefinition getBestResource(Building building) {
        Array<ResourceDefinition> matches = getMatchingResources();

        ResourceDefinition best = null;
        float bestScore = -Float.MAX_VALUE;

        for (ResourceDefinition res : new Array.ArrayIterable<>(matches)) {

            float score = building.localInventory.getAmount(res)*getPerUnitValue(res);

            if (score > bestScore) {
                bestScore = score;
                best = res;
            }
        }
        if (best == null) {
            throw new RuntimeException("No resource found for: "+this);
        }
        return best;
    }
}
