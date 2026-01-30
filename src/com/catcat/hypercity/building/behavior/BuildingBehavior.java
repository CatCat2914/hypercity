package com.catcat.hypercity.building.behavior;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.display.building.BuildingWindow;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.loaders.BuildingLoader;

public abstract class BuildingBehavior implements Json.Serializable {
    private BuildingDefinition definition;

    public BuildingBehavior() {
    }
    public BuildingBehavior(BuildingDefinition definition){
        setDefinition(definition);
    }

    public void place(Building building, boolean newPlace) {
    }

    /**
     * called each tick.
     *
     * @param building building
     * @param delta    delta
     */
    public void update(Building building, float delta) {
    }

    /**
     * the point of it is to distribute the products evenly between all output roads and not everything
     * so also this can be used to get the resource types as well as the amount produced (for initializing default roads and rates)
     *
     * @return what this building produces
     */
    public ObjectMap<ResourceDefinition, Float> getResourceOutputs() {
        return new ObjectMap<>();
    }

    /**
     * @return EnumMap<CityResource, Float> CityResource is the city resource, float is the consumption
     */
    public ObjectMap<ResourceDefinition, Float> getResourceInputs() {
        return new ObjectMap<>();
    }

    /**
     * @param building       for DATA
     * @param buildingWindow for UI (overwrite tabs and other helper methods. The reason it's needed is because it's not instantiated in building yet)
     * @return the custom tabs
     */
    public Array<Table> getCustomTabContent(Building building, BuildingWindow buildingWindow) { //purpose of the custom tabs
        return null;
    }

    public Array<String> getCustomTabNames() {
        return null;
    }

    /**
     * @param building the current building
     * @param tabName  the name of the active tab in the window
     * @return The default method returns the default help, when no specific help has been made for that tab. Methods overriding this can specify what they want to return.
     */
    public Table getHelpTab(Building building, String tabName) {
        switch (tabName) {
            case "Info": {
                Table infoHelp = new Table();
                infoHelp.setName("Info Help");
                infoHelp.pad(4f);
                Label title = new Label("Info Help", building.city.game.skin, "window");
                title.setColor(Color.CYAN);

                Label info = new Label(
                    "This is the default info tab help.\n\n" +
                        "Tips:\n" +
                        "   • Here you can see info such as position.\n" +
                        "   • 'Change Position' allows you to change the building's position.\n" +
                        "   • 'Add Road' adds or removes roads after you click the target building. Right-clicking the building has the same functionality.\n" +
                        "   • 'Delete' will delete the building and all resources in it.",
                    building.city.game.skin);
                info.setWrap(true);
                infoHelp.add(title).padBottom(6f).row();
                infoHelp.add(info).width(280f).left().row();
                return infoHelp;

            }
            case "Resources": {

                Table defaultHelp = new Table();
                defaultHelp.setName("Resources Help");
                defaultHelp.pad(4f);
                Label title = new Label("Resources Help", building.city.game.skin, "window");
                title.setColor(Color.CYAN);

                Label info = new Label(
                    "Resource tab help.\n\n" +
                        "See what resources are stored and how quickly they are increasing or decreasing.",
                    building.city.game.skin);
                info.setWrap(true);

                defaultHelp.add(title).padBottom(6f).row();
                defaultHelp.add(info).width(200f).left().row();
                return defaultHelp;
            }
            default: {
                Table defaultHelp = new Table();
                defaultHelp.setName("Help");
                defaultHelp.pad(4f);
                Label info = new Label(
                    "This tab type currently has no specific help entry.\n\n",
                    building.city.game.skin);
                info.setWrap(true);
                defaultHelp.add(info).width(200f).left().row();
                return defaultHelp;
            }
        }
    }

    public BuildingDefinition getDefinition() {
        return definition;
    }

    protected void setDefinition(BuildingDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void write(Json json) {
        json.writeValue("definition", definition.key, String.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        definition = BuildingLoader.getByKey(json.readValue("definition", String.class, jsonData));
    }
}
