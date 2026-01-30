package com.catcat.hypercity.display.city;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.definitions.campaign.WinningCondition;
import com.catcat.hypercity.display.resources.ResourceIcon;
import com.catcat.hypercity.display.ui.CategoryTree;
import com.catcat.hypercity.display.workers.CityWorkerDisplay;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.catcat.hypercity.display.resources.ResourceDisplay;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.transport.Road;
import com.catcat.hypercity.definitions.campaign.LevelData;
import com.catcat.hypercity.display.ui.AutoResizeWindow;
import com.catcat.hypercity.display.electricity.ElectricityDisplay;
import com.catcat.hypercity.loaders.ResourceLoader;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class CityScreen implements Screen {
    public final Stage stage;
    public final Stage uiStage;

    //<editor-fold desc="Private Instance Variables">
    private final ScreenViewport viewport;
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final CategoryTree resourceDisplayTable;
    private Label timeLabel;
    private Label windLabel;
    private boolean paused;
    private Window pauseWindow;
    private final Window buildingPlaceWindow;
    private boolean placingBuilding;
    private boolean needShiftToPlaceBuilding;
    private final City city;
    private final TiledBackground bg;

    //</editor-fold>
    public CityScreen(City city) {
        // TODO: 11/4/25 tutorial windows telling you what to do. it would be a flag in the constructor and mainMenuScreen can have a createdCity flag for the first city so it would make a tutorial.
        this.city = city;
        viewport = new ScreenViewport();
        stage = new Stage(viewport, city.game.batch) {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                super.touchDown(x, y, pointer, button);
                Vector2 stageCoordinates = stage.screenToStageCoordinates(new Vector2(x, y));
                return stage.hit(stageCoordinates.x, stageCoordinates.y, true) != null; //if we don't touch a building or a building window it should be false and carry to the gesture listener
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                super.scrolled(amountX, amountY);
                OrthographicCamera cam = (OrthographicCamera)stage.getCamera();
                float zoomSpeed = 0.05f;
                cam.zoom += amountY * zoomSpeed * cam.zoom;
                float minZoom = 0.33f;
                float maxZoom = 3f;
                cam.zoom = MathUtils.clamp(cam.zoom, minZoom, maxZoom);
                cam.update();
                return false;
            }
        };
        bg = new TiledBackground(city.game.assets.get("bg.png", Texture.class), stage.getViewport().getCamera());

        OrthographicCamera cam = (OrthographicCamera)stage.getCamera();
        cam.position.set(0,0,0);
        uiStage = new Stage(new ScreenViewport(), city.game.batch) {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                super.touchDown(screenX, screenY, pointer, button);
                city.game.click.play();
                Vector2 stageCoordinates = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
                if (uiStage.hit(stageCoordinates.x, stageCoordinates.y, true) == null) {
                    uiStage.unfocusAll();
                }
                return uiStage.hit(stageCoordinates.x, stageCoordinates.y, true) != null; // #1, if ui is hit
            }
        };
        this.resourceDisplayTable = new CategoryTree(city.game.skin);
        for (ResourceDefinition resource : ResourceLoader.getAll()) {
            resourceDisplayTable.addItem(resource.category, new ResourceDisplay(resource, city.getCityInventory(), city));
        }

        Window resourceWindow = getResourceWindow();
        uiStage.addActor(resourceWindow);
        if (city.isCampaign()) {
            Window campaignWindow = getCampaignWindow();
            uiStage.addActor(campaignWindow);
        }
        buildingPlaceWindow = new AutoResizeWindow("0x01 Place Buildings", city.game.skin, "default");
        buildBuildingPlaceWindow();
        buildingPlaceWindow.setPosition(16000f, 0f);//to the right a lil
        uiStage.addActor(buildingPlaceWindow);
        Window cityInfoWindow = getCityInfoWindow();
        uiStage.addActor(cityInfoWindow);
    }

    //<editor-fold desc="UI Elements">
    //I just felt like it was taking up too much space in there before
    private Window getResourceWindow() {
        Window resourceWindow = new AutoResizeWindow("0x00 City Resources", city.game.skin, "default");
        //is now initialized with all values with "new ResourceDisplayTable(new Array<>(CityResource.values()), game)" in the constructor

        resourceWindow.add(wrapScrollableContent(resourceDisplayTable, 250f));
        resourceWindow.setPosition(0, 9000);
        resourceWindow.pack();
        resourceWindow.setSize(200f, resourceWindow.getHeight());
        return resourceWindow;
    }

    private Window getCampaignWindow() {
        Window window = new AutoResizeWindow("0x03 Campaign", city.game.skin, "default");
        Table table = new Table();
        for (WinningCondition winningCondition : new Array.ArrayIterable<>(city.getLevelData().getWinningConditions())) {
            for (WinningCondition.ConditionType type : winningCondition.getConditionTypes()) {
                String labelText = "";

                switch (type) {
                    case POPULATION:
                        table.add(new Image((Texture)city.game.assets.get("worker.png")));
                        labelText = "Reach a population of " + winningCondition.getTargetPopulation();
                        break;
                    case RESOURCE_AMOUNT:
                        table.add(new ResourceIcon(winningCondition.getTargetResource(), city));
                        labelText = "Obtain " +
                            winningCondition.getTargetAmount() + " " +
                            winningCondition.getTargetResource().name;
                        break;
                    case RESOURCE_RATE:
                        table.add(new ResourceIcon(winningCondition.getTargetResource(), city));
                        labelText = "Reach a " +
                            winningCondition.getTargetResource().name + " production rate of " +
                            winningCondition.getRequiredRate() + "/s";
                        break;
                }
                table.add(new Label(labelText, city.game.skin)).pad(5).left().row();
            }
        }
        window.add(wrapScrollableContent(table, 100f)).row();
        TextButton showStory = new TextButton("Show Dialog", city.game.skin);
        showStory.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                displayStartDialog(city.getLevelData());
            }
        });
        window.add(showStory).width(showStory.getWidth()+8f);
        window.setPosition(16000, 9000);
        return window;
    }
    private Window getStoryWindow() {
        Label startLabel = new Label(city.game.storyManager.getActiveQuest().getQuestInfo(), city.game.skin);
        startLabel.setWrap(true);
        Window window = new AutoResizeWindow("0x03 Story", city.game.skin, "default"){
            @Override
            public void act(float delta) {
                startLabel.setText(city.game.storyManager.getActiveQuest().getQuestInfo());
                super.act(delta);
            }
        };
        window.add(startLabel).width(250f).center().row();
        window.setPosition(16000, 9000);
        return window;
    }
     public void buildBuildingPlaceWindow() {
        buildingPlaceWindow.clearChildren();
        CategoryTree categoryTable = new CategoryTree(city.game.skin);

        for (BuildingDefinition def : BuildingLoader.getAll()) {
            //skip banned buildings
            if (city.isCampaign() && city.getLevelData().getBannedBuildings().contains(def.key))
                continue;
            //skip locked buildings
            if (city.isStory()&&!city.isBuildingUnlocked(def))
                continue;

            //icon and button combo meal
            Table buildingRow = new Table();
            buildingRow.add(new Image(city.game.assets.get(def.texturePath, Texture.class))).size(32, 32).padRight(5).left();
            TextButton button = getAddBuildingButton(def, buildingPlaceWindow);
            buildingRow.add(button).right().width(150f).height(36f);


            categoryTable.addItem(def.category, buildingRow);
        }

        buildingPlaceWindow.add(wrapScrollableContent(categoryTable, 300f));
        buildingPlaceWindow.pack();
    }

    private TextButton getAddBuildingButton(BuildingDefinition def, Window window) {
        TextButton button = new TextButton("Place " + def.name, city.game.skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                placingBuilding = true;
                button.setText("Click Anywhere");
                window.setTouchable(Touchable.disabled);

                EventListener stageListener = new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float stageX, float stageY) {
                        if (!placingBuilding) return;

                        if (!needShiftToPlaceBuilding || Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                            if (city.canPlaceBuilding(stageX, stageY)) {
                                Building toAdd = new Building(def.key, stageX, stageY, city);
                                city.addBuilding(toAdd);
                                needShiftToPlaceBuilding = true;
                                button.setText("Place " + def.name);
                            } else {
                                button.setText("Too Close!");
                                //cool feature chatgpt showed me, may use elsewhere
                                Timer.schedule(new Timer.Task() {
                                    @Override
                                    public void run() {
                                        button.setText("Place " + def.name);
                                    }
                                }, 2f); // revert after 2 seconds
                            }
                        }

                        if (!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                            placingBuilding = false;
                            needShiftToPlaceBuilding = false;

                            // remove temporary stage listener
                            stage.removeListener(this);
                            window.setTouchable(Touchable.enabled);
                        }
                    }
                };
                stage.addListener(stageListener);

            }
        });
        return button;
    }

    private Window getCityInfoWindow() {
        Window window = new AutoResizeWindow("0x02 City Info", city.game.skin, "default");
        timeLabel = new Label("", city.game.skin);
        Widget minSizeWidget = new Widget(){
            @Override
            public float getMinWidth() {
                return 200f;
            }
        };
        window.add(minSizeWidget).row();
        window.add(timeLabel).row();
        windLabel = new Label("", city.game.skin);
        window.add(windLabel).row();
        window.add(new ElectricityDisplay(city.game.skin, city)).row();
        window.add(new CityWorkerDisplay(city, city.game.skin)).row();
        window.add(new Minimap(city)).size(150, 150).pad(5).row();
        TextButton pauseButton = new TextButton("Pause/Quit", city.game.skin);
        pauseButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                pause();
            }
        });
        window.add(pauseButton).width(120).height(25);

        window.setPosition(0f, 0f);
        window.pack();
        window.setSize(240f, window.getHeight());
        return window;
    }

    private void showPauseButton() {
        pauseWindow = new AutoResizeWindow("Paused", city.game.skin, "dialog");
        pauseWindow.add(new Label("The game is paused.", city.game.skin)).row();
        TextButton mainMenuButton = new TextButton("Main Menu", city.game.skin);
        TextButton resumeButton = new TextButton("Resume", city.game.skin);
        pauseWindow.add(mainMenuButton).width(mainMenuButton.getWidth()+8f);
        pauseWindow.add(resumeButton).width(resumeButton.getWidth()+8f);
        pauseWindow.setPosition(MathUtils.roundPositive(uiStage.getCamera().position.x - pauseWindow.getWidth() / 2f), MathUtils.roundPositive(uiStage.getCamera().position.y - pauseWindow.getHeight() / 2f));
        pauseWindow.pack();
        uiStage.addActor(pauseWindow);
        mainMenuButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                city.game.setScreen(city.game.mainMenu);
                pauseWindow.remove();
                paused = false;
            }
        });
        resumeButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                CityScreen.this.resume();
            }
        });
    }

    private void displayStartDialog(LevelData levelData) {
        Window startWindow = new AutoResizeWindow("Welcome", city.game.skin, "dialog");
        Label startLabel = new Label(levelData.getStory(), city.game.skin);
        startLabel.setWrap(true);
        startWindow.add(startLabel).width(250f).center().row();
        TextButton okButton = new TextButton("Ok", city.game.skin);
        startWindow.add(okButton).width(okButton.getWidth()+8f);
        startWindow.setPosition(MathUtils.roundPositive(uiStage.getCamera().position.x - startWindow.getWidth() / 2f), MathUtils.roundPositive(uiStage.getCamera().position.y - startWindow.getHeight() / 2f));
        startWindow.pack();
        uiStage.addActor(startWindow);
        okButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                startWindow.remove();
            }
        });
    }

    public void displayWinDialog() {
        Window winWindow = new AutoResizeWindow("You Won", city.game.skin, "dialog");
        winWindow.add(new Label("You Won! Congrats.", city.game.skin)).row();
        TextButton mainMenuButton = new TextButton("Main Menu", city.game.skin);
        TextButton nextLevelButton = new TextButton("Next Level", city.game.skin);
        winWindow.add(mainMenuButton).width(mainMenuButton.getWidth()+8f);
        winWindow.add(nextLevelButton).width(nextLevelButton.getWidth()+8f);
        winWindow.setPosition(MathUtils.roundPositive(uiStage.getCamera().position.x - winWindow.getWidth() / 2f), MathUtils.roundPositive(uiStage.getCamera().position.y - winWindow.getHeight() / 2f));
        winWindow.pack();
        uiStage.addActor(winWindow);
        mainMenuButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                city.game.setScreen(city.game.mainMenu);
                winWindow.remove();
            }
        });
        nextLevelButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                City city = new City(CityScreen.this.city.game, CityScreen.this.city.game.campaignManager.getNextLevel());
                city.game.addCity(city);
                city.game.setScreen(city.screen);
            }
        });
    }

    private Table wrapScrollableContent(Actor content, float maxHeight) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // allow vertical scrolling
        scrollPane.setForceScroll(false, true);
        Table wrapper = new Table();
        wrapper.add(scrollPane)
            .growX()
            .minHeight(Math.min(maxHeight, scrollPane.getPrefHeight()))
            .maxHeight(maxHeight);
        return wrapper;
    }

//</editor-fold>

    //<editor-fold desc="Screen Overrides">
    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer(); //need this because 2 stages
        Gdx.input.setInputProcessor(multiplexer);
        GestureDetector gestureDetector = getGestureDetector();
        // Add gesture detector last so it gets input after UI but before stage click listeners

        multiplexer.addProcessor(uiStage); // ui should get input first
        multiplexer.addProcessor(stage);   // world input second
        multiplexer.addProcessor(gestureDetector);

        if (city.isCampaign()) {
            displayStartDialog(city.getLevelData());
        }
        if (city.isStory())
        {
            city.game.storyManager.cityActive = true;
        }
        city.game.cityMusic.play();
    }

    private GestureDetector getGestureDetector() {
        OrthographicCamera cam = (OrthographicCamera)stage.getCamera();
        return new GestureDetector(new GestureDetector.GestureAdapter() {
            float initialZoom = cam.zoom;

            //I think I fixed it, forgot that ClickListener is left only by default so handling was different (it fixed when I updated index.html rah aaa)
            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                Vector2 uiStageCoordinates = uiStage.screenToStageCoordinates(new Vector2(x, y));
                Vector2 stageCoordinates = stage.screenToStageCoordinates(new Vector2(x, y));
                if (uiStage.hit(uiStageCoordinates.x, uiStageCoordinates.y, true) != null)
                    return false;
                if (stage.hit(stageCoordinates.x, stageCoordinates.y, true) != null) return false;
                // Move opposite to finger drag for natural feel
                cam.translate(-deltaX * cam.zoom, deltaY * cam.zoom);
                return true;
            }

            @Override
            public boolean zoom(float initialDistance, float distance) {//called on mobile, desktop scrolling handled in constructor
                // Only zoom if neither finger is over UI
                int pointer1X = Gdx.input.getX(0);
                int pointer1Y = Gdx.input.getY(0);
                int pointer2X = Gdx.input.getX(1);
                int pointer2Y = Gdx.input.getY(1);
                Vector2 stageCoordinates1 = uiStage.screenToStageCoordinates(new Vector2(pointer1X, pointer1Y));
                Vector2 stageCoordinates2 = uiStage.screenToStageCoordinates(new Vector2(pointer2X, pointer2Y));
                if (uiStage.hit(stageCoordinates1.x, stageCoordinates1.y, true) != null || uiStage.hit(stageCoordinates2.x, stageCoordinates2.y, true) != null)
                    return false;

                float ratio = initialDistance / distance;
                cam.zoom = initialZoom * ratio;
                float minZoom = 0.33f;
                float maxZoom = 3f;
                cam.zoom = MathUtils.clamp(cam.zoom, minZoom, maxZoom);
                return true;
            }

            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                // Ignore UI touches for initialZoom
                Vector2 stageCoordinates = uiStage.screenToStageCoordinates(new Vector2(x, y));
                if (uiStage.hit(stageCoordinates.x, stageCoordinates.y, true) != null) return false;
                initialZoom = cam.zoom;
                return true;
            }

            @Override
            public boolean panStop(float x, float y, int pointer, int button) {
                initialZoom = cam.zoom;
                return true;
            }

            @Override
            public void pinchStop() {
                initialZoom = cam.zoom;
            }
        });
    }

    @Override
    public void render(float delta) {
        delta = MathUtils.clamp(delta, 0.001f, 1f); //1ms-1s so nothing chaotic happens when the delta is high (only place I saw this was when the window was inactive
        // organize code into three methods
        input(delta);
        if (!paused) {
            city.logic(delta);
        }
        draw(delta);
    }

    @Override
    public void dispose() {
        stage.dispose();
        uiStage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, false); // true centers the camera
        uiStage.getViewport().update(width, height, true); // For the UI on the screen
    }

    @Override
    public void pause() {
        paused = true;
        showPauseButton();
    }

    @Override
    public void resume() {
        pauseWindow.remove();
        paused = false;
    }

    @Override
    public void hide() {
        if (city.isStory())
        {
            city.game.storyManager.cityActive = false;
        }
        city.game.cityMusic.stop();
    }
    //</editor-fold>

    //<editor-fold desc="Update Methods">
    private void input(float delta) {

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (!paused) {
                pause();
            } else {
                resume();
            }
        }

        OrthographicCamera cam = (OrthographicCamera)viewport.getCamera();
        float moveSpeed = 400f * delta * cam.zoom; // base speed adjusted by delta and zoom
        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= moveSpeed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += moveSpeed;

        //normalize so diagonal is same speed if we are changing both
        if (dx != 0 && dy != 0) {
            dx /= (float)Math.sqrt(2);
            dy /= (float)Math.sqrt(2);
        }

        cam.position.add(dx, dy, 0);
        cam.update();

    }

    private void draw(float delta) {
        setTimeLabel();
        windLabel.setText("Wind Speed: " + Math.round(city.getWindSpeed() * 10f) / 10f + "m/s");
        float hours = (float)(city.getGameTime() / 3600f) % 24f;//I can deal with colors later
        if (hours <= 6f || hours >= 19f) {
            ScreenUtils.clear(0.15f, 0.15f, 0.30f, 1f);
        } else if (hours <= 7f) {
            ScreenUtils.clear(1f, 0.5f, 0.4f, 1f);
        } else if (hours <= 18f) {
            ScreenUtils.clear(0.5f, 0.7f, 1f, 1f);
        } else if (hours <= 19f) {
            ScreenUtils.clear(0.55f, 0.3f, 0.0f, 1f);
        } else throw new RuntimeException("time was not in bounds");

        city.game.batch.setProjectionMatrix(stage.getCamera().combined);
        city.game.batch.begin();
        drawBackground();
        city.game.batch.end();
        drawShapes();

        stage.act(delta); // updates stage (animations, input, etc.)
        stage.draw();

        uiStage.act(delta);
        uiStage.draw();
    }

    private void drawBackground() {
        bg.draw(city.game.batch, 1);
    }

    private void drawShapes() {
        //draw line to window
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Building building : city.getAllBuildings()) {
            building.drawWindowLine();

            for (Road road : new Array.ArrayIterable<>(building.roads)) {
                Building linked = road.getTargetBuilding();
                if (linked == building) continue;
                float x1 = building.getX();
                float y1 = building.getY();
                float x2 = linked.getX();
                float y2 = linked.getY();

                // Draw the line
                shapeRenderer.rectLine(x1, y1, x2, y2, 3);

                //code for putting an arrow pointing the right way is not mine, chatGPT made this one

                // Angle toward target
                float angle = (float)Math.atan2(y2 - y1, x2 - x1);

                // Draw a simple arrow at midpoint
                float arrowLength = 20f;
                float arrowAngle = (float)Math.toRadians(30);

                //now find the midpoint
                float midX = (x1 + x2 + arrowLength * (float)(Math.cos(arrowAngle) * Math.cos(angle))) / 2f;
                float midY = (y1 + y2 + arrowLength * (float)(Math.cos(arrowAngle) * Math.sin(angle))) / 2f;

                // Left wing
                shapeRenderer.rectLine(
                    midX, midY,
                    midX - arrowLength * (float)Math.cos(angle - arrowAngle),
                    midY - arrowLength * (float)Math.sin(angle - arrowAngle), 1);

                // Right wing
                shapeRenderer.rectLine(
                    midX, midY,
                    midX - arrowLength * (float)Math.cos(angle + arrowAngle),
                    midY - arrowLength * (float)Math.sin(angle + arrowAngle), 1);
            }
        }
        shapeRenderer.end();
    }

    @SuppressWarnings("OverlyComplexMethod")
    private void setTimeLabel() {//used rather than java.time.Instant because google
        long totalSeconds = (long)city.getGameTime();

        // Base date: 2050-01-01 00:00:00 UTC
        int year = 2050;
        int month = 1;
        int day = 1;

        // Split into days and remainder seconds
        long days = totalSeconds / 86400;
        long secondsOfDay = totalSeconds % 86400;
        // --- Calculate year ---
        while (true) {
            int daysInYear = ((year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 366 : 365);
            if (days < daysInYear) break;
            days -= daysInYear;
            year++;
        }

        // --- Calculate month ---
        int[] daysInMonth = {31, (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int i = 0; i < 12; i++) {
            if (days < daysInMonth[i]) {
                month = i + 1;
                day += (int)days;
                break;
            }
            days -= daysInMonth[i];
        }

        // --- Calculate time of day ---
        int hour = (int)(secondsOfDay / 3600);
        int minute = (int)((secondsOfDay % 3600) / 60);
        int second = (int)(secondsOfDay % 60);

        // --- Build string manually (faster than String.format) ---
        StringBuilder sb = new StringBuilder(25);
        sb.append("Time: ")
            .append(year).append('-');
        if (month < 10) sb.append('0');
        sb.append(month).append('-');
        if (day < 10) sb.append('0');
        sb.append(day).append('T');
        if (hour < 10) sb.append('0');
        sb.append(hour).append(':');
        if (minute < 10) sb.append('0');
        sb.append(minute).append(':');
        if (second < 10) sb.append('0');
        sb.append(second).append('Z');

        timeLabel.setText(sb.toString());
    }

    public void drawLine(float x, float y, float endX, float endY) {
        shapeRenderer.line(x, y, endX, endY);
    }
    //</editor-fold>

    public void showDialog(String text, float width){
        showDialog("Info", "Close", text, width);
    }
    private void showDialog(String title, String button, String text, float width) {
        Window window = new AutoResizeWindow(title, city.game.skin, "dialog");
        Label label = new Label(text, city.game.skin);
        label.setWrap(true);
        window.add(label).width(width).center().row();
        TextButton okButton = new TextButton(button, city.game.skin);
        window.add(okButton).width(okButton.getWidth()+8f);
        window.setPosition(MathUtils.roundPositive(uiStage.getCamera().position.x - window.getWidth() / 2f), MathUtils.roundPositive(uiStage.getCamera().position.y - window.getHeight() / 2f));
        window.pack();
        uiStage.addActor(window);
        okButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                window.remove();
            }
        });
    }
    public void addStoryWindow() {
        Window storyWindow = getStoryWindow();
        uiStage.addActor(storyWindow);
    }
}
