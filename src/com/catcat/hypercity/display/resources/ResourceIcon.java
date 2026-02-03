package com.catcat.hypercity.display.resources;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.recipe.RecipeEntry;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.display.ui.AutoResizeWindow;
import com.catcat.hypercity.loaders.RecipeRegistry;

public class ResourceIcon extends Image {
    private static AutoResizeWindow activeWindow;
    private static final Array<AutoResizeWindow> windowStack = new Array<>();
    public ResourceIcon(ResourceDefinition definition, City city){
        super(city.game.assets.get(definition.texturePath, Texture.class));
        addListener(new ClickListener() {
            public void clicked (InputEvent event, float x, float y) {
                if (activeWindow != null) {
                    windowStack.add(activeWindow);
                    activeWindow.remove();
                }
                AutoResizeWindow window = new AutoResizeWindow("Recipe Book", city.game.skin, "default");

                Table header = new Table();
                TextButton backButton = new TextButton("Back", city.game.skin);
                backButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (windowStack.notEmpty()) {
                            window.remove();
                            activeWindow = windowStack.pop();
                            city.screen.uiStage.addActor(activeWindow);
                        }
                    }
                });
                header.add(backButton).size(50f,30f);
                header.add(new Image(city.game.assets.get(definition.texturePath, Texture.class))).size(50, 50);
                header.add(new Label(definition.name, city.game.skin, "window")).pad(6f);
                window.add(header).row();

                Array<RecipeDisplay> uses = new Array<>();
                window.add(new Label("Obtained By: ", city.game.skin)).row();
                Table obtainedBy = new Table();

                for (RecipeRegistry.RecipeData recipeData : new Array.ArrayIterable<>(RecipeRegistry.getAll())) {
                    if (containsResource(recipeData.recipe.getOutputs(), definition)) {
                        obtainedBy.add(new RecipeDisplay(recipeData, definition, city)).row();
                    }
                    if (containsResource(recipeData.recipe.getInputs(), definition)) {
                        uses.add(new RecipeDisplay(recipeData, definition, city));
                    }
                }

                window.add(wrapScrollableContent(obtainedBy, 250f, city.game.skin)).row();
                window.add(new Label("Used In: ", city.game.skin)).row();
                Table usedIn = new Table();
                for (RecipeDisplay recipeDisplay:new Array.ArrayIterable<>(uses)) {
                    usedIn.add(recipeDisplay).row();
                }
                window.add(wrapScrollableContent(usedIn, 250f, city.game.skin)).row();
                TextButton closeButton = new TextButton("Close", city.game.skin);
                closeButton.addListener(new ClickListener(){
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        window.remove();
                        activeWindow = null;
                    }
                });
                window.add(closeButton).size(60f, 30f).padTop(10f);
                window.pack();

                window.setPosition(
                    MathUtils.roundPositive(
                        city.screen.uiStage.getCamera().position.x - window.getWidth() / 2f),
                    MathUtils.roundPositive(
                        city.screen.uiStage.getCamera().position.y - window.getHeight() / 2f)
                );

                city.screen.uiStage.addActor(window);
                activeWindow = window;
            }
        });
    }
    private boolean containsResource(Array<RecipeEntry> entries, ResourceDefinition definition) {
        for (RecipeEntry entry : new Array.ArrayIterable<>(entries)) {
            for (ResourceDefinition res : new Array.ArrayIterable<>(entry.getMatchingResources())) {
                if (res.equals(definition)) {
                    return true;
                }
            }
        }
        return false;
    }
    private Table wrapScrollableContent(Table content, float maxHeight, Skin skin) {
        content.pad(10f);
        ScrollPane scrollPane = new ScrollPane(content, skin, "clean");
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // vertical only
        scrollPane.pack();


        Table wrapper = new Table();
        wrapper.add(scrollPane)
            .growX()
            .maxHeight(maxHeight)
            .width(scrollPane.getPrefWidth() + 10f);
        return wrapper;
    }

}
