package com.catcat.hypercity.building.behavior.recipe;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.recipe.Recipe;

import java.util.HashMap;

public class WindTurbineBuildingBehavior extends RecipeBuildingBehavior {
    private transient Building building;

    @SuppressWarnings("unused")

    public WindTurbineBuildingBehavior(){}
    public WindTurbineBuildingBehavior(BuildingDefinition definition) {
        super(definition);
    }

    @Override
    protected Array<Recipe> defineRecipes() {
        Array<Recipe> recipes = new Array<>();

        Recipe wind = new Recipe(
            "WIND",
            "Use Wind",
            -1f,
            0,
            new Array<>(),
            new Array<>()
        );

        recipes.add(wind);
        return recipes;
    }

    @Override
    public Recipe getCurrentRecipe() {
        Recipe recipe = super.getCurrentRecipe();

        // max turbine output
        float maxOutput = 2f;

        // square relationship
        float windFactor = (building.city.getWindSpeed() / 20f);
        windFactor = windFactor * windFactor; // so real wind power is cubic but that's a bit much

        return new Recipe(
            recipe.getKey(),
            recipe.getName(),
            -1f * maxOutput * windFactor,
            recipe.getWorkers(),
            recipe.getInputs(),
            recipe.getOutputs()
        );
    }

    @Override
    public void place(Building building, boolean newPlace) {
        this.building = building; // get building when placed, just because the current recipe needs the game time
        super.place(building, newPlace);
    }

    @Override
    public void update(Building building, float delta) {
        building.getWindow().updateDisplayTable();
        updateRecipeDisplay(building);
        super.update(building, delta);
    }

    @Override
    public boolean isScalable() {
        return false;
    }
}
