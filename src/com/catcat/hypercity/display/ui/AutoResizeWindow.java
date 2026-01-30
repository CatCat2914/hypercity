package com.catcat.hypercity.display.ui;


import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

public class AutoResizeWindow extends Window {
    public AutoResizeWindow(String title, Skin skin) {
        this(title, skin, "default");
    }

    public AutoResizeWindow(String title, Skin skin, String styleName) {
        super(title, skin, styleName);
        //I'm using defaults here because I forget often but I almost always want these (as in every time)
        padTop(20).padBottom(10).padLeft(6).padRight(6);
        setResizeBorder(16);
        defaults().pad(6f);
        if (styleName.equals("dialog")) {
            pad(8f);
            padTop(20f);
        }
    }

    @Override
    public void act(float delta) {
        this.setSize(this.getPrefWidth(), this.getPrefHeight());
        super.act(delta);
    }
}
