package com.catcat.hypercity.loaders;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.building.behavior.recipe.RecipeBuildingBehavior;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.recipe.Recipe;

import java.util.Objects;

public class RecipeRegistry {
    private static final ObjectMap<String,RecipeData> recipes = new ObjectMap<>();
    public static void register()
    {
        for (BuildingDefinition definition:BuildingLoader.getAll()) {
            BuildingBehavior behavior = BuildingLoader.createBuilding(definition);
            if(behavior instanceof RecipeBuildingBehavior){
                ((RecipeBuildingBehavior)behavior).getRecipes().forEach(recipe -> recipes.put(definition.key+"."+ recipe.getKey(),new RecipeData(recipe,behavior.getDefinition())));
            }
        }
    }
    public static RecipeData get(String key){
        return recipes.get(key);
    }
    public static Array<RecipeData> getAll(){
        return new ObjectMap.Values<>(recipes).toArray();
    }
    public static class RecipeData {
        public final Recipe recipe;
        public final BuildingDefinition building;

        public RecipeData(Recipe recipe, BuildingDefinition building) {
            this.recipe = recipe;
            this.building = building;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RecipeData)) return false;
            RecipeData that = (RecipeData)o;
            return Objects.equals(recipe, that.recipe) && Objects.equals(building, that.building);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recipe, building);
        }
    }
}
