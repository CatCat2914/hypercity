package com.catcat.hypercity.loaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.catcat.hypercity.building.behavior.BuildingBehaviorFactory;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.recipe.Recipe;
import com.catcat.hypercity.building.behavior.*;
import com.catcat.hypercity.building.behavior.recipe.*;

import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.exception.MissingBuildingTypeException;

public class BuildingLoader {
    private static final ObjectMap<String, BuildingDefinition> DEFINITIONS = new ObjectMap<>();
    private static BuildingBehaviorFactory behaviorFactory;

    //uninstantiable utility class
    private BuildingLoader() {
        throw new UnsupportedOperationException();
    }
    public static void load(FileHandle[] packs) {
        for (FileHandle pack : packs) {
            FileHandle buildingsFile = Gdx.files.internal(pack.path() + "/buildings.json");
            if (!buildingsFile.exists()) {
                throw new IllegalArgumentException("pack '" + pack.name() + "' does not contain buildings.json");
            }

            JsonValue root = new JsonReader().parse(buildingsFile);
            // root is an array, iterate over it
            for (JsonValue value = root.child; value != null; value = value.next) {
                BuildingDefinition def = new BuildingDefinition();
                def.key = pack.name() + "." + value.getString("key");
                def.name = value.getString("name", def.key);
                def.category = value.getString("category", "Misc"); // default if missing
                def.texturePath = value.getString("texturePath", "placeholder.png");
                def.color = value.getString("color", "FFFFFF"); // default white
                def.recipeFile = value.getString("recipeFile", null);
                def.className = value.getString("className", null);

                if (!def.texturePath.equals("placeholder.png")) {
                    def.texturePath = pack.path() + "/" + def.texturePath;
                }
                if (def.recipeFile != null) {
                    def.recipeFile = pack.path() + "/" + def.recipeFile;
                }

                DEFINITIONS.put(def.key, def);
            }
        }
    }

    public static void setBehaviorFactory(BuildingBehaviorFactory factory) {//for avoiding GWT Reflection (libgdx has a way to allow reflection)
        behaviorFactory = factory;
    }

    public static BuildingBehavior createBuilding(BuildingDefinition def) {
        if (def.className != null) {//add class building. The other 2 are all okay for any platform but this mf isn't
            if (behaviorFactory == null)
                throw new IllegalStateException("Behavior factory not set");
            return behaviorFactory.create(def);
        } else if (def.recipeFile != null) { //for simple buildings, use a recipe file
            return new SimpleRecipeBuildingBehavior(def);
        } else { //for decorations (which means useless)
            return new DecorativeBuildingBehavior(def);
        }
    }

    public static ObjectMap.Values<BuildingDefinition> getAll() {
        return new ObjectMap.Values<>(DEFINITIONS);
    }

    public static BuildingDefinition getByKey(String key) {
        BuildingDefinition def = DEFINITIONS.get(key);
        if (def == null) throw new MissingBuildingTypeException("No building with key: " + key);
        return def;
    }

    public static class SimpleRecipeBuildingBehavior extends RecipeBuildingBehavior {
        SimpleRecipeBuildingBehavior(BuildingDefinition definition) {
            super(definition);
        }
        @SuppressWarnings("unused")
        SimpleRecipeBuildingBehavior(){
            super();
        }

        @Override
        protected Array<Recipe> defineRecipes() {
            FileHandle file = Gdx.files.internal(super.getDefinition().recipeFile);
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file); // root is an array

            Array<Recipe> recipes = new Array<>();

            for (JsonValue value = root.child; value != null; value = value.next) {
                Recipe r = new Recipe();
                r.read(new Json(), value); // use your Serializable implementation
                recipes.add(r);
            }
            return recipes;
        }
    }

    public static class DecorativeBuildingBehavior extends BuildingBehavior {
        DecorativeBuildingBehavior(BuildingDefinition definition) {
            super();
            setDefinition(definition);
        }
        @SuppressWarnings("unused")
        DecorativeBuildingBehavior(){}
    }
}
