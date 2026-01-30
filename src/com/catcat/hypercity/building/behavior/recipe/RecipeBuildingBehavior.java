package com.catcat.hypercity.building.behavior.recipe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.recipe.RecipeEntry;
import com.catcat.hypercity.display.building.BuildingWindow;
import com.catcat.hypercity.definitions.recipe.Recipe;
import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.display.resources.RecipeDisplay;
import com.catcat.hypercity.exception.MissingRecipeException;
import com.catcat.hypercity.loaders.ResourceLoader;

public abstract class RecipeBuildingBehavior extends BuildingBehavior implements Json.Serializable {
    private Array<Recipe> recipes;
    private Recipe baseRecipe;
    private Recipe currentRecipe;
    private int scale = 1;
    protected abstract Array<Recipe> defineRecipes();
    private final Table recipeHolder = new Table();

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("recipe", baseRecipe.getKey(), String.class);
        json.writeValue("scale", scale);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        this.recipes=defineRecipes();
        try {
            baseRecipe = getRecipeByKey(json.readValue("recipe", String.class, jsonData));
        }
        catch (MissingRecipeException e)
        {
            Gdx.app.error("GAME_DATA",e.getMessage()+", setting recipe to first.");
            baseRecipe = recipes.first();
        }
        scale = json.readValue("scale", Integer.class, jsonData);
        currentRecipe = getScaledRecipe();
    }
    private Recipe getRecipeByKey(String key)
    {
        for (int i = 0; i < recipes.size; i++) {
            if(recipes.get(i).getKey().equals(key))
            {
                return recipes.get(i);
            }
        }
        throw new MissingRecipeException("Recipe not found for building type \""+this.getDefinition().key+"\": "+key);
    }
    public RecipeBuildingBehavior(){}
    protected RecipeBuildingBehavior(BuildingDefinition definition) {
        super.setDefinition(definition);
        recipes = defineRecipes();
        baseRecipe = recipes.first();
        currentRecipe = baseRecipe;
    }

    @Override
    public void place(Building building, boolean newPlace) {
        super.place(building, newPlace);
        if (newPlace && getCurrentRecipe().getWorkers() < 0) {
            for (RecipeEntry entry : new Array.ArrayIterator<>(getCurrentRecipe().getInputs())) {
                building.localInventory.addAmount(entry.getMatchingResources().first(), entry.value * 10);
            }
        }
        updateRecipeDisplay(building);
    }

    public Array<Recipe> getRecipes() {
        return recipes;
    }

    public Recipe getCurrentRecipe() {
        return currentRecipe;
    }

    /**
     * @param recipe   the base recipe before scaling
     * @param building the building
     */
    private void setCurrentRecipe(Recipe recipe, Building building) {
        baseRecipe = recipe;
        currentRecipe = getScaledRecipe();
        if (building != null && building.getWindow() != null) {
            building.getWindow().updateDisplayTable();
            updateRecipeDisplay(building);
        }
    }

    void updateRecipeDisplay(Building building) {
        recipeHolder.clear();
        recipeHolder.add(new RecipeDisplay(getCurrentRecipe(), building.city));
    }

    private Recipe getScaledRecipe() {
        if (!isScalable()) return baseRecipe;
        Array<RecipeEntry> scaledInputs = scaleArray(baseRecipe.getInputs(), scale);
        Array<RecipeEntry> scaledOutputs = scaleArray(baseRecipe.getOutputs(), scale);
        return new Recipe(baseRecipe.getKey(), baseRecipe.getName(),
            baseRecipe.getPowerConsumption() * scale,
            baseRecipe.getWorkers() * scale,
            scaledInputs, scaledOutputs);
    }
    private Array<RecipeEntry> scaleArray(Array<RecipeEntry> array, int factor) {
        Array<RecipeEntry> scaled = new Array<>();
        for (RecipeEntry entry:new Array.ArrayIterable<>(array)) {
            RecipeEntry s = new RecipeEntry();
            s.attribute = entry.attribute;
            s.tag = entry.tag;
            s.resource = entry.resource;
            s.value = entry.value * factor;
            scaled.add(s);
        }
        return scaled;
    }

    @Override
    public void update(Building building, float delta) {
        Recipe recipe = getCurrentRecipe();
        ObjectMap<RecipeEntry, ResourceDefinition> resolvedInputs = new ObjectMap<>();
        // pick best resource once per entry
        for (RecipeEntry entry : new Array.ArrayIterable<>(recipe.getInputs())) {
            resolvedInputs.put(entry, entry.getBestResource(building));
        }
        //maybe we can't produce the full amount so we need a ratio 2026-1-28 idk if this is still needed idk
        float ratio = recipe.getWorkers() > 0 ? building.assignedWorkers / (float)recipe.getWorkers() : 1f;
        for (RecipeEntry entry : new Array.ArrayIterable<>(recipe.getInputs())) {
            ResourceDefinition resource = resolvedInputs.get(entry);
            float value = entry.value;
            if (value == 0) continue;
            ratio = Math.min(ratio, building.localInventory.getAmount(resource) / (value * delta)); //unitless
        }
        building.requestedElectricity = recipe.getPowerConsumption() * ratio;
        float electricityRatio = (building.requestedElectricity <= 0) ? 1f : Math.min(1f, building.receivedElectricity / building.requestedElectricity);//the ternary check is for things like the tree or the coal power plant
        float r = Math.min(ratio, electricityRatio);
        recipe.getInputs().forEach((entry) -> {
            ResourceDefinition resource = resolvedInputs.get(entry);
            building.localInventory.addAmount(resource, -entry.value / entry.getPerUnitValue(resource) * delta * r);
        });
        recipe.getOutputs().forEach((entry) -> {
            if (entry.resource == null) {
                throw new IllegalStateException("Recipe output must be a concrete resource");
            }
            ResourceDefinition resource = ResourceLoader.getByKey(entry.resource);
            building.localInventory.addAmount(resource, entry.value * delta * r);
        });
        if (recipe.getWorkers() < 0) {
            building.city.addWorkers(-Math.round(recipe.getWorkers() * r));
            building.assignedWorkers -= Math.round(recipe.getWorkers() * r);
        }
    }

    @Override
    public ObjectMap<ResourceDefinition, Float> getResourceOutputs() {
        ObjectMap<ResourceDefinition, Float> out = new ObjectMap<>();

        for (RecipeEntry entry : new Array.ArrayIterable<>(getCurrentRecipe().getOutputs())) {
            // enforce concrete resource
            if (entry.resource == null) {
                throw new IllegalStateException("Recipe output must be a concrete resource");
            }

            ResourceDefinition res = ResourceLoader.getByKey(entry.resource);
            out.put(res, entry.value); //use the json value directly because it has to be of type resource
        }

        return out;
    }

    @Override
    public ObjectMap<ResourceDefinition, Float> getResourceInputs() {
        ObjectMap<ResourceDefinition, Float> resolved = new ObjectMap<>();
        ObjectSet<ResourceDefinition> seen = new ObjectSet<>();

        for (RecipeEntry entry : new Array.ArrayIterable<>(getCurrentRecipe().getInputs())){
            for (ResourceDefinition res : new Array.ArrayIterable<>(entry.getMatchingResources())) {
                // only add unique resources
                if (seen.add(res)) {
                    resolved.put(res, entry.value/entry.getPerUnitValue(res));
                }
            }
        }

        return resolved;
    }

    private void setRecipeScale(Building building, int newScale) {
        scale = newScale;
        setCurrentRecipe(baseRecipe, building);
    }

    public boolean isScalable() {
        return true;
    }

    public int getScale() {
        return scale;
    }

    @Override
    public Array<Table> getCustomTabContent(Building building, BuildingWindow buildingWindow) {
        Array<Table> customTabs = new Array<>();
        customTabs.add(getRecipeTab(building));
        return customTabs;
    }

    @Override
    public Array<String> getCustomTabNames() {
        Array<String> names = new Array<>();
        names.add("Recipes");
        return names;
    }

    @Override
    public Table getHelpTab(Building building, String tabName) {
        if (tabName.equals("Recipes")) {
            Table recipeHelp = new Table();
            recipeHelp.setName("Recipe Help");
            recipeHelp.pad(4f);
            Label title = new Label("Recipe Help", building.city.game.skin, "window");
            title.setColor(Color.CYAN);

            Label info = new Label(
                "Recipe tab help.\n\n" +
                    "Here you can change what the building produces, and the scale of the building. A higher scale will produce more, but be careful, as it will also consume more!",
                building.city.game.skin);
            info.setWrap(true);

            recipeHelp.add(title).padBottom(6f).row();
            recipeHelp.add(info).width(200f).left().row();
            return recipeHelp;
        }
        return super.getHelpTab(building, tabName);
    }

    //<editor-fold desc="make recipe tab">
    private Table getRecipeTab(Building building) {
        Table content = new Table();
        content.add(new Label("Current Recipe:", building.city.game.skin)).row();
        content.add(recipeHolder).row();
        if (isScalable()) {
            content.add(getScaleSelector(building)).row();
        }
        Array<TextButton> recipeButtonArray = new Array<>();
        for (Recipe recipe : new Array.ArrayIterator<>(getRecipes())) {
            TextButton button = new TextButton(recipe.getName(), building.city.game.skin, "toggle"){
                @Override
                public void act(float delta) {
                    super.act(delta);
                    this.setChecked(getCurrentRecipe().equals(recipe));
                }
            };
            content.add(button).width(button.getWidth()+8f).row();
            recipeButtonArray.add(button);
        }
        makeRecipeButtons(recipeButtonArray, building);

        //make it scrollable so we can have a lot of recipes

        // Wrap the content table in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // allow vertical scrolling
        scrollPane.setForceScroll(false, true);
        scrollPane.setSmoothScrolling(true);
        scrollPane.setFillParent(false); // don't force full height
        scrollPane.setClamp(true);


        //limit height
        Table wrapper = new Table();
        wrapper.add(scrollPane).growX().height(Math.min(content.getPrefHeight(), 250f)).row();
        return wrapper;
    }

    private Actor getScaleSelector(Building building) {
        RecipeBuildingBehavior behavior = (RecipeBuildingBehavior)building.behavior;

        // Label replaces TextField
        Label scaleLabel = new Label(String.valueOf(behavior.getScale()), building.city.game.skin);
        scaleLabel.setAlignment(Align.center);

        TextButton decrementButton = new TextButton("-1", building.city.game.skin);
        TextButton largeDecrementButton = new TextButton("-5", building.city.game.skin);
        TextButton incrementButton = new TextButton("+1", building.city.game.skin);
        TextButton largeIncrementButton = new TextButton("+5", building.city.game.skin);

        // Add listeners
        incrementButton.addListener(makeScaleChangeListener(building, scaleLabel, +1));
        largeIncrementButton.addListener(makeScaleChangeListener(building, scaleLabel, +5));
        decrementButton.addListener(makeScaleChangeListener(building, scaleLabel, -1));
        largeDecrementButton.addListener(makeScaleChangeListener(building, scaleLabel, -5));

        // Layout
        Table table = new Table(building.city.game.skin);
        table.add(new Label("Scale: ", building.city.game.skin));
        table.add(largeDecrementButton).width(largeDecrementButton.getWidth()+8f).padRight(4f);
        table.add(decrementButton).width(decrementButton.getWidth()+8f);
        table.add(scaleLabel).width(30);
        table.add(incrementButton).width(incrementButton.getWidth()+8f).padRight(4f);
        table.add(largeIncrementButton).width(largeIncrementButton.getWidth()+8f);

        return table;
    }

    private ChangeListener makeScaleChangeListener(Building building, Label scaleLabel, int delta) {
        int minScale = 1;
        int maxScale = 10;

        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                RecipeBuildingBehavior behavior = (RecipeBuildingBehavior)building.behavior;
                try {
                    int currentValue = Integer.parseInt(scaleLabel.getText().toString());
                    int newValue = MathUtils.clamp(currentValue + delta, minScale, maxScale);
                    scaleLabel.setText(String.valueOf(newValue));
                    behavior.setRecipeScale(building, newValue);
                    building.getWindow().setScale(behavior.getScale());
                } catch (NumberFormatException e) {
                    // reset label if text somehow invalid
                    scaleLabel.setText(String.valueOf(behavior.getScale()));
                }
            }
        };
    }

    private void makeRecipeButtons(Array<TextButton> recipeButtonArray, Building building) {

        ButtonGroup<TextButton> recipeButtonGroup = new ButtonGroup<>();
        for (int i = 0; i < recipeButtonArray.size; i++) {
            TextButton b = recipeButtonArray.get(i);
            Recipe r = recipes.get(i);
            b.pad(4f);
            b.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    setCurrentRecipe(r, building);
                }
            });

            recipeButtonGroup.add(b);
        }
        recipeButtonGroup.setMaxCheckCount(1);
        recipeButtonGroup.setMinCheckCount(1);
        recipeButtonGroup.setChecked(recipeButtonArray.get(0).getText().toString());
    }
    //</editor-fold>
}
