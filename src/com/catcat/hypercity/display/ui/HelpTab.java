package com.catcat.hypercity.display.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.catcat.hypercity.CityGame;

public class HelpTab extends TabbedWindow.Tab {
    public HelpTab(TabbedWindow window, String name, Table content, Table helpContent, Button button, CityGame game) {
        super(name, content, button);
        TextureRegionDrawable imageUp = new TextureRegionDrawable(game.assets.get("misc/help-up.png", Texture.class));
        imageUp.setMinSize(32, 32);
        TextureRegionDrawable imageDown = new TextureRegionDrawable(game.assets.get("misc/help-down.png", Texture.class));
        imageDown.setMinSize(32, 32);
        Button openHelp = new Button(imageUp, imageDown, imageDown);
        openHelp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.overwriteTab(name, helpContent);
                openHelp.setChecked(false);
            }
        });
        Button closeHelp = new Button(imageDown, imageUp, imageUp);
        closeHelp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.overwriteTab(name, content);
                closeHelp.setChecked(false);
            }
        });
        content.add(openHelp).bottom().center();
        helpContent.add(closeHelp).bottom().center();
    }
}
