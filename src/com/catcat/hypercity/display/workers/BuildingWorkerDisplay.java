package com.catcat.hypercity.display.workers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.building.behavior.recipe.RecipeBuildingBehavior;
import com.catcat.hypercity.city.City;

public class BuildingWorkerDisplay extends Table {
    private final Label label;
    private final Building building;

    public BuildingWorkerDisplay(Building building, Skin skin, City city) {
        this.building = building;

        // Icon
        Image icon = new Image((Texture)city.game.assets.get("worker.png"));
        this.add(icon).size(32, 32);

        // Label
        this.label = new Label(null, skin);
        this.add(label).left();

        this.row();
    }

    public void act(float delta) {
        if (!(building.behavior instanceof RecipeBuildingBehavior)) {
            label.setText("Workers: N/A");
            return;
        }
        int workers = ((RecipeBuildingBehavior)building.behavior).getCurrentRecipe().getWorkers();
        if (workers <= 0) {
            label.setText("Workers Housed: " + (building.assignedWorkers) + "/" + (-workers));
        } else {
            label.setText("Workers: " + building.assignedWorkers + "/" + workers);
        }
        super.act(delta);
    }

    public void updateLabel(CharSequence text) {
        label.setText(text);
    }
}
