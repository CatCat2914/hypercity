package com.catcat.hypercity.definitions.building;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.catcat.hypercity.loaders.BuildingLoader;

import java.util.Objects;

public class BuildingDefinition implements Json.Serializable {
    public String key;  // unique building ID (like "FACTORY")
    public String name;        // display name
    public String category;    // "Agriculture", "Utility", etc.
    public String texturePath; // texture file
    public String color;       // stored as hex
    public String className;
    public String recipeFile;  // path to recipes JSON
    public BuildingDefinition(){}
    public Color getColorObj() {
        try {
            return Color.valueOf(color);
        } catch (Exception e) {
            return Color.MAGENTA;
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("key", key);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        key = json.readValue("key", String.class, jsonData);
        BuildingDefinition def = BuildingLoader.getByKey(key);
        name = def.name;
        category = def.category;
        texturePath = def.texturePath;
        color = def.color;
        className = def.className;
        recipeFile = def.recipeFile;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BuildingDefinition)) return false;
        BuildingDefinition that = (BuildingDefinition)o;
        return Objects.equals(key, that.key) && Objects.equals(name, that.name) && Objects.equals(category, that.category) && Objects.equals(texturePath, that.texturePath) && Objects.equals(color, that.color) && Objects.equals(className, that.className) && Objects.equals(recipeFile, that.recipeFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, name, category, texturePath, color, className, recipeFile);
    }
}
