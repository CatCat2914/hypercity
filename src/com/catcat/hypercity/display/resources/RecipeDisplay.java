package com.catcat.hypercity.display.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.recipe.Recipe;
import com.catcat.hypercity.definitions.recipe.RecipeEntry;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.loaders.RecipeRegistry;
import com.catcat.hypercity.loaders.ResourceLoader;
public class RecipeDisplay extends Table {
    private static final float ICON_SIZE = 32f;
    public RecipeDisplay(RecipeRegistry.RecipeData recipeData, City city) {
        this(recipeData, null, city);
    }
    public RecipeDisplay(RecipeRegistry.RecipeData recipeData, ResourceDefinition resource, City city){
        Table content = new Table();
        if(recipeData.recipe.getWorkers()>0){
            content.add(new Image(city.game.assets.get("worker.png", Texture.class))).size(ICON_SIZE, ICON_SIZE);
            content.add(new Label(String.valueOf(recipeData.recipe.getWorkers()), city.game.skin)).padLeft(3f).padRight(6f);
        }
        if(recipeData.recipe.getPowerConsumption()>0){
            content.add(new Image(city.game.assets.get("electricity.png", Texture.class))).size(ICON_SIZE, ICON_SIZE);
            content.add(new Label(round3(recipeData.recipe.getPowerConsumption())+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }

        for (RecipeEntry entry : new Array.ArrayIterable<>(recipeData.recipe.getInputs())) {
            Table row = new Table();
            Array<ResourceDefinition> resources = new Array<>(entry.getMatchingResources());
            resources.sort((o1, o2) -> Float.compare(entry.getPerUnitValue(o2), entry.getPerUnitValue(o1)));
            float scrollY = ICON_SIZE * (resources.indexOf(resource, false));
            //noinspection GDXJavaUnsafeIterator
            for (ResourceDefinition res : resources) {
                row.add(new ResourceIcon(res, city)).size(ICON_SIZE, ICON_SIZE).padRight(2f);
                // show the amount for the first resource
                row.add(new Label(round3(entry.value / entry.getPerUnitValue(res)) + "/s", city.game.skin)).padLeft(6f).padRight(6f).row();
            }

            if (entry.getMatchingResources().size > 1) {
                ScrollPane scroll = new ScrollPane(row);
                scroll.setScrollingDisabled(true, false);
                scroll.setFadeScrollBars(false);
                scroll.setOverscroll(false, false);
                scroll.setSmoothScrolling(true);

                // Create a Pixmap for the border
                int borderSize = 2; // thickness of the border
                int width = 8, height = 8; // small pixmap because NinePatch will scale
                Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
                pixmap.setColor(new Color(0x282828ff));  // border color
                pixmap.fill();                 // fill the whole pixmap

                NinePatch ninePatch = new NinePatch(new Texture(pixmap), borderSize, borderSize, borderSize, borderSize);
                NinePatchDrawable borderDrawable = new NinePatchDrawable(ninePatch);

                // Wrap the scroll in a container with the border
                Container<ScrollPane> container = new Container<>(scroll);
                container.background(borderDrawable);
                container.height(ICON_SIZE);

                // Add to your layout
                content.add(container);

                Gdx.app.postRunnable(() -> {
                    scroll.layout();
                    scroll.setScrollY(scrollY);
                    scroll.updateVisualScroll();
                });
            } else {
                // Only one input — add it directly without scroll
                ResourceDefinition res = entry.getMatchingResources().first();
                content.add(new ResourceIcon(res, city)).size(ICON_SIZE, ICON_SIZE).padRight(2f);
                content.add(new Label(round3(entry.value / entry.getPerUnitValue(res)) + "/s", city.game.skin)).padLeft(6f).padRight(6f);
            }

        }

        content.add(new Image(city.game.assets.get("right-arrow.png", Texture.class))).size(ICON_SIZE/2, ICON_SIZE/2).padLeft(10f).padRight(10f);

        for (RecipeEntry entry:new Array.ArrayIterable<>(recipeData.recipe.getOutputs())) {
            content.add(new ResourceIcon(ResourceLoader.getByKey(entry.resource), city)).size(ICON_SIZE, ICON_SIZE);
            content.add(new Label(round3(entry.value)+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }




        if(recipeData.recipe.getPowerConsumption()<0){
            content.add(new Image(city.game.assets.get("electricity.png", Texture.class))).size(ICON_SIZE, ICON_SIZE);
            content.add(new Label(round3(-(recipeData.recipe.getPowerConsumption()))+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }
        if(recipeData.recipe.getWorkers()<0){
            content.add(new Image(city.game.assets.get("worker.png", Texture.class))).size(ICON_SIZE, ICON_SIZE);
            content.add(new Label(String.valueOf(-recipeData.recipe.getWorkers()), city.game.skin)).padLeft(3f).padRight(6f);
        }
        if(recipeData.building!=null) {
            content.add(new Label("(" + recipeData.building.name + ")", city.game.skin)).padLeft(6f);
        }

        this.add(wrapScrollableContentHorizontal(content,400, city.game.skin));
    }

    /**
     * for when you don't need to know which kind of building it is
     * @param recipe the recipe to display
     * @param city basically whatever city you can find idk (the current one)
     */
    public RecipeDisplay(Recipe recipe, City city) {
        this(new RecipeRegistry.RecipeData(recipe, null), city);
    }
    @SuppressWarnings("DefaultLocale")
    private String round3(float num) {
        return String.format("%.3f", num)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
    }
    private Table wrapScrollableContentHorizontal(Table content, float maxWidth, Skin skin) {
        content.pad(10f);
        ScrollPane scrollPane = new ScrollPane(content, skin, "clean");
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, true);
        scrollPane.pack();
        Table wrapper = new Table();
        wrapper.add(scrollPane)
            .growX()
            .maxWidth(maxWidth);
        return wrapper;
    }
}
