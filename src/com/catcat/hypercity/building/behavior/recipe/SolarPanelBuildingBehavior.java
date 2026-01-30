package com.catcat.hypercity.building.behavior.recipe;

import com.badlogic.gdx.utils.Array;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.recipe.Recipe;


public class SolarPanelBuildingBehavior extends RecipeBuildingBehavior {
    private transient Building building;

    @SuppressWarnings("unused")
    public SolarPanelBuildingBehavior(){}
    public SolarPanelBuildingBehavior(BuildingDefinition definition) {
        super(definition);
    }

    @Override
    protected Array<Recipe> defineRecipes() {
        Array<Recipe> recipes = new Array<>();

        Recipe sun = new Recipe(
            "SUN",
            "Use The Sun",
            -1f,
            0,
            new Array<>(),
            new Array<>()
        );

        recipes.add(sun);
        return recipes;
    }

    @Override
    public Recipe getCurrentRecipe() {
        Recipe base = super.getCurrentRecipe();

        float hours = (float)(building.city.getGameTime() / 3600f) % 24f;

        float sunFactor;
        if (hours >= 6f && hours <= 7f) {
            float t = (hours - 6f);
            sunFactor = t * t * (3 - 2 * t);
        } else if (hours > 7f && hours < 17f) {
            sunFactor = 1f;
        } else if (hours >= 17f && hours <= 18f) {
            float t = (hours - 17f);
            sunFactor = 1f - (t * t * (3 - 2 * t));
        } else {
            sunFactor = 0f;
        }
        // chatgpt said to create a derived recipe instead of mutating the base that way it can be immutable
        return new Recipe(
            base.getKey(),
            base.getName(),
            -1 * sunFactor,
            base.getWorkers(),
            base.getInputs(),
            base.getOutputs()
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
