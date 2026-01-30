package com.catcat.hypercity.teavm;

import com.catcat.hypercity.building.behavior.BuildingBehavior;
import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.loaders.BuildingLoader;
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration;
import com.github.xpenatan.gdx.backends.teavm.TeaApplication;
import com.catcat.hypercity.CityGame;

/**
 * Launches the TeaVM/HTML application.
 */
public class TeaVMLauncher {
    public static void main(String[] args) {
        TeaApplicationConfiguration config = new TeaApplicationConfiguration("canvas");
        //// If width and height are each greater than 0, then the app will use a fixed size.
        //config.width = 640;
        //config.height = 480;
        //// If width and height are both 0, then the app will use all available space.
        config.width = 0;
        config.height = 0;
        BuildingLoader.setBehaviorFactory(def -> {
            Class<?> behaviorClass;
            try {
                behaviorClass = Class.forName(def.className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            try {
                return (BuildingBehavior) behaviorClass.getDeclaredConstructor(BuildingDefinition.class).newInstance(def);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        new TeaApplication(new CityGame(), config);
    }
}