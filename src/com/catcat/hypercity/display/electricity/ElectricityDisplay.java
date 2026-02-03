package com.catcat.hypercity.display.electricity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.catcat.hypercity.city.City;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * this should go here because it is possible for it to be seen and should be documented somewhere.
 * So the deal with electricity is it acts normal from 50-500.
 * Below 50, buildings will start to produce less resources/consume less electricity and by about 0 they will no longer be consuming electricity/producing resources.
 * Above 500 there is exponential decay for electricity added. There is no cap but it does get a lot harder to add eventually.
 * In both cases, equilibrium can be achieved, though it is better to be on the 500+ one of course.
 */
public class ElectricityDisplay extends Table {
    private final Label label;
    private final City city;
    private float electricityLastFrame;
    private final Deque<Float> dxs = new ArrayDeque<>();

    public ElectricityDisplay(Skin skin, City city) {
        this.city = city;
        this.electricityLastFrame = city.getElectricity();

        // Icon
        Image icon = new Image((Texture)city.game.assets.get("electricity.png"));
        this.add(icon).size(32, 32);

        // Label
        this.label = new Label(null, skin);
        this.add(label).left();

        this.row();
    }

    public void act(float delta) {
        float rawChange = (city.getElectricity() - electricityLastFrame) / delta;
        dxs.add(rawChange);
        if (dxs.size() > 60) dxs.removeFirst();
        electricityLastFrame = city.getElectricity();
        float avgChange = (float)dxs.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        String labelString = getLabelString(avgChange);
        label.setText(labelString + ")");

        // Change color based on whether the resource is increasing or decreasing
        if (avgChange >= 0.005f) {
            label.setColor(Color.GREEN);
        } else if (avgChange <= -0.005f) {
            label.setColor(Color.RED);
        } else {
            label.setColor(Color.WHITE); // basically no change
        }
        super.act(delta);
    }
    @SuppressWarnings("DefaultLocale")
    private String getLabelString(float avgChange) {
        String labelString = "Electricity: " + String.format("%.2f", city.getElectricity()) + "\n(dx: " + String.format("%.2f",avgChange);
        if (city.getElectricity() > City.MAX_E_THRESHOLD) {
            labelString += ", loss: " + String.format("%.2f", (100 * (1 - (float)Math.exp(-City.HIGH_E_DECAY * (city.getElectricity() - City.MAX_E_THRESHOLD))))) + "%";
        } else if (city.getElectricity() < City.MIN_E_THRESHOLD) {
            float normalized = Math.max(0f, city.getElectricity()) / (City.MIN_E_THRESHOLD);
            float ratio = normalized * normalized * (3 - 2 * normalized);
            labelString += ", ratio: " + String.format("%.2f", ratio * 100) + "%";
        }
        return labelString;
    }
}
