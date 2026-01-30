package com.catcat.hypercity.definitions.campaign;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.city.City;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LevelData implements Json.Serializable {
    private StartingCondition startingCondition;
    private Array<WinningCondition> winningConditions = new Array<>();
    private String story;
    private String name;
    private Set<String> bannedBuildings = new HashSet<>(); //start with no buildings banned
    private final transient Array<BuildingInfo> buildings = new Array<>(); //start off the city with building information to be built. If it was not transient, it would replace every time it's reloaded maybe.

    @SuppressWarnings("unused")
    public LevelData(){}
    public LevelData(String levelName, StartingCondition startingCondition, WinningCondition winningCondition, String story) {
        name = levelName;
        this.startingCondition = startingCondition;
        this.winningConditions.add(winningCondition);
        this.story = story;
    }

    public boolean checkWinningCondition(City city) {
        boolean won = true;
        for (WinningCondition winningCondition : new Array.ArrayIterable<>(winningConditions)) {
            won = won && winningCondition.checkWinningCondition(city);
        }
        return won;
    }

    public String getStory() {
        return story;
    }

    public StartingCondition getStartingCondition() {
        return startingCondition;
    }

    public String getName() {
        return name;
    }

    //ye we using method chaining here
    public LevelData banBuilding(String... keys) {
        bannedBuildings.addAll(Arrays.asList(keys));
        return this;
    }

    /**
     * so you can start from empty
     *
     * @return this
     */
    public LevelData banAll() {
        ObjectMap.Values<BuildingDefinition> definitions = BuildingLoader.getAll();
        for (BuildingDefinition def : definitions) {
            bannedBuildings.add(def.key);
        }
        return this;
    }

    public LevelData unbanBuilding(String... keys) {
        Arrays.asList(keys).forEach(bannedBuildings::remove);
        return this;
    }

    public LevelData addWinningCondition(WinningCondition... winningConditions) {
        this.winningConditions.addAll(winningConditions);
        return this;
    }

    public LevelData addStartingBuilding(String key, float x, float y) {
        buildings.add(new BuildingInfo(x, y, key)); //buildings will be actually made in the city
        return this;
    }

    public Array<WinningCondition> getWinningConditions() {
        return winningConditions;
    }

    public Set<String> getBannedBuildings() {
        return bannedBuildings;
    }

    @Override
    public void write(Json json) {
        json.writeValue("name", name);
        json.writeValue("story", story);
        json.writeValue("startingCondition", startingCondition, StartingCondition.class);
        json.writeValue("winningConditions", winningConditions, Array.class, WinningCondition.class);
        json.writeValue("bannedBuildings", bannedBuildings, Array.class, BuildingDefinition.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(Json json, JsonValue jsonData) {
        name = json.readValue("name", String.class, jsonData);
        story = json.readValue("story", String.class, jsonData);
        startingCondition = json.readValue("startingCondition", StartingCondition.class, jsonData);
        winningConditions = json.readValue("winningConditions", Array.class, WinningCondition.class, jsonData);
        bannedBuildings = json.readValue("bannedBuildings", Set.class, BuildingDefinition.class, jsonData);
    }

    public Array<BuildingInfo> getBuildings() {
        return buildings;
    }

    public static class BuildingInfo {
        public float x;
        public float y;
        public String key;

        public BuildingInfo(float x, float y, String key){
            this.x = x;
            this.y = y;
            this.key = key;
        }
    }
}
