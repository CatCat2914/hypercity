package com.catcat.hypercity.building.behavior.utility;

import com.catcat.hypercity.definitions.building.BuildingDefinition;
import com.catcat.hypercity.building.behavior.BuildingBehavior;

import java.util.HashMap;
import java.util.function.Consumer;

//look at java.util.function stuff surely something there is useful
//consider using a supplier somewhere in my code
//runnable runnable runnable
@SuppressWarnings("unused")
public class ScriptingModuleBehavior extends BuildingBehavior {
    HashMap<String, Consumer<String>> functions;
    public ScriptingModuleBehavior(){}
    public ScriptingModuleBehavior(BuildingDefinition definition) {
        setDefinition(definition);
    }
}
