package com.catcat.hypercity.story;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.catcat.hypercity.CityGame;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.loaders.ResourceLoader;

import java.util.function.Predicate;
public class StoryManager {
    public transient boolean cityActive;
    public transient City city;
    private int currentQuest = 0;
    private final transient Array<Quest> quests = new Array<>();
    private static final Quest endQuest = new Quest(null,
        "Congratulations! You have completed every quest this game has to offer. By now, your city has grown and grown, and is a powerful force of production."
        , city -> false);

    @SuppressWarnings("unused")
    private StoryManager(){}

    /**
     * Creates a new StoryManager. Calls to this must be followed up with a call to {@link #onStart()} before this object can be used.
     * @param game The CityGame instance.
     */
    public StoryManager(CityGame game) {
        this.city = new City(game, "Story Mode");
        game.addCity(this.city);
    }

    /**
     * Called upon creating a StoryManager instance. Adds the quests, makes the city into a story mode city, and displays the current quest dialog.
     */
    public void onStart() {
        //<editor-fold desc="Quests">
        //<editor-fold desc="Pre-Population Basics">
        quests.add(
            new Quest(city,
                "Welcome to Story Mode! The flat grassland you will build your city on stretches as far as the eye can see. There is no sign of civilization... for now. Soon, that will all change. This grassland will be the site for the greatest city the world has ever seen. But first, you must look around.\n This game uses a top-down view. You can left click and drag anywhere on the screen to move around, and scroll to zoom in or out. Try it out!\n\nZoom in to 1.1x, zoom out to 0.9x, and move 50 meters from the center.",
                new Predicate<City>(){
                    boolean hasZoomedIn = false;
                    boolean hasZoomedOut = false;
                    boolean hasMovedFromCenter = false;
                    /**
                     * Zoom in to 1.1x, zoom out to 0.9x, and move 50 meters from the center.
                     */
                    @Override
                    public boolean test(City city) {
                        OrthographicCamera camera = ((OrthographicCamera)city.screen.stage.getCamera());
                        if(camera.zoom > 1.1){
                            hasZoomedIn = true;
                        }
                        if(camera.zoom < 0.9){
                            hasZoomedOut = true;
                        }
                        if(camera.position.len() > 50f){
                            hasMovedFromCenter = true;
                        }
                        return hasZoomedOut&&hasZoomedIn&&hasMovedFromCenter;
                    }
                },
                () -> {
                    city.unlockBuilding(BuildingLoader.getByKey("base.FARM"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.WATER_PUMP"));
                })/// Unlock FARM and WATER_PUMP
        );
        quests.add(
            new Quest(city,
                "After looking around, you've decided that this is a great place to build a city. The land is fertile, but also ready to handle the weight of any amount of buildings you choose to build. For your future city, you'll need to have a steady source of food and water for your citizens. You have unlocked Water Pumps and Farms, and you'll need to place these in order to make food. To place a building, expand the category the building is in, click the \"Place\" button, and then click where you want to build it. Buildings are free and are built instantly.\n\nPlace 1 Farm and 1 Water Pump.",
                new Predicate<City>(){
                    /**
                     * Place 1 Farm and 1 Water Pump.
                     */
                    @Override
                    public boolean test(City city) {
                        boolean hasFarm = false;
                        boolean hasPump = false;
                        for(Building building:city.getAllBuildings()) {
                            hasFarm = hasFarm||building.isOfType("base.FARM");
                            hasPump = hasPump||building.isOfType("base.WATER_PUMP");
                        }
                        return hasFarm&&hasPump;
                    }
                })
        );
        quests.add(
            new Quest(city,
                "Great! Both buildings have been built, but right now, the farm won't work because it doesn't have water. To transport resources from one building to another, you need to build a road. Roads in this game are one-way roads. To make a road, click on the source building (in this case, the water pump) to open the building window. The building window has multiple tabs, each of which serves an important purpose. In the \"Info\" tab, you’ll see a button labeled \"Add/Remove Road.\" Click this button, then click the target building (the farm). If the window is blocking the farm, you can drag it elsewhere.\n\nConnect a road from the Water Pump to the Farm.",
                new Predicate<City>() {
                    /**
                     * Connect a road from the Water Pump to the Farm.
                     */
                    @Override
                    public boolean test(City city) {
                        for (Building building : city.getAllBuildings()) {
                            if(building.isOfType("base.WATER_PUMP") && building.hasRoadTo("base.FARM"))
                            {
                                return true;
                            }
                        }
                        return false;
                    }
                },
                () -> {
                    city.unlockBuilding(BuildingLoader.getByKey("base.COAL_POWER_PLANT"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.MINE"));
                }/// Unlock MINE and COAL_POWER_PLANT
            )
        );
        quests.add(
            new Quest(city,
                "The farm is ready to produce food once you get people, but you still need to have a steady supply of electricity for your city. Right now, your only option is to use coal power, but you'll unlock more ways later. You need to get coal from the mine and transport it to the coal power plant.\nTo maximize efficiency, you should click on the \"Recipes\" tab in the mine, and switch it to \"Prioritize Coal.\" With this recipe, you produce more coal but less of other resources.\n\nConnect a mine to a coal power plant.",
                new Predicate<City>() {
                    /**
                     * Connect a mine to a coal power plant.
                     */
                    @Override
                    public boolean test(City city) {
                        for (Building building : city.getAllBuildings()) {
                            if(building.isOfType("base.MINE") && building.hasRoadTo("base.COAL_POWER_PLANT"))
                            {
                                return true;
                            }
                        }
                        return false;
                    }
                })
        );
        quests.add(
            new Quest(city,
                "Your city is almost ready to move its first people in! All that's left is prioritizing all of these buildings. You can find it in the \"Info\" tab. Prioritizing a building makes workers work there first. Workers are distributed in order of when the building was placed, but by prioritizing a building you can force workers to work there first. Notably, workers do not need roads. They use an interconnected underground tunnel system, so that the roads are open for transporting resources.\n\nPrioritize the water pump, the farm, the mine, and the coal power plant.",
                new Predicate<City>() {
                    /**
                     * Prioritize the water pump, the farm, the mine, and the coal power plant.
                     */
                    @Override
                    public boolean test(City city) {
                        boolean hasWaterPump = false;
                        boolean hasFarm = false;
                        boolean hasMine = false;
                        boolean hasCoalPlant = false;

                        for (Building building : city.getAllBuildings()) {
                            if (!building.isPrioritized()) continue;

                            if (building.isOfType("base.WATER_PUMP")){
                                hasWaterPump = true;
                            }
                            if (building.isOfType("base.FARM")){
                                hasFarm = true;
                            }
                            if (building.isOfType("base.MINE")){
                                hasMine = true;
                            }
                            if (building.isOfType("base.COAL_POWER_PLANT")){
                                hasCoalPlant = true;
                            }

                            if (hasWaterPump && hasFarm && hasMine && hasCoalPlant) return true;
                        }

                        return false;
                    }
                },
                () -> city.unlockBuilding(BuildingLoader.getByKey("base.HOUSE"))
            )
        );
        //</editor-fold>
        //<editor-fold desc="Game Basics">
        quests.add(
            new Quest(city,
                "You finally have everything you need for a self-sustaining population! Once you place a house, you'll receive 10 seconds of complimentary resources, which gives you time to connect roads to supply the house with resources. If you run out of time, the people will move out. Don't worry, just delete the house and try again.\nIt is recommended that you connect the water pump to the house first, and then connect the farm to the house. On computer, you can right-click a building to begin adding a road (and then left-click the target), which serves as a shortcut to pressing the add road button. Whenever you're ready, start your city!\n\nMaintain a population of 6 or more for 15 seconds.",
                new Predicate<City>() {
                    long timeAchieved = -1L;

                    /**
                     * Maintain a population of 6 or more for 15 seconds.
                     */
                    @Override
                    public boolean test(City city) {
                        if (city.getWorkers() >= 6 && timeAchieved == -1L) {
                            timeAchieved = TimeUtils.millis();
                        }
                        if (city.getWorkers() < 6 && timeAchieved != -1L) {
                            timeAchieved = -1L;
                        }
                        return timeAchieved != -1L && TimeUtils.timeSinceMillis(timeAchieved) >= 15000;
                    }
                },
                () -> {
                    for (BuildingDefinition def : BuildingLoader.getAll()) {
                        city.lockBuilding(def);
                    }
                    for (Building building : city.getAllBuildings()) {
                        building.setDeletable(false);
                    }
                }
            )
        );
        quests.add(
            new Quest(city,
                "Your town can support itself, but it is not ready to produce extra resources yet. One of the most important basic functions of the game is the \"Rates\" tab. It provides information on how much of each resource is consumed/produced, assuming all inputs are satisfied. This information can be used to calculate how many of each building you'll need in order to get the desired amount of product. Rather than placing new buildings, you can scale up most buildings in the \"Recipes\" tab, which is equivalent to placing more buildings. If you don't want to calculate, you can also use the \"Resources\" tab to see if your resources are increasing overall.\n\nUsing the scale tool, optimize the scale of your buildings to achieve a population of 42 and have 750 electricity.",
                new Predicate<City>() {
                    final Predicate<City> populationCheck = makeDurationCondition(city -> city.getWorkers() >= 42,5000);
                    /**
                     * Using the scale tool, optimize the scale of your buildings to achieve a population of 42 and have 750 electricity.
                     */
                    @Override
                    public boolean test(City city) {
                        return city.getElectricity()>=750f && populationCheck.test(city);
                    }
                },
                () -> {
                    city.unlockBuilding(BuildingLoader.getByKey("base.WATER_PUMP"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.FARM"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.MINE"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.COAL_POWER_PLANT"));
                    city.unlockBuilding(BuildingLoader.getByKey("base.HOUSE"));

                    for (Building building : city.getAllBuildings()) {
                        building.setDeletable(true);
                    }

                    city.unlockBuilding(BuildingLoader.getByKey("base.DEPOT"));
                }/// Unlock DEPOT
            )
        );
        quests.add(
            new Quest(city,
                "Great! Your city is now ready to produce extra supplies! You’ve also unlocked the Depot building. Depots allow you to access your city storage. Every depot shares the same inventory. If you add a road going to a depot, resources are sent to the city storage. If you add a road from a depot, resources will be taken out of the city storage.\nA neighboring city needs water. Deliver 20 units of water to the city storage so it can be sent to them. In return, they promise to teach you about an important building.\n\nDeliver 20 units of water to the city storage.",
                new Predicate<City>() {
                    /**
                     * Deliver 20 units of water to the city storage.
                     */
                    @Override
                    public boolean test(City city) {
                        return city.getResourceAmount(ResourceLoader.getByKey("base.WATER")) >= 20f;
                    }
                },
                () -> {
                    city.addResourceAmount(ResourceLoader.getByKey("base.WATER"), -20f);
                    city.unlockBuilding(BuildingLoader.getByKey("base.NODE"));
                }/// Unlock NODE
            )
        );
        quests.add(
            new Quest(city,
                "Since you solved the nearby city's water crisis, they agree to teach you the tricks of the Node building. The node building doesn't produce or consume anything, but it allows you to control its input and output rate, as well as which resources are allowed in or out. It may not seem useful at first, but it allows for greater control over the flow of resources in your city. To demonstrate this, the neighboring city has challenged you to deliver ~0.4u/s of rock from your mine to the city storage. In the \"Rates\" tab of a node, you can control its input/output rate. In the \"Configure\" tab, you can choose which resources to input and output.\n\nDeliver between 0.35u/s and 0.45u/s of Rock to the city storage for 5 seconds.",
                makeDurationCondition(city -> {
                    float changeRate = city.getResourceChangeRate(ResourceLoader.getByKey("base.ROCK"));
                    return changeRate>=0.35f&&changeRate<=0.45f;
                }, 5000),
                () -> city.unlockBuilding(BuildingLoader.getByKey("base.FACTORY"))
            )
        );
        //</editor-fold>
        //</editor-fold>
        this.city.declareAsStory();
        city.screen.showDialog(getActiveQuest().getQuestInfo(), 400f);
    }

    /**
     * Checks the status of the current quest and displays the next quest dialog if the current quest is won.
     */
    public void update() {
        if(!cityActive) return;
        if(getActiveQuest().check()) {
            currentQuest++; //incremented instantly so it stops showing up
            city.screen.showDialog(getActiveQuest().getQuestInfo(), 400f);//this is the next quest after the check
        }
    }

    /**
     * Gets the active quest.
     * @return The active quest.
     */
    public Quest getActiveQuest() {
        if(currentQuest<quests.size) {
            return quests.get(currentQuest);
        }
        return endQuest;
    }

    /**
     * For quests that could be cheesed easily, like if you only had to reach a population for 1 frame, this fixes that. Now, the condition has to be true for the whole time period.
     *
     * @param condition The condition that must be true for a certain amount of time.
     * @param millis The amount of time the condition must be true for.
     * @return the predicate that checks the range of time
     */
    private Predicate<City> makeDurationCondition(Predicate<City> condition, int millis) {
        return new Predicate<City>() {
            long timeAchieved = -1L;
            @Override
            public boolean test(City city) {
                boolean withinRange = condition.test(city);
                if (withinRange && timeAchieved == -1L) {
                    timeAchieved = TimeUtils.millis();
                }
                if (!withinRange && timeAchieved != -1L) {
                    timeAchieved = -1L;
                }
                return timeAchieved != -1L && TimeUtils.timeSinceMillis(timeAchieved) >= millis;
            }
        };
    }
}
