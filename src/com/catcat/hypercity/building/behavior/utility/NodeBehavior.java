package com.catcat.hypercity.building.behavior.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.display.building.BuildingWindow;
import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.display.ui.CategoryTree;
import com.catcat.hypercity.loaders.ResourceLoader;

public class NodeBehavior extends BuildingBehavior{
    private Array<ResourceDefinition> enabledInputs = new Array<>();
    private Array<ResourceDefinition> enabledOutputs = new Array<>();
    private transient Slider inputSlider;
    private transient Label inputNumber;
    private transient Slider outputSlider;
    private transient Label outputNumber;
    private static final float MAX_SPEED = 10.0f;
    private float inputSpeed = MAX_SPEED;
    private float outputSpeed = MAX_SPEED;
    @SuppressWarnings("unused")
    public NodeBehavior(){}
    public NodeBehavior(BuildingDefinition definition) {
        setDefinition(definition);
    }

    @Override
    public ObjectMap<ResourceDefinition, Float> getResourceOutputs() {
        ObjectMap<ResourceDefinition, Float> products = new ObjectMap<>();
        enabledOutputs.forEach(resource -> products.put(resource, outputSpeed / 2.00f));
        return products;
    }

    public ObjectMap<ResourceDefinition, Float> getResourceInputs() {
        ObjectMap<ResourceDefinition, Float> requested = new ObjectMap<>();
        enabledInputs.forEach(resource -> requested.put(resource, inputSpeed / 2.00f));//these are 2.00f in case I have to remove later
        return requested;
    }

    @Override
    public Array<Table> getCustomTabContent(Building building, BuildingWindow buildingWindow) {
        Array<Table> customTabs = new Array<>();

        Table ratesContent = new Table();
        ratesContent.setName("Rates");
        inputSlider = new Slider(0.1f, MAX_SPEED, 0.1f, false, building.city.game.skin);
        inputSlider.setValue(inputSpeed);
        inputNumber = new Label(Float.toString(inputSpeed), building.city.game.skin);
        ratesContent.add(new Label("Input Rate (0.1-" + MAX_SPEED + "u/s): ", building.city.game.skin)).row();
        ratesContent.add(inputSlider, inputNumber).row();
        outputSlider = new Slider(0.1f, MAX_SPEED, 0.1f, false, building.city.game.skin);
        outputSlider.setValue(outputSpeed);
        outputNumber = new Label(Float.toString(outputSpeed), building.city.game.skin);
        ratesContent.add(new Label("Output Rate (0.1-" + MAX_SPEED + "u/s): ", building.city.game.skin)).row();
        ratesContent.add(outputSlider, outputNumber).row();

        customTabs.add(ratesContent);

        Table selectionTab = new Table(building.city.game.skin);
        Table inputTable = new Table(building.city.game.skin);
        CategoryTree inputTree = new CategoryTree(building.city.game.skin);
        inputTable.add(" Enable Inputs: ").row();
        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            CheckBox cb = new CheckBox(resource.name, building.city.game.skin);
            cb.setChecked(enabledInputs.contains(resource,false));
            addCheckboxListener(building, resource, cb, enabledInputs);
            inputTree.addItem(resource.category, cb);
        }
        inputTable.add(inputTree);

        Table outputTable = new Table(building.city.game.skin);
        CategoryTree outputTree = new CategoryTree(building.city.game.skin);
        outputTable.add(" Enable Outputs: ").row();
        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            CheckBox cb = new CheckBox(resource.name, building.city.game.skin);
            cb.setChecked(enabledOutputs.contains(resource,false));
            addCheckboxListener(building, resource, cb, enabledOutputs);
            outputTree.addItem(resource.category, cb);
        }
        outputTable.add(outputTree);
        selectionTab.add(inputTable).padRight(5);
        selectionTab.add(outputTable);

        customTabs.add(selectionTab);
        return customTabs;
    }

    private void addCheckboxListener(Building building, ResourceDefinition resource, CheckBox cb, Array<ResourceDefinition> enabled) {
        cb.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (cb.isChecked()) {
                    if (!enabled.contains(resource, false)) {
                        enabled.add(resource);
                        // Only add ResourceDisplay if not already present
                        building.getWindow().addDisplayedResource(resource);
                    }
                } else {
                    enabled.removeValue(resource, false);

                    // remove the resource display if both input & output are disabled
                    if (!enabledInputs.contains(resource, false) && !enabledOutputs.contains(resource, false)) {
                        if (building.localInventory.getAmount(resource) < 0.005f) {
                            building.getWindow().removeDisplayedResource(resource);
                        }
                    }
                }
            }
        });
    }

    public Array<String> getCustomTabNames() {
        Array<String> customTabNames = new Array<>();
        customTabNames.add("Rates");
        customTabNames.add("Configure");
        return customTabNames;
    }

    @Override
    public void update(Building building, float delta) {
        // Round slider values to 1 decimal
        float roundedInput = Math.round(inputSlider.getValue() * 10) / 10f;
        float roundedOutput = Math.round(outputSlider.getValue() * 10) / 10f;

        // Update if changed
        if (inputSpeed != roundedInput || outputSpeed != roundedOutput) {
            inputSpeed = roundedInput;
            outputSpeed = roundedOutput;
            inputNumber.setText(Float.toString(inputSpeed));
            outputNumber.setText(Float.toString(outputSpeed));
        }

        super.update(building, delta);
    }

    @Override
    public Table getHelpTab(Building building, String tabName) {
        if (tabName.equals("Rates")) {
            Table nodeHelp = new Table();
            nodeHelp.setName("Rates");
            nodeHelp.pad(4f);
            Label title = new Label("Node Rates", building.city.game.skin, "window");
            title.setColor(Color.CYAN);

            Label info = new Label(
                "You can drag the sliders in the Rates tab of Node to modify the input/output speed of resources.",
                building.city.game.skin);
            info.setWrap(true);

            nodeHelp.add(title).padBottom(6f).row();
            nodeHelp.add(info).width(200f).left().row();
            return nodeHelp;
        }
        if (tabName.equals("Configure")) {
            Table nodeHelp = new Table();
            nodeHelp.setName("Rates");
            nodeHelp.pad(4f);
            Label title = new Label("Node I/O Help", building.city.game.skin, "window");
            title.setColor(Color.CYAN);

            Label info = new Label(
                "Here, you can select the resources that will be taken in and outputted by this node. All resources selected will share the same speed as defined in Rates.",
                building.city.game.skin);
            info.setWrap(true);

            nodeHelp.add(title).padBottom(6f).row();
            nodeHelp.add(info).width(200f).left().row();
            return nodeHelp;
        }
        return super.getHelpTab(building, tabName);
    }

    @Override
    public void write(Json json) {
        super.write(json);
        json.writeValue("inputSpeed", inputSpeed);
        json.writeValue("outputSpeed", outputSpeed);
        json.writeValue("enabledInputs", enabledInputs, Array.class, ResourceDefinition.class);
        json.writeValue("enabledOutputs", enabledOutputs, Array.class, ResourceDefinition.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(Json json, JsonValue jsonData) {
        super.read(json, jsonData);
        inputSpeed = json.readValue("inputSpeed", Float.class, jsonData);
        outputSpeed = json.readValue("outputSpeed", Float.class, jsonData);
        enabledInputs = json.readValue("enabledInputs", Array.class, ResourceDefinition.class, jsonData);
        enabledOutputs = json.readValue("enabledOutputs", Array.class, ResourceDefinition.class, jsonData);
    }
}

