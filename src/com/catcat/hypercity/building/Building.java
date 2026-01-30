package com.catcat.hypercity.building;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.display.building.BuildingWindow;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.resources.ResourceInventory;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.transport.Road;
/**
 * Manages things common to all buildings.
 */
public class Building implements Json.Serializable{
    private int id;
    private float x;
    private float y;
    public transient Image image;// TODO: 10/16/25 add option for different images per recipe (building's original image can stay as a default)

    public BuildingBehavior behavior;
    public ResourceInventory localInventory = new ResourceInventory();
    public transient City city;
    private boolean isPrioritized;
    public Array<Road> roads = new Array<>();
    public transient float requestedElectricity = 0f;
    public transient float receivedElectricity = 0f; // amount of electricity received this frame
    public transient int assignedWorkers = 0; //for houses, it's the amount of people it is providing
    private transient BuildingWindow buildingWindow;

    @Override
    public void write(Json json) {
        json.writeValue("x", x);
        json.writeValue("y", y);
        json.writeValue("prioritized", isPrioritized);
        json.writeValue("behavior", behavior, BuildingBehavior.class);
        if(hasUniqueInventory()) {
            json.writeValue("localInventory", localInventory, ResourceInventory.class);
        }
        json.writeValue("roads",roads, Array.class,Road.class);
    }
    private static float snap(float v) {
        return Math.round(v * 2f) / 2f;
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        id = Integer.parseInt(jsonData.name);
        x = json.readValue("x", Float.class, jsonData);
        y = json.readValue("y", Float.class, jsonData);
        isPrioritized = json.readValue("prioritized", Boolean.class, jsonData);
        behavior = json.readValue("behavior", BuildingBehavior.class, jsonData);
        if(hasUniqueInventory()) {
            localInventory = json.readValue("localInventory", ResourceInventory.class, jsonData);
        }
        //noinspection unchecked
        roads = json.readValue("roads", Array.class,Road.class, jsonData);
    }

    public void rebuild(City city){
        rebuild(city, false);
    }
    /**
     * rebuild methods are called after serialization is done.
     */
    private void rebuild(City city, boolean fromConstructor){
        this.city = city;
        for (int i = roads.size-1; i >= 0 ; i--) {
            roads.get(i).rebuild(city);
            if (roads.get(i).isInvalid())
                roads.removeIndex(i);
        }
        this.image = new Image((Texture)city.game.assets.get(behavior.getDefinition().texturePath));
        image.setSize(50, 50);
        image.setPosition(x, y, Align.center);
        city.screen.stage.addActor(image);
        behavior.place(this, fromConstructor);
        buildingWindow = new BuildingWindow(this);
        addLeftClickListener();
        addRightClickListener();
    }
    public Building(){}
    public Building(String key, float x, float y, City city) {
        this.id = city.nextID();
        this.city = city;
        this.behavior = BuildingLoader.createBuilding(BuildingLoader.getByKey(key));
        this.x = snap(x);
        this.y = snap(y);
        rebuild(city, true);
    }

    private void addLeftClickListener() {
        image.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //road logic
                if (city.isMakingRoad()) {
                    if (city.shouldAddOrRemoveRoad(Building.this)) {
                        city.getRoadOrigin().addOrRemoveRoadFromThis(Building.this);
                        city.setNeedShiftToMakeRoad(true);
                    }
                    city.toggleMakingRoad(Building.this);
                    // reset road mode if not shifting
                    if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                        city.stopMakingRoad();
                    }
                    return;
                }
                buildingWindow.toggleVisibility();
            }
        }); //opens building info/settings window
    }

    /**
     * @param to building it's going to
     */
    public void addOrRemoveRoadFromThis(Building to)
    {
        if (to.roads.removeValue(new Road(this, to), false)) {//remove
            roads.removeValue(new Road(this, to), false);
        } else {//add
            final float MAX_ROAD_DISTANCE = 1000f;
            float dx = x-to.getX();
            float dy = y-to.getY();
            if(dx*dx+dy*dy<MAX_ROAD_DISTANCE*MAX_ROAD_DISTANCE) {
                Road road = new Road(this, to);
                to.roads.add(road);
                roads.add(road);
            }
            else {
                city.screen.showDialog("Road too long!", 100);
            }
        }
    }

    private void addRightClickListener() {
        image.addListener(new ClickListener(Input.Buttons.RIGHT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                city.toggleMakingRoad(Building.this);
            }
        });
    }

    public void updatePosition(float x, float y) {
        if (city.canMoveBuilding(this, x, y)) {
            this.x = snap(x);
            this.y = snap(y);
            this.image.setPosition(x, y, 1);
            buildingWindow.changeBuildingPosition(x, y);
        }
    }

    //<editor-fold desc="Update Methods">
    public void update(float delta) {

        //checking every building to see if it still exists ya know
        for (int i = roads.size - 1; i >= 0; i--) {
            Road road = roads.get(i);
            if (city.getBuilding(road.getToID()) == null) {
                roads.removeIndex(i);
            }
        }

        if (usesDefaultDistribution()) { //depots have their own distribution code
            resolveRequests(delta);
        }
        if (hasUniqueInventory()) {
            localInventory.update(delta);
        }
        behavior.update(this, delta);
    }

    private boolean hasUniqueInventory() {
        return !isOfType("base.DEPOT") && !isOfType("base.MULTICITY_LINK");
    }

    private boolean usesDefaultDistribution() {
        return !isOfType("base.DEPOT");
    }

    private void resolveRequests(float delta) {
        //so, this is important. As this is to distribute TO the target building, if there is more than one building linked to the target building, it can increase past the max speed.
        for (ResourceDefinition resource : new ObjectMap.Keys<>(behavior.getResourceOutputs())) {
            float totalRequested = 0f;
            //amount that should be distributed
            Array<Road> targets = new Array<>();
            Array<Float> amounts = new Array<>();
            for (Road road : new Array.ArrayIterable<>(roads)) {
                if (road.getTargetBuilding().behavior.getResourceInputs().containsKey(resource)) {
                    targets.add(road);
                    float requested = delta * road.getOverallSpeed(resource);
                    amounts.add(requested);
                    totalRequested += requested;
                }
            }
            if (totalRequested <= 0) continue;
            float ratio = Math.min(1f, localInventory.getAmount(resource) / totalRequested);
            for (int i = 0; i < targets.size; i++) {
                float grant = amounts.get(i) * ratio;
                localInventory.transferResource(resource, targets.get(i).getTargetBuilding().localInventory, grant);
            }

        }
    }
    //</editor-fold>

    @Override
    public String toString() {
        return "Building{" +
            "key=" + id +
            ", name=" + behavior.getDefinition().name +
            ", position=(" + x + ", " + y + ")" +
            ", roads=" + roads.size +
            '}';
    }

    public int getId() {
        return id;
    }

    public void drawWindowLine() {
        buildingWindow.drawWindowLine(x, y);
    }

    public BuildingWindow getWindow() {
        return buildingWindow;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isPrioritized() {
        return isPrioritized;
    }

    public void setPrioritized(boolean prioritized) {
        isPrioritized = prioritized;
    }

    public int countRoadsTo(String targetKey)
    {
        int count = 0;
        for (Road road : new Array.ArrayIterable<>(roads)) {
            if (road.getTargetBuilding().isOfType(targetKey)) {
                count++;
            }
        }
        return count;
    }

    public boolean hasRoadTo(String targetKey)
    {
        return countRoadsTo(targetKey)>0;
    }

    public String getKey()
    {
        return behavior.getDefinition().key;
    }

    public boolean isOfType(String key)
    {
        return getKey().equals(key);
    }

    public BuildingDefinition getDefinition()
    {
        return behavior.getDefinition();
    }

    public void setDeletable(boolean b) {
        buildingWindow.setDeletable(b);
    }
}
