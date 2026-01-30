package com.catcat.hypercity.definitions.resource;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.catcat.hypercity.loaders.ResourceLoader;

import java.util.Objects;

public class ResourceDefinition implements Json.Serializable {
    public String key;         // unique string ID (like "base.WOOD")
    public transient String name;        // display name
    public transient String category;    // e.g. "Ores", "Food"
    public transient String texturePath; // e.g. "textures/materials/wood.png"
    public transient ObjectSet<String> tags = new ObjectSet<>();
    public transient ObjectMap<String, Float> attributes = new ObjectMap<>();

    public ResourceDefinition() {
    }

    public ResourceDefinition(String key, String name, String category, String texturePath) {
        this.key = key;
        this.name = name;
        this.category = category;
        this.texturePath = texturePath;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    public float getAttribute(String attribute) {
        return attributes.get(attribute, 0f);
    }

    @Override
    public String toString() {
        return "ResourceDefinition{" +
            "key='" + key + '\'' +
            ", name='" + name + '\'' +
            ", category='" + category + '\'' +
            ", texturePath='" + texturePath + '\'' +
            '}';
    }

    @Override
    public void write(Json json) {
        json.writeValue("key", key);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        key = json.readValue("key", String.class, jsonData);
        ResourceDefinition def = ResourceLoader.getByKey(key);
        this.name = def.name;
        this.category = def.category;
        this.texturePath = def.texturePath;
        this.attributes = new ObjectMap<>(def.attributes);
        this.tags = new ObjectSet<>(def.tags);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResourceDefinition)) return false;
        ResourceDefinition that = (ResourceDefinition)o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
