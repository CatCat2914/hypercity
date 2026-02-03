package com.catcat.hypercity.city;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.catcat.hypercity.CityGame;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.building.behavior.recipe.RecipeBuildingBehavior;
import com.catcat.hypercity.building.behavior.utility.DepotBehavior;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.campaign.LevelData;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.display.city.CityScreen;
import com.catcat.hypercity.exception.MissingBuildingTypeException;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.resources.ResourceInventory;
import com.catcat.hypercity.loaders.ResourceLoader;
import com.catcat.hypercity.transport.Road;


// TODO: 11/11/25 add tech tree
public class City implements Json.Serializable {

    //<editor-fold desc="Class Constants">
    public static final float HIGH_E_DECAY = 0.004605f; //decay factor, honestly just ask chatgpt with what you want to do with it rather than calculate exponentials
    public static final float MAX_E_THRESHOLD = 500f; //max electricity before exponential decay starts
    public static final float MIN_E_THRESHOLD = 50f; // above this, full power
    //</editor-fold>

    public transient CityGame game;
    public transient CityScreen screen;

    // <editor-fold desc="Private Instance Variables">
    private String name;
    @SuppressWarnings("PointlessArithmeticExpression")
    private double gameTime = (7 * 3600) + (0 * 60) + 0;//this is the offset from 2050-01-01T00:00:00Z
    private ResourceInventory cityInventory;
    //key, building
    private ObjectMap<Integer, Building> buildings = new ObjectMap<>();
    private float electricity = 0f; //electricity is NOT a resource.
    private transient int workers;// TODO: 1/20/26 figure this out so it shows the right number upon loading game (try to avoid updating everything)
    private transient int assignedWorkers;//workers assigned to work this frame
    private transient int thisFrameWorkers = 0;// workers are NOT a resource and are NOT consumable. This one is 0 except between update and resolveWorkers.
    private transient boolean isMakingRoad;
    private transient Building roadOrigin = null;
    private transient boolean needShiftToMakeRoad;
    private final transient Array<DepotBehavior.Request> pendingRequests = new Array<>();
    private float windSpeed = 10f;
    private int nextBuildingID = 0;
    private float targetWindSpeed = windSpeed; // equal to start to set a new target
    private LevelData levelData;
    private transient boolean isCampaign;
    private boolean isStory;
    private ObjectSet<String> unlockedBuildings = new ObjectSet<>();
    private transient boolean hasWon = false;

    //these do not update, they only exist because reading and making camera do not happen in the same method. Once these are used, do not use again.
    private float savedCameraX = 0;
    private float savedCameraY = 0;
    private float savedZoom = 1;
    // </editor-fold>

    //<editor-fold desc="Constructors">
    public City() {}

    public City(final CityGame game, String name) {
        this(game, name, null);
        electricity += 350f;
        for (BuildingDefinition def : BuildingLoader.getAll()) {
            unlockBuilding(def);
        }
    }

    public City(final CityGame game, LevelData levelData) {
        this(game, levelData == null ? "Sandbox" : levelData.getName(), levelData);
        for (BuildingDefinition def : BuildingLoader.getAll()) {
            unlockBuilding(def);
        }
    }
    private City(final CityGame game, String name, LevelData levelData) {
        this.name = name;
        this.levelData = levelData;
        this.cityInventory = new ResourceInventory();
        rebuild(game);
        if (isCampaign) {
            for (LevelData.BuildingInfo building:new Array.ArrayIterable<>(levelData.getBuildings())) {
                addBuilding(new Building(building.key, building.x, building.y, this));
            } 
            electricity += levelData.getStartingCondition().getStartingElectricity();
            levelData.getStartingCondition().getStartingResources().forEach(cityInventory::addAmount);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Save/Load">
    @Override
    public void write(Json json) {
        json.writeValue("name", name);
        json.writeValue("gameTime", Math.floor(gameTime));
        json.writeValue("cityInventory", cityInventory, ResourceInventory.class);
        json.writeObjectStart("buildings");
        for (ObjectMap.Entry<Integer, Building> entry : new ObjectMap.Entries<>(buildings)) {
            json.writeValue(entry.key.toString(), entry.value, Building.class);//idk
        }
        json.writeObjectEnd();
        json.writeValue("electricity", electricity);
        json.writeValue("windSpeed", windSpeed);
        json.writeValue("nextBuildingID", nextBuildingID);
        json.writeValue("targetWindSpeed", targetWindSpeed);
        json.writeValue("camX",screen.stage.getCamera().position.x);
        json.writeValue("camY",screen.stage.getCamera().position.y);
        json.writeValue("zoom",((OrthographicCamera)screen.stage.getCamera()).zoom);
        if(levelData!=null) json.writeValue("levelData", levelData, LevelData.class);
        json.writeValue("isStory", isStory);
        if(isStory||levelData!=null) {
            json.writeValue("unlockedBuildings", unlockedBuildings, ObjectSet.class, String.class);
        }
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        name = json.readValue("name", String.class, jsonData);
        gameTime = json.readValue("gameTime", Double.class, jsonData);
        cityInventory = json.readValue("cityInventory", ResourceInventory.class, jsonData);
        buildings = new ObjectMap<>();
        JsonValue buildingsNode = jsonData.get("buildings");
        for (JsonValue node = buildingsNode.child; node != null; node = node.next) {
            int key = Integer.parseInt(node.name());
            try {
                Building value = json.readValue(Building.class, node);
                buildings.put(key, value);
            }
            catch(MissingBuildingTypeException e)
            {
                Gdx.app.error("GAME_DATA",e.getMessage()+", skipping building in city: \""+getName()+"\"");
            }
        }
        electricity = json.readValue("electricity", Float.class, jsonData);
        windSpeed = json.readValue("windSpeed", Float.class, jsonData);
        nextBuildingID = json.readValue("nextBuildingID", Integer.class, jsonData);
        targetWindSpeed = json.readValue("targetWindSpeed", Float.class, jsonData);
        //null check, may need elsewhere too
        JsonValue v = jsonData.get("levelData");
        levelData = (v == null || v.isNull()) ? null : json.readValue(LevelData.class, v);
        isStory = json.readValue("isStory", Boolean.class, jsonData);
        savedCameraX = json.readValue("camX", Float.class, jsonData);
        savedCameraY = json.readValue("camY", Float.class, jsonData);
        savedZoom = json.readValue("zoom",Float.class, jsonData);
        if(isStory||levelData!=null) {
            //noinspection unchecked
            unlockedBuildings = json.readValue("unlockedBuildings", ObjectSet.class, jsonData);
        }
    }

    /**
     * Called after deserialization is complete.
     */
    public void rebuild(CityGame game) {
        this.game = game;
        this.isCampaign = (levelData != null);
        this.screen = new CityScreen(this);
        moveCamera(savedCameraX, savedCameraY);
        zoomCamera(savedZoom);
        if(!isCampaign&&!isStory){
            for (BuildingDefinition def : BuildingLoader.getAll()) {
                unlockBuilding(def);
            }
        }
        if(isStory)
        {
            startStory();
        }
        buildings.forEach(entry -> entry.value.rebuild(this));
    }
    //</editor-fold>
    public void logic(float delta) {
        if (!hasWon && levelData != null && levelData.checkWinningCondition(this)) {
            hasWon = true;
            screen.displayWinDialog();
            game.campaignManager.beatLevel(levelData);
        }
        updateTime(delta, 60f);
        updateWind(delta);
        updateBuildings(delta);
        cityInventory.update(delta);
    }

    // <editor-fold desc="Environmental Management">
// TODO: 11/6/25 add a full weather system and seasons (just because realism idk, maybe we can make things like water catchers or reduced needs for farms when rain or something). Can't forget about clouds hindering solar panels.

    /**
     * @param timeScaling 1 = real time, 60 = 1 min per second, etc.
     */
    private void updateTime(float delta, float timeScaling) {
        gameTime += delta * timeScaling;
    }

    private void updateWind(float delta) {
        float minWind = 0f;
        float maxWind = 20f;
        // Occasionally pick a new target every few minutes
        if (Math.abs(windSpeed - targetWindSpeed) < 0.1f) {
            targetWindSpeed = minWind + (float)Math.random() * (maxWind - minWind);
        }
        float changeSpeed = 0.02f; //how much it changes per real second (in game minute)
        // Smoothly move towards the target
        windSpeed += Math.signum(targetWindSpeed - windSpeed) * changeSpeed * delta;
    }

    // </editor-fold>

    // <editor-fold desc="Building Update Management">
    private void updateBuildings(float delta) {
        resolveWorkers();
        generateElectricity(delta);
        distributeElectricity(delta);
        new ObjectMap.Values<>(buildings).forEach(building -> building.update(delta));
        resolveDepotRequests();
    }

    private void distributeElectricity(float delta) {
        float ratio = 1f;
        if (electricity < MIN_E_THRESHOLD) {
            // As electricity -> 0, ratio -> 0 (never reaches it exactly)
            // As electricity -> 50, ratio -> 1
            float normalized = Math.max(0f, electricity) / MIN_E_THRESHOLD;
            ratio = normalized * normalized * (3 - 2 * normalized);
        }
        for (Building building : new ObjectMap.Values<>(buildings)) {
            if (building.requestedElectricity > 0) {
                building.receivedElectricity = (building.requestedElectricity * ratio);
                electricity -= (building.requestedElectricity * ratio * delta);
            }
        }
    }

    private void generateElectricity(float delta) {
        for (Building building : new ObjectMap.Values<>(buildings)) {
            //add up the producers first
            if (building.requestedElectricity < 0) {
                float generated = -building.requestedElectricity; //minus because negative for producers
                float excess = Math.max(0, electricity + generated * delta - MAX_E_THRESHOLD);
                float effectiveGenerated = generated;
                if (excess > 0) {
                    effectiveGenerated = generated * (float)Math.exp(-HIGH_E_DECAY * excess);
                }
                electricity += effectiveGenerated * delta;
            }
        }
    }

    private void resolveWorkers() {
        int usedWorkers = 0;
        for (Building building : new ObjectMap.Values<>(buildings)) building.assignedWorkers = 0;

        for (Building building : new ObjectMap.Values<>(buildings)) {
            if (building.isPrioritized()) {
                usedWorkers = assignWorkersToBuilding(building, usedWorkers);
            }
        }
        for (Building building : new ObjectMap.Values<>(buildings)) {
            if (!building.isPrioritized()) {
                usedWorkers = assignWorkersToBuilding(building, usedWorkers);
            }
        }
        workers = thisFrameWorkers;
        assignedWorkers = usedWorkers;
        thisFrameWorkers = 0;//reset for next frame
    }

    /**
     * Must be after the building updates so we can resolve this frame's requests
     */
    private void resolveDepotRequests() {
        if (pendingRequests.isEmpty()) return;

        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            // collect requests for this resource
            Array<DepotBehavior.Request> resourceRequests = new Array<>();
            float totalRequested = 0f;
            for (DepotBehavior.Request req : new Array.ArrayIterator<>(pendingRequests)) {
                if (req.resource == resource) {
                    resourceRequests.add(req);
                    totalRequested += req.requested;
                }
            }

            if (totalRequested <= 0) continue;

            //amount that should be distributed
            float available = Math.min(cityInventory.getAmount(resource), totalRequested);

            for (DepotBehavior.Request req : new Array.ArrayIterator<>(resourceRequests)) {
                float grant = (req.requested / totalRequested) * available;
                req.targetBuilding.localInventory.addAmount(resource, grant);
            }

            cityInventory.addAmount(resource, -available);
        }

        pendingRequests.clear();
    }

    /**
     * @param building    the building to get workers
     * @param usedWorkers the current number of workers
     * @return the number of workers + assigned
     */
    private int assignWorkersToBuilding(Building building, int usedWorkers) {
        if (!(building.behavior instanceof RecipeBuildingBehavior)) return usedWorkers;
        RecipeBuildingBehavior behavior = (RecipeBuildingBehavior)building.behavior;
        if (behavior.getCurrentRecipe().getWorkers() <= 0) return usedWorkers;
        if (thisFrameWorkers > usedWorkers) {
            int assigned = Math.min(thisFrameWorkers - usedWorkers, behavior.getCurrentRecipe().getWorkers());
            usedWorkers += assigned;
            building.assignedWorkers = assigned;
        }
        return usedWorkers;
    }

    // </editor-fold>

    // <editor-fold desc="Encapsulation Methods">
    public int nextID() {
        return nextBuildingID++;
    }

    public double getGameTime() {
        return gameTime;
    }

    public boolean canPlaceBuilding(float x, float y) {
        final float MIN_DISTANCE = 50f;
        for (Building existing : new ObjectMap.Values<>(buildings)) {
            float dx = x - existing.getX();
            float dy = y - existing.getY();
            float distanceSquared = dx * dx + dy * dy; //squared is faster and works for comparison
            if (distanceSquared < MIN_DISTANCE * MIN_DISTANCE) {
                // Too close to another building
                return false;
            }
        }
        return true;
    }

    public boolean canMoveBuilding(Building building, float x, float y) {
        final float MIN_DISTANCE = 50f;
        final float MAX_ROAD_DISTANCE = 1000f;
        for (Building existing : new ObjectMap.Values<>(buildings)) {
            if (existing == building) continue;
            if (!distanceGreaterThan(x,y,existing.getX(),existing.getY(),MIN_DISTANCE)) {
                screen.showDialog("Too close to existing building!", 100);
                return false;
            }
        }
        for(Road road:new Array.ArrayIterable<>(building.roads))
        {
            if(road.getSourceBuilding().equals(building))
            {
                if (distanceGreaterThan(x,y,road.getTargetBuilding().getX(),road.getTargetBuilding().getY(),MAX_ROAD_DISTANCE)) {
                    screen.showDialog("Moving this building here would result in roads that are too long.", 150);
                    return false;
                }
            }
            if(road.getTargetBuilding().equals(building))
            {
                if (distanceGreaterThan(x,y,road.getSourceBuilding().getX(),road.getSourceBuilding().getY(),MAX_ROAD_DISTANCE)) {
                    screen.showDialog("Moving this building here would result in roads that are too long.", 150);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean distanceGreaterThan(float x1, float y1, float x2, float y2, float max)
    {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceSquared = dx * dx + dy * dy; //squared is faster and works for comparison
        return distanceSquared > max * max;
    }

    public Building getBuilding(int id) {
        return buildings.get(id);
    }

    public ObjectMap.Values<Building> getAllBuildings() {

        return new ObjectMap.Values<>(buildings);
    }

    public int getBuildingCount() {
        return buildings.size;
    }

    //idk just doing some check to confirm it should be linking because why not. pass in building so later I can maybe change how it's done because it's sketchy rn w/ the referencing
    public ResourceInventory linkInventory(Building building) {
        if (building.behavior instanceof DepotBehavior) {
            //why am I not doing this I should see  building.localInventory=cityInventory;
            return cityInventory;
        }
        throw new IllegalArgumentException("This building cannot access city inventory directly");
    }

    public float getResourceAmount(ResourceDefinition resource) {
        return cityInventory.getAmount(resource);
    }

    public float getResourceChangeRate(ResourceDefinition resource) {
        return cityInventory.getChangeRate(resource);
    }

    public float getElectricity() {
        return electricity;
    }

    public int getWorkers() {
        return workers;
    }

    /**
     * not what you think; don't use unless you know you should
     */
    public void addWorkers(int workers) {
        this.thisFrameWorkers += workers;
    }

    public boolean isMakingRoad() {
        return isMakingRoad;
    }

    private void startMakingRoad(Building building) {
        isMakingRoad = true;
        roadOrigin = building;
    }

    public void stopMakingRoad() {
        isMakingRoad = false;
        needShiftToMakeRoad = false;
        roadOrigin = null;
    }

    /**
     * @param building the building
     */
    public void toggleMakingRoad(Building building) {
        if (isMakingRoad()) {
            stopMakingRoad();
        } else {
            startMakingRoad(building);
        }
    }

    public Building getRoadOrigin() {
        return roadOrigin;
    }

    /**
     * checks if the origin is the building and checks shift placing
     *
     * @param building building
     * @return if a road should be added
     */
    public boolean shouldAddOrRemoveRoad(Building building) {
        return building != getRoadOrigin() && (!needShiftToMakeRoad || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));
    }

    public void setNeedShiftToMakeRoad(boolean needShiftToMakeRoad) {
        this.needShiftToMakeRoad = needShiftToMakeRoad;
    }

    public void addDepotRequest(DepotBehavior.Request request) {
        pendingRequests.add(request);
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceInventory getCityInventory() {
        return cityInventory;
    }

    public int getAssignedWorkers() {
        return assignedWorkers;
    }

    public boolean isCampaign() {
        return isCampaign;
    }

    public LevelData getLevelData() {
        return levelData;
    }

    public boolean isStory() {
        return isStory;
    }
    public void declareAsStory() {
        if(isStory) return;
        isStory = true;
        unlockedBuildings.clear();
        startStory();
        screen.buildBuildingPlaceWindow();
    }
    private void startStory()
    {
        screen.addStoryWindow();
    }
    public boolean isBuildingUnlocked(BuildingDefinition def) {
        return unlockedBuildings.contains(def.key);
    }

    // </editor-fold>

    //<editor-fold desc="Story Mode Power Tools">
    public void unlockBuilding(BuildingDefinition def) {
        unlockedBuildings.add(def.key);
        screen.buildBuildingPlaceWindow();
    }
    public void lockBuilding(BuildingDefinition def) {
        unlockedBuildings.remove(def.key);
        screen.buildBuildingPlaceWindow();
    }
    public void addResourceAmount(ResourceDefinition res, float amount) {
        cityInventory.addAmount(res, amount);
    }


    /**
     * Attempts to add a building to the city.
     *
     * @param building The building to add.
     */
    public void addBuilding(Building building) {
        buildings.put(building.getId(), building);
    }

    public void removeBuilding(Building building) {
        buildings.remove(building.getId());
    }

    public void moveCamera(float x, float y) {
        screen.stage.getCamera().position.set(x, y, 0);
    }

    public void zoomCamera(float zoom) {
        ((OrthographicCamera)screen.stage.getCamera()).zoom = zoom;
    }

    //</editor-fold>
}
