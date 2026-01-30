package com.catcat.hypercity.display.workers;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.catcat.hypercity.city.City;

public class CityWorkerDisplay extends Table {
    private final Label label;
    private final City city;

    public CityWorkerDisplay(City city, Skin skin) {
        this.city = city;
        // Icon
        Image icon = new Image((Texture)city.game.assets.get("worker.png"));
        this.add(icon).size(32, 32);

        // Label
        this.label = new Label(null, skin);
        this.add(label).left();

        this.row();
    }

    public void act(float delta) {
        label.setText("Total Workers: " + city.getWorkers() + "\nAssigned: " + city.getAssignedWorkers());
        super.act(delta);
    }
}
