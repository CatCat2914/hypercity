package com.catcat.hypercity.display.resources;

import com.badlogic.gdx.graphics.Color;
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

    @SuppressWarnings("DefaultLocale")
    @Override
    public void act(float delta) {
        float changeRate = inventory.getChangeRate(resource);
        label.setText(resource.name + ": " +
            String.format("%.2f", inventory.getAmount(resource)) +
            " (dx: " + String.format("%+.2f", changeRate));
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
