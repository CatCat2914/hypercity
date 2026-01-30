package com.catcat.hypercity.display.building;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.definitions.recipe.Recipe;
import com.catcat.hypercity.display.ui.CategoryTree;
import com.catcat.hypercity.display.ui.HelpTab;
import com.catcat.hypercity.display.resources.ResourceDisplay;
import com.catcat.hypercity.display.ui.TabbedWindow;
import com.catcat.hypercity.display.ui.TabbedWindow.Tab;
import com.catcat.hypercity.CityGame;
import com.catcat.hypercity.building.behavior.recipe.RecipeBuildingBehavior;
import com.catcat.hypercity.display.ui.AutoResizeWindow;
import com.catcat.hypercity.display.workers.BuildingWorkerDisplay;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
public class BuildingWindow {
    // TODO: 11/9/25 add a red dot like thing when population isn't full or any input is 0
    //<editor-fold desc="Private Instance Variables">
    private TabbedWindow window;
    private final Building building;
    private final CityGame game;
    private final City city;
    private final CategoryTree resourceDisplayTable;
    private final ObjectMap<ResourceDefinition, ResourceDisplay> resourceDisplays = new ObjectMap<>();
    private final Label scaleLabel;
    private boolean canDelete = true;

    //</editor-fold>
    public BuildingWindow(Building building) {
        this.building = building;
        this.city = building.city;
        this.game = building.city.game;
        this.resourceDisplayTable = new CategoryTree(game.skin);
        createWindow(city.screen.stage);
        scaleLabel = getScaleLabel(building);
        city.screen.stage.addActor(scaleLabel);
    }

    //<editor-fold desc="UI Creation">

    /**
     * creates a window which will have options such as move as well as stats and resources
     */
    private void createWindow(Stage stage) {
        window = new TabbedWindow("0x" + Integer.toHexString(building.getId()) + " (" + building.getId() + ") " + building.behavior.getDefinition().name, game.skin, "default", false);
        window.setPosition(building.getX() + 30, building.getY() + 30);
        window.setVisible(false);
        window.setKeepWithinStage(false);
        window.addTab(getInfoTab());
        window.addTab(getResourceTab());
        addCustomTabs();
        window.pack();
        stage.addActor(window);
    }

    //<editor-fold desc="Info Tab">
    private Tab getInfoTab() {
        Table content = new Table();
        Label positionLabel = new Label("", game.skin){
            @Override
            public void act(float delta) {
                this.setText("x: " + Math.round(building.getX()) + " y: " + Math.round(building.getY()));
                super.act(delta);
            }
        };
        content.add(positionLabel).row();
        if (building.behavior instanceof RecipeBuildingBehavior) {
            BuildingWorkerDisplay workerDisplay = new BuildingWorkerDisplay(building, game.skin, city);
            content.add(workerDisplay).row();
        }
        TextButton changePositionButton = getChangePositionButton(city.screen.stage);
        content.add(changePositionButton).width(changePositionButton.getWidth()+8f).row();
        TextButton addRoadButton = getAddRoadButton();
        content.add(addRoadButton).width(addRoadButton.getWidth()+8f).row();
        if (building.behavior instanceof RecipeBuildingBehavior) {
            boolean consumesWorkers = false;
            for (Recipe r : new Array.ArrayIterator<>(((RecipeBuildingBehavior)building.behavior).getRecipes())) {
                if (r.getWorkers() > 0) {
                    consumesWorkers = true;
                    break;
                }
            }
            if (consumesWorkers) {
                TextButton priorityButton = makePriorityButton();
                content.add(priorityButton).width(priorityButton.getWidth()+8f).row();
            }
        }
        TextButton deleteButton = getDeleteButton();
        content.add(deleteButton).width(deleteButton.getWidth()+8f).row();
        return new HelpTab(window, "Info", content, building.behavior.getHelpTab(building, "Info"), new TextButton("Info", game.skin, "toggle"), game);
    }

    private TextButton getChangePositionButton(Stage stage) {
        TextButton changePositionButton = new TextButton("Change Position", game.skin);
        changePositionButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                changePositionButton.setText("Click Anywhere");
                stage.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float stageX, float stageY) {
                        building.updatePosition(stageX, stageY);
                        changePositionButton.setText("Change Position");
                        stage.removeListener(this); // remove temporary listener
                    }
                });
            }
        });
        return changePositionButton;
    }

    private TextButton getAddRoadButton() {
        TextButton roadButton = new TextButton("Add/Remove Road", game.skin){
            @Override
            public void act(float delta) {
                if (city.isMakingRoad()) {
                    this.setText("Click a Building");
                } else {
                    this.setText("Add/Remove Road");
                }
                super.act(delta);
            }
        };
        roadButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                city.toggleMakingRoad(building);
            }
        });
        return roadButton;
    }

    private TextButton makePriorityButton() {
        TextButton priorityButton = new TextButton("Prioritize Working Here", game.skin, "toggle"){
            @Override
            public void act(float delta) {
                this.setChecked(building.isPrioritized());
                super.act(delta);
            }
        };
        priorityButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                building.setPrioritized(!building.isPrioritized());
            }
        });
        return priorityButton;
    }

    private TextButton getDeleteButton() {
        TextButton deleteButton = new TextButton("Delete", game.skin){
            @Override
            public void act(float delta) {
                super.act(delta);
                this.setDisabled(!canDelete);
                this.setTouchable(canDelete?Touchable.enabled:Touchable.disabled);
            }
        };
        deleteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Window window1 = new AutoResizeWindow("Delete Building?", game.skin, "dialog");
                window1.add(new Label(" This action \n cannot be undone. ", game.skin)).row();
                TextButton confirmButton = new TextButton("Delete", game.skin);
                TextButton noButton = new TextButton("Keep", game.skin);
                window1.add(confirmButton).width(confirmButton.getWidth()+8f);
                window1.add(noButton).width(noButton.getWidth()+8f);
                window1.setPosition(MathUtils.roundPositive(city.screen.uiStage.getCamera().position.x - window1.getWidth() / 2f), MathUtils.roundPositive(city.screen.uiStage.getCamera().position.y - window1.getHeight() / 2f));
                window1.pack();
                city.screen.uiStage.addActor(window1);
                confirmButton.addListener(new ClickListener() {
                    public void clicked(InputEvent event, float x, float y) {
                        // Remove window and image from stage
                        window.remove();
                        building.image.remove();
                        scaleLabel.remove();
                        city.removeBuilding(building);
                        window1.remove();
                    }
                });
                noButton.addListener(new ClickListener() {
                    public void clicked(InputEvent event, float x, float y) {
                        window1.remove();
                    }
                });
            }
        });
        deleteButton.getLabel().setColor(Color.RED);
        return deleteButton;
    }

    //</editor-fold>
    //<editor-fold desc="Resource Tab">
    private Tab getResourceTab() {
        Table content = new Table();
        content.add(new Label("Stored: ", game.skin)).row();
        constructResourceDisplayTable();
        content.add(resourceDisplayTable).row();
        return new HelpTab(window, "Resources", content, building.behavior.getHelpTab(building, "Resources"), new TextButton("Resources", game.skin, "toggle"), game);
    }

    private void constructResourceDisplayTable() {
        for (ResourceDefinition resource : new ObjectMap.Keys<>(building.behavior.getResourceInputs())) {
            if (!building.behavior.getResourceOutputs().containsKey(resource)) {
                addResourceToResourceTable(resource);
            }
        }
        for (ResourceDefinition resource : new ObjectMap.Keys<>(building.behavior.getResourceOutputs())) {
            addResourceToResourceTable(resource);
        }
    }

    private void addResourceToResourceTable(ResourceDefinition resource) {
        if (!resourceDisplays.containsKey(resource)) {
            ResourceDisplay rd = new ResourceDisplay(resource, building.localInventory, city);
            resourceDisplayTable.addItem(resource.category, rd);
            resourceDisplays.put(resource, rd);
        }
    }

    //</editor-fold>
    private void addCustomTabs() {
        Array<Table> customTabs = building.behavior.getCustomTabContent(building, this); // assume it returns Table[]
        Array<String> customTabNames = building.behavior.getCustomTabNames(); // same order

        if (customTabs != null) {
            for (int i = 0; i < customTabs.size; i++) {
                window.addTab(new HelpTab(window, customTabNames.get(i), customTabs.get(i), building.behavior.getHelpTab(building, customTabNames.get(i)), new TextButton(customTabNames.get(i), game.skin, "toggle"), game));
            }
        }
    }
    private Label getScaleLabel(Building building) {
        Label scaleLabel;
        scaleLabel = new Label("", game.skin);
        scaleLabel.setColor(Color.RED);
        if (building.behavior instanceof RecipeBuildingBehavior && ((RecipeBuildingBehavior)building.behavior).isScalable()) {
            scaleLabel.setText(((RecipeBuildingBehavior)building.behavior).getScale() + "x");
        }
        scaleLabel.setPosition(building.getX() - 25, building.getY() - 25 + scaleLabel.getPrefHeight() / 2);
        return scaleLabel;
    }

    //</editor-fold>
    //<editor-fold desc="State Changed UI Methods">
    //<editor-fold desc="Recipe IO Change">
    public void updateDisplayTable() {
        resourceDisplayTable.clearNodes();
        resourceDisplays.clear();
        constructResourceDisplayTable();
    }

    public void addDisplayedResource(ResourceDefinition resource) {
        addResourceToResourceTable(resource);
    }

    public void removeDisplayedResource(ResourceDefinition resource) {
        resourceDisplayTable.removeItem(resource.category, resourceDisplays.get(resource));
        resourceDisplays.remove(resource);
    }

    //</editor-fold>
    public void setScale(int scale) {
        scaleLabel.setText(scale + "x");
    }

    public void changeBuildingPosition(float x, float y) {
        scaleLabel.setPosition(x - 25, y - 25 + scaleLabel.getPrefHeight() / 2);
    }

    public void toggleVisibility() {
        getWindow().setVisible(!getWindow().isVisible());
        if (getWindow().isVisible()) getWindow().toFront();
    }

    //</editor-fold>
    //<editor-fold desc="Getters">
    public Window getWindow() {
        return window;
    }

    //</editor-fold>
    public void drawWindowLine(float x, float y) {
        float endX = getWindow().getX() + getWindow().getWidth() / 2;
        float endY = getWindow().getY() + getWindow().getHeight() / 2;

        if (getWindow().isVisible()) {
            city.screen.drawLine(x, y, endX, endY);
        }
    }

    public void setDeletable(boolean b) {
        canDelete = b;
    }
}
