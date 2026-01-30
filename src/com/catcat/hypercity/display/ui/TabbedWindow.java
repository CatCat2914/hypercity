package com.catcat.hypercity.display.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.HashMap;

public class TabbedWindow extends AutoResizeWindow {
    private final Table buttons = new Table();
    private final ButtonGroup<Button> buttonGroup = new ButtonGroup<>();
    private final HashMap<String, Tab> tabs = new HashMap<>();
    private Cell<Table> contentCell;
    private final boolean allowDuplicates;

    public TabbedWindow(String title, Skin skin, String styleName, boolean allowDuplicates) {
        super(title, skin, styleName);
        this.add(buttons).row();
        buttons.defaults().pad(2f);
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);
        this.allowDuplicates = allowDuplicates;
    }

    public void addTab(Tab tab) {
        Tab existing = tabs.get(tab.getName());
        if (!allowDuplicates && existing != null) {
            // Replace content of existing tab without touching buttons
            existing.setContent(tab.getContent());
            if (contentCell.getActor() == existing.getContent()) {
                contentCell.setActor(tab.getContent());
            }
            return; // no need to add a new button
        }

        // add tab normally
        tab.getButton().addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setVisibleTab(tab);
            }
        });
        buttons.add(tab.getButton()).width(tab.getButton().getWidth()+8f);
        buttonGroup.add(tab.getButton());
        if (contentCell == null) {
            setVisibleTab(tab);
        }
        tabs.put(tab.getName(), tab);
    }

    private void setVisibleTab(Tab tab) {
        if (contentCell == null) {
            contentCell = this.add(tab.getContent());
            row();
        } else {
            contentCell.setActor(tab.getContent());
        }
        pack();
    }

    public void overwriteTab(String name, Table newContent) {
        Tab tab = tabs.get(name);
        if (tab == null) return;
        if (contentCell.getActor() == tab.getContent()) {
            contentCell.setActor(newContent);
        }
        tab.setContent(newContent);
        pack();
    }

    public static class Tab {
        private final String name;
        private Table content;
        private final Button button;

        /**
         * @param name    the name of the tab
         * @param content the content in the tab
         * @param button  how the button will look. Do not add a listener.
         */
        public Tab(String name, Table content, Button button) {
            this.name = name;
            this.content = content;
            this.button = button;
        }

        String getName() {
            return name;
        }

        Table getContent() {
            return content;
        }

        void setContent(Table content) {
            this.content = content;
        }

        Button getButton() {
            return button;
        }
    }
}
