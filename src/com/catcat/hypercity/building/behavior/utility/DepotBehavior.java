package com.catcat.hypercity.building.behavior.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.transport.Road;
import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.loaders.ResourceLoader;


public class DepotBehavior extends BuildingBehavior {
    private static final float INPUT_RATE = 32.0f;
    private static final float OUTPUT_RATE = 32.0f;
    @SuppressWarnings("unused")
    public DepotBehavior(){}
    public DepotBehavior(BuildingDefinition definition) {
        setDefinition(definition);
    }

    public void place(Building building, boolean newPlace) {
        building.localInventory = building.city.linkInventory(building);
    }

    public void update(Building depot, float delta) {

        for (Road road : new Array.ArrayIterable<>(depot.roads)) {
            Building b = road.getTargetBuilding();
            if (b.isOfType("DEPOT")) continue;
            for (ResourceDefinition resource : new ObjectMap.Keys<>(b.behavior.getResourceInputs())) {
                depot.city.addDepotRequest(new Request(b, resource, delta * road.getOverallSpeed(resource)));
            }
        }
    }

    public ObjectMap<ResourceDefinition, Float> getResourceOutputs() {
        ObjectMap<ResourceDefinition, Float> products = new ObjectMap<>();
        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            products.put(resource, OUTPUT_RATE);
        }
        return products;
    }

    public ObjectMap<ResourceDefinition, Float> getResourceInputs() {
        ObjectMap<ResourceDefinition, Float> requestedResources = new ObjectMap<>();
        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            requestedResources.put(resource, INPUT_RATE);//does not consume resources
        }
        return requestedResources;
    }

    //so the depots all put in a request for how much of each resource they want
    public static class Request {
        public final Building targetBuilding;   // which building should receive
        public final ResourceDefinition resource;
        public final float requested;

        Request(Building targetBuilding, ResourceDefinition resource, float requested) {
            this.targetBuilding = targetBuilding;
            this.resource = resource;
            this.requested = requested;
        }
    }

    public Table getHelpTab(Building building, String tabName) {
        if (tabName.equals("Info")) {
            Table nodeHelp = new Table();
            nodeHelp.setName("Info");
            nodeHelp.pad(4f);
            Label title = new Label("Depot Help", building.city.game.skin, "window");
            title.setColor(Color.CYAN);

            Label info = new Label(
                "The depot is a link to/from the city inventory. All depots are connected to this same inventory.\n\nEach depot has a max input rate of "+INPUT_RATE+"u/s and a max output rate of "+OUTPUT_RATE+"u/s.",
                building.city.game.skin);
            info.setWrap(true);

            nodeHelp.add(title).padBottom(6f).row();
            nodeHelp.add(info).width(300f).left().row();
            return nodeHelp;
        }
        return super.getHelpTab(building, tabName);
    }

}
