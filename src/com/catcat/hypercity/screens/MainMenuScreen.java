package com.catcat.hypercity.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.catcat.hypercity.CityGame;
import com.catcat.hypercity.definitions.campaign.LevelData;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.display.ui.AutoResizeWindow;
public class MainMenuScreen implements Screen {
    private final transient CityGame game;
    private final transient Stage stage;
    private transient Window citySelectWindow;
    private transient Window pastLevelsWindow;

    public MainMenuScreen(final CityGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.batch);
        addStoryModeButton();
        addNewGameButton();
        addContinueButton();
        addCampaignButton();
        addPastLevelButton();
        addCreditsButton();
        addTitleImage();
    }

    //<editor-fold desc="Buttons & Windows">
    private void addCreditsButton() {
        TextButton credits = new TextButton("Credits", game.skin);
        credits.setSize(200, 40f);
        credits.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new CreditsScreen(game));
            }
        });
        credits.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - credits.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - credits.getHeight() / 2f - 200f));
        stage.addActor(credits);
    }

    private void addTitleImage() {
        Image title = new Image(new Texture("misc/title.png"));
        title.setScale(1);
        title.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - title.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y + 200 - title.getHeight() / 2f));
        stage.addActor(title);
    }

    private void addPastLevelButton() {
        TextButton pastLevels = new TextButton("Previous Levels", game.skin);
        pastLevels.setSize(200, 40f);
        pastLevels.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (pastLevelsWindow == null || !pastLevelsWindow.isVisible()) {
                    AddPastLevelsWindow();
                } else {
                    pastLevelsWindow.setVisible(false);
                }
            }

            private void AddPastLevelsWindow() {
                pastLevelsWindow = new AutoResizeWindow("Replay Campaign", game.skin, "default");
                Array<LevelData> beaten = game.campaignManager.getBeatenLevels();
                for (int i = 0; i < beaten.size; i++) {
                    LevelData levelData = beaten.get(i);
                    Label name = new Label("Level " + (i + 1) + ": " + levelData.getName(), game.skin);
                    pastLevelsWindow.add(name);
                    TextButton playButton = new TextButton("Play", game.skin);
                    playButton.addListener(new ClickListener() {
                        public void clicked(InputEvent event, float x, float y) {
                            City city = new City(game, levelData);
                            game.addCity(city);
                            game.setScreen(city.screen);
                            pastLevelsWindow.remove();
                        }
                    });
                    pastLevelsWindow.add(playButton).row();
                }
                pastLevelsWindow.setPosition(MathUtils.roundPositive(stage.getCamera().position.x - pastLevelsWindow.getWidth() / 2f), MathUtils.roundPositive(stage.getCamera().position.y - pastLevelsWindow.getHeight() / 2f));
                stage.addActor(pastLevelsWindow);
            }
        });
        pastLevels.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - pastLevels.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - pastLevels.getHeight() / 2f - 150f));
        stage.addActor(pastLevels);
    }

    private void addCampaignButton() {
        TextButton campaignButton = new TextButton("Next level", game.skin);
        campaignButton.setSize(200, 40f);
        campaignButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                City city = new City(game, game.campaignManager.getNextLevel());
                game.addCity(city); //have this if I want to include campaign in saves
                game.setScreen(city.screen);
            }
        });
        campaignButton.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - campaignButton.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - campaignButton.getHeight() / 2f - 100f));
        stage.addActor(campaignButton);
    }

    private void addContinueButton() {
        TextButton continueButton = new TextButton("Continue", game.skin);
        continueButton.setSize(200, 40f);
        continueButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (citySelectWindow == null || !citySelectWindow.isVisible()) {
                    AddCitySelectWindow();
                } else {
                    citySelectWindow.setVisible(false);
                }
            }

            private void AddCitySelectWindow() {
                citySelectWindow = new AutoResizeWindow("Select City", game.skin, "default");
                for (City city : new Array.ArrayIterator<>(game.getCities())) {
                    Table cityEntry = new Table();

                    Table buttons = new Table();
                    TextField nameField = new TextField(city.getName(), game.skin);
                    nameField.setTextFieldListener((textField, c) -> city.setName(textField.getText().trim()));
                    buttons.add(nameField).width(100).height(20).padBottom(5f).row();
                    TextButton continueButton = new TextButton("Continue", game.skin);
                    continueButton.addListener(new ClickListener() {
                        public void clicked(InputEvent event, float x, float y) {
                            citySelectWindow.remove();
                            game.setScreen(city.screen);
                        }
                    });
                    buttons.add(continueButton).width(100).height(20).padBottom(5f).row();
                    TextButton deleteButton = new TextButton("Delete", game.skin);
                    deleteButton.addListener(new ClickListener() {
                        public void clicked(InputEvent event, float x, float y) {
                            Window window1 = new AutoResizeWindow("Delete City?", game.skin, "dialog");
                            window1.add(new Label(" This action \n cannot be undone. ", game.skin)).row();
                            TextButton confirmButton = new TextButton("Delete", game.skin);
                            TextButton noButton = new TextButton("Keep", game.skin);
                            window1.add(confirmButton).width(confirmButton.getWidth()+8f);
                            window1.add(noButton).width(noButton.getWidth()+8f);
                            window1.setPosition(MathUtils.roundPositive(stage.getCamera().position.x - window1.getWidth() / 2f), MathUtils.roundPositive(stage.getCamera().position.y - window1.getHeight() / 2f));
                            window1.pack();
                            stage.addActor(window1);
                            confirmButton.addListener(new ClickListener() {
                                public void clicked(InputEvent event, float x, float y) {
                                    game.removeCity(city);
                                    cityEntry.remove();
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
                    buttons.add(deleteButton).width(100).height(20).padBottom(5f).row();

                    String infoStr = "\nBuildings: " + city.getBuildingCount() + "\nWorkers: " + city.getWorkers();
                    Label cityInfo = new Label(infoStr, game.skin);
                    cityEntry.add(buttons);
                    cityEntry.add(cityInfo).row();
                    citySelectWindow.add(cityEntry).row();
                }
                citySelectWindow.setPosition(MathUtils.roundPositive(stage.getCamera().position.x - citySelectWindow.getWidth() / 2f), MathUtils.roundPositive(stage.getCamera().position.y - citySelectWindow.getHeight() / 2f));
                stage.addActor(citySelectWindow);
            }
        });
        continueButton.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - continueButton.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - continueButton.getHeight() / 2f - 50f));
        stage.addActor(continueButton);
    }

    private void addNewGameButton() {
        TextButton newGameButton = new TextButton("New Game", game.skin);
        newGameButton.setSize(200, 40f);
        newGameButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                City city = new City(game, "my city");
                game.addCity(city);
                game.setScreen(city.screen);
            }
        });
        newGameButton.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - newGameButton.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - newGameButton.getHeight() / 2f));
        stage.addActor(newGameButton);
    }

    private void addStoryModeButton() {
        TextButton storyModeButton = new TextButton("Story Mode", game.skin);
        storyModeButton.setSize(200, 40f);
        storyModeButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(game.storyManager.city.screen);
            }
        });
        storyModeButton.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - storyModeButton.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y - storyModeButton.getHeight() / 2f + 50f));
        stage.addActor(storyModeButton);
    }

    //</editor-fold>
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
        Gdx.input.setCatchKey(Input.Keys.SPACE, true);
        Gdx.input.setCatchKey(Input.Keys.W, true);
        Gdx.input.setCatchKey(Input.Keys.A, true);
        Gdx.input.setCatchKey(Input.Keys.S, true);
        Gdx.input.setCatchKey(Input.Keys.D, true);
        game.mainMenuMusic.play();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.DARK_GRAY);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, false); // true centers the camera
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        if (citySelectWindow != null) {
            citySelectWindow.setVisible(false);
            citySelectWindow.remove();
        }
        if (pastLevelsWindow != null) {
            pastLevelsWindow.setVisible(false);
            pastLevelsWindow.remove();
        }
        game.mainMenuMusic.stop();
    }

    @Override
    public void dispose() {

    }
}
