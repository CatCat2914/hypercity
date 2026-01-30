package com.catcat.hypercity.display.resources;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.resources.ResourceInventory;

import java.util.Objects;

public class ResourceDisplay extends Table {
    private final ResourceDefinition resource;
    private final Label label;
    private final ResourceInventory inventory;

    public ResourceDisplay(ResourceDefinition resource, ResourceInventory inventory, City city) {
        this.resource = resource;
        this.inventory = inventory;
        if (city.game.skin == null) {
            label = null;
            return;
        }
        // Icon
        ResourceIcon icon = new ResourceIcon(resource, city);
        this.add(icon).size(32, 32);

        // Label
        label = new Label("", city.game.skin);
        this.add(label).left();

        this.row();
    }

    // FIXME: 1/28/26 negative seems to have 3 decimal places
    private String format2(float value) {
        int intPart = (int)Math.abs(value);
        int decimalPart = (int)Math.abs((value - intPart) * 100);
        return intPart + "." + (decimalPart < 10 ? "0" : "") + decimalPart;
    }

    @Override
    public void act(float delta) {
        float changeRate = inventory.getChangeRate(resource);
        label.setText(resource.name + ": " +
            format2(inventory.getAmount(resource)) +
            " (dx: " + (changeRate >= 0 ? "+" : "-") + format2(changeRate) + ")");
        // Change color based on whether the resource is increasing or decreasing
        if (changeRate >= 0.005f) {
            label.setColor(Color.GREEN);
        } else if (changeRate <= -0.005f) {
            label.setColor(Color.RED);
        } else {
            label.setColor(Color.WHITE); // basically no change
        }
        getChildren().forEach(actor -> actor.act(delta));
    }

    private ResourceDefinition getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ResourceDisplay display = (ResourceDisplay)o;
        return resource == display.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(resource);
    }
}
