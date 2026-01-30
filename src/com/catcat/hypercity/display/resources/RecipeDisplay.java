package com.catcat.hypercity.display.resources;

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
    public RecipeDisplay(RecipeRegistry.RecipeData recipeData, City city){
        Table content = new Table();
        if(recipeData.recipe.getWorkers()>0){
            content.add(new Image(city.game.assets.get("worker.png", Texture.class))).size(32, 32);
            content.add(new Label(String.valueOf(recipeData.recipe.getWorkers()), city.game.skin)).padLeft(3f).padRight(6f);
        }
        if(recipeData.recipe.getPowerConsumption()>0){
            content.add(new Image(city.game.assets.get("electricity.png", Texture.class))).size(32, 32);
            content.add(new Label(round3(recipeData.recipe.getPowerConsumption())+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }

        for (RecipeEntry entry : new Array.ArrayIterable<>(recipeData.recipe.getInputs())) {
            Table row = new Table();
// TODO: 1/29/26 sort this array by the value it provides
            Array<ResourceDefinition> resources = new Array<>(entry.getMatchingResources());
            resources.sort((o1, o2) -> Float.compare(entry.getPerUnitValue(o2), entry.getPerUnitValue(o1)));
            //noinspection GDXJavaUnsafeIterator
            for (ResourceDefinition res : resources) {
                row.add(new ResourceIcon(res, city)).size(32, 32).padRight(2f);
                // show the amount for the first resource
                row.add(new Label(round3(entry.value / entry.getPerUnitValue(res)) + "/s", city.game.skin)).padLeft(6f).padRight(6f).row();
            }

            if (entry.getMatchingResources().size > 1) {
                ScrollPane scroll = new ScrollPane(row);
                scroll.setScrollingDisabled(true, false);
                scroll.setFadeScrollBars(false);
                scroll.setOverscroll(false, false);
                scroll.setSmoothScrolling(true);
                scroll.setScrollY(0); // start at top
                scroll.layout();

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
                container.height(32);

                // Add to your layout
                content.add(container);
            } else {
                // Only one input — add it directly without scroll
                ResourceDefinition res = entry.getMatchingResources().first();
                content.add(new ResourceIcon(res, city)).size(32, 32).padRight(2f);
                content.add(new Label(round3(entry.value / entry.getPerUnitValue(res)) + "/s", city.game.skin)).padLeft(6f).padRight(6f);
            }

        }

        content.add(new Image(city.game.assets.get("right-arrow.png", Texture.class))).size(16, 16).pad(10f);

        for (RecipeEntry entry:new Array.ArrayIterable<>(recipeData.recipe.getOutputs())) {
            content.add(new ResourceIcon(ResourceLoader.getByKey(entry.resource), city)).size(32, 32);
            content.add(new Label(round3(entry.value)+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }




        if(recipeData.recipe.getPowerConsumption()<0){
            content.add(new Image(city.game.assets.get("electricity.png", Texture.class))).size(32, 32);
            content.add(new Label(round3(-(recipeData.recipe.getPowerConsumption()))+"/s", city.game.skin)).padLeft(3f).padRight(6f);
        }
        if(recipeData.recipe.getWorkers()<0){
            content.add(new Image(city.game.assets.get("worker.png", Texture.class))).size(32, 32);
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
    private String round3(float num) {
        int v = Math.round(num * 1000f); // fixed-point rounding
        int abs = Math.abs(v);

        int whole = abs / 1000;
        int frac  = abs % 1000;

        StringBuilder sb = new StringBuilder(16);
        if (v < 0) sb.append('-');

        sb.append(whole);

        if (frac != 0) {
            sb.append('.');

            // Convert fractional part to string manually, keeping leading zeros
            if (frac < 100) sb.append('0');
            if (frac < 10) sb.append('0');
            sb.append(frac);

            // Remove trailing zeros
            int i = sb.length() - 1;
            while (sb.charAt(i) == '0') {
                sb.deleteCharAt(i);
                i--;
            }
        }

        return sb.toString();
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
