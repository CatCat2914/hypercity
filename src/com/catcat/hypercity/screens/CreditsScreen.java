package com.catcat.hypercity.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.catcat.hypercity.CityGame;


public class CreditsScreen extends InputAdapter implements Screen {
    private final Stage stage;

    CreditsScreen(final CityGame game) {
        this.stage = new Stage(new ScreenViewport(), game.batch);
        Label credits = new Label(
            "--Credits--\n" +
                "Lead Developer: CatCat2914\n\n" +
                "Music:\n" +
                "Title Music: \"Meninjau - Noctara\"\n" +
                "Free Music Archive CC BY\n\n" +
                "City Music: Unknown"
            , game.skin);
        credits.setWrap(true);
        credits.setAlignment(Align.center);
        Table table = new Table();
        table.add(credits).width(800).height(450).row();
        table.setVisible(true);
        credits.setVisible(true);
        table.setPosition(
            MathUtils.roundPositive(stage.getCamera().position.x - table.getWidth() / 2f),
            MathUtils.roundPositive(stage.getCamera().position.y + 200 - table.getHeight() / 2f));
        stage.addActor(table);
        TextButton backButton = new TextButton("Back", game.skin);
        backButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(game.mainMenu);
            }
        });
        table.add(backButton).width(72).height(36);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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

    }

    @Override
    public void dispose() {
    }

}
