package com.catcat.hypercity;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.TimeUtils;
import com.catcat.hypercity.building.behavior.recipe.RecipeBuildingBehavior;
import com.catcat.hypercity.building.behavior.recipe.SolarPanelBuildingBehavior;
import com.catcat.hypercity.building.behavior.recipe.WindTurbineBuildingBehavior;
import com.catcat.hypercity.building.behavior.utility.DepotBehavior;
import com.catcat.hypercity.building.behavior.utility.NodeBehavior;
import com.catcat.hypercity.building.behavior.utility.ScriptingModuleBehavior;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.exception.InvalidGameDataException;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.campaign.CampaignManager;
import com.catcat.hypercity.loaders.RecipeRegistry;
import com.catcat.hypercity.loaders.ResourceLoader;
import com.catcat.hypercity.screens.MainMenuScreen;
import com.catcat.hypercity.story.StoryManager;

import java.util.Arrays;

//todo: do game balancing
// TODO: 12/29/25 actually add education (productivity boost), would be slight challenge
// TODO: 1/26/26 car manufacturing?

public class CityGame extends Game implements Json.Serializable {
    public transient SpriteBatch batch;
    public transient Skin skin;
    public transient final AssetManager assets = new AssetManager();
    public transient Music mainMenuMusic;
    public transient Music cityMusic;
    public transient Sound click;
    public transient Screen mainMenu;
    public CampaignManager campaignManager;
    public StoryManager storyManager;
    private Array<City> cities = new Array<>();
    private transient long lastSaveTime = TimeUtils.millis(); // timestamp of last save in milliseconds
    private static final long SAVE_INTERVAL = 60_000; // 60 seconds

    @Override
    public void create() {
        Gdx.app.log("debug",Arrays.toString(Gdx.files.internal("").list()));
        batch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        FileHandle[] packs = getPacks(); // get all the packs
        if (packs.length == 0) {
            throw new InvalidGameDataException("No game data packs found.");
        }
        ResourceLoader.load(packs);
        BuildingLoader.load(packs);
        RecipeRegistry.register();
        campaignManager = new CampaignManager();
        String[] assetsLines = Gdx.files.internal("assets.txt").readString().split("\\r?\\n");
        for (String path : assetsLines) {
            path = path.trim();
            if (path.isEmpty()) continue;
            if (!Gdx.files.internal(path).exists()) {
                throw new InvalidGameDataException("Missing asset: " + path);
            }
            if (path.endsWith(".png")) {
                assets.load(path, Texture.class);
            }
        }
        click = Gdx.audio.newSound(Gdx.files.internal("audio/click.mp3"));
        mainMenuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/Meninjau - Noctara.mp3"));
        cityMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/Audioinsmusic - Journey.mp3"));
        cityMusic.setLooping(true);
        //noinspection StatementWithEmptyBody
        while (!assets.update()); //wait
        storyManager = new StoryManager(this);
        if (Gdx.app.getType() != Application.ApplicationType.WebGL) {//fixme this is temporary so it can get running (makes it not load saved data)
            load();
        }
        storyManager.onStart();

        this.mainMenu = new MainMenuScreen(this);
        this.setScreen(mainMenu);
        mainMenuMusic.setLooping(true);
        mainMenuMusic.setVolume(.5f);
    }

    @Override
    public void render() { //I can use this to simulate things like updating the story mode city, potentially.
        super.render(); // important!
        storyManager.update();

        //autosave timer
        long currentTime = TimeUtils.millis();
        if (currentTime - lastSaveTime >= SAVE_INTERVAL) {
            save();
            lastSaveTime = currentTime;
        }
    }

    @Override
    public void dispose() {
        save();
        skin.dispose();
        batch.dispose();
        click.dispose();
        assets.dispose();
        mainMenu.dispose();
        cityMusic.dispose();
        mainMenuMusic.dispose();
    }

    private FileHandle[] getPacks() {
        String[] lines = Gdx.files.internal("assets.txt")
            .readString()
            .split("\\r?\\n");

        ObjectSet<String> packNames = new ObjectSet<>();
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("game_data/")) continue;
            String[] parts = line.split("/");
            if (parts.length >= 2) {
                packNames.add(parts[1]);
            }
        }
        FileHandle[] packs = new FileHandle[packNames.size];
        int i = 0;
        for (String packName : new ObjectSet.ObjectSetIterator<>(packNames)) {
            packs[i++] = Gdx.files.internal("game_data/" + packName);
        }
        return packs;
    }

    public Array<City> getCities() {
        return cities;
    }

    public void addCity(City city) {
        this.cities.add(city);
    }

    public void removeCity(City city)
    {
        this.cities.removeValue(city, true);
    }

    @Override
    public void write(Json json) {
        json.writeValue("cities", cities, Array.class, City.class);
        json.writeValue("campaignManager", campaignManager, CampaignManager.class);
        json.writeValue("storyManager", storyManager, StoryManager.class);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        //noinspection unchecked
        cities = json.readValue("cities", Array.class, City.class, jsonData);
        campaignManager = json.readValue("campaignManager", CampaignManager.class, jsonData);
        storyManager = json.readValue("storyManager", StoryManager.class, jsonData);
    }

    private void save() {
        Json json = new Json();
        addClassTags(json);
        Gdx.files.local("save.json").writeString(json.toJson(this), false);
    }

    private void load() {
        FileHandle file = Gdx.files.local("save.json");
        if (!file.exists()) {
            Gdx.app.log("LOAD", "save.json not found, creating new save.");
            return;
        }
        Json json = new Json();
        addClassTags(json);
        CityGame loaded = json.fromJson(CityGame.class, file.readString());
        if (loaded == null) {
            return;
        }
        this.cities = loaded.cities;
        cities.forEach(city -> city.rebuild(this));
        this.campaignManager = loaded.campaignManager;
        this.storyManager = loaded.storyManager;
        for (City c : new Array.ArrayIterable<>(cities)) {
            if (c.isStory()) {
                storyManager.city = c;
            }
        }
        if (storyManager.city == null) {
            Gdx.app.log("LOAD", "No story mode city found, creating new story mode city.");
            storyManager = new StoryManager(this);
        }
        Gdx.app.log("LOAD", "Save data loaded successfully.");
    }
    private void addClassTags(Json json)
    {
        json.addClassTag("DepotBehavior", DepotBehavior.class);
        json.addClassTag("NodeBehavior", NodeBehavior.class);
        json.addClassTag("ScriptingModuleBehavior", ScriptingModuleBehavior.class);
        json.addClassTag("RecipeBuildingBehavior", RecipeBuildingBehavior.class);
        json.addClassTag("SolarPanelBehavior", SolarPanelBuildingBehavior.class);
        json.addClassTag("WindTurbineBehavior", WindTurbineBuildingBehavior.class);
        json.addClassTag("SimpleRecipeBuildingBehavior", BuildingLoader.SimpleRecipeBuildingBehavior.class);
        json.addClassTag("DecorativeBuildingBehavior", BuildingLoader.DecorativeBuildingBehavior.class);
    }

}
