package com.catcat.hypercity.display.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;

public class CategoryNode extends Tree.Node<CategoryNode, Object, Actor> {

    public CategoryNode(Actor actor) {
        super(actor);
        this.setSelectable(false);
    }

    public static CategoryNode createLabelNode(String name, Skin skin) {
        Label label = new Label(name, skin);
        return new CategoryNode(label);
    }
    public static CategoryNode createTableNode(Table table) {
        return new CategoryNode(table);
    }
}
