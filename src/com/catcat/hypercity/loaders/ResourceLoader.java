package com.catcat.hypercity.loaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;
import com.catcat.hypercity.exception.MissingResourceTypeException;

public class ResourceLoader {
    private static final ObjectMap<String, ResourceDefinition> DEFINITIONS = new ObjectMap<>();
    private static final ObjectMap<String, Array<ResourceDefinition>> TAG_MAP = new ObjectMap<>();

    //uninstantiable utility class
    private ResourceLoader() {
        throw new UnsupportedOperationException();
    }

    public static void load(FileHandle[] packs) {
        for (FileHandle pack : packs) {
            FileHandle resources = Gdx.files.internal(pack.path() + "/resources.json");
            if (!resources.exists()) {
                throw new IllegalArgumentException("pack '" + pack.name() + "' does not contain resources.json");
            }
            JsonValue root = new JsonReader().parse(resources);
            // root is an array, iterate over it
            for (JsonValue value = root.child; value != null; value = value.next) {//ChatGPT came up with this
                ResourceDefinition def = new ResourceDefinition();
                def.key = pack.name() + "." + value.getString("key");

                def.name = value.getString("name", def.key);

                def.category = value.getString("category", "Misc");

                def.texturePath = value.getString("texturePath", "placeholder.png");

                if (!def.texturePath.equals("placeholder.png")) {
                    def.texturePath = pack.path() + "/" + def.texturePath;
                }
                else
                {
                    Gdx.app.log("ASSETS", "No resource texture defined for: "+def.key);
                }
                JsonValue tags = value.get("tags");
                if (tags != null) {
                    for (JsonValue tag = tags.child; tag != null; tag = tag.next) {
                        def.tags.add(tag.asString());
                    }
                }
                JsonValue attrs = value.get("attributes");
                if (attrs != null) {
                    for (JsonValue attr = attrs.child; attr != null; attr = attr.next) {
                        def.attributes.put(attr.name(), attr.asFloat());
                    }
                }
                DEFINITIONS.put(def.key, def);
            }
        }
        buildTagMap();
    }

    public static ObjectMap.Values<ResourceDefinition> getAll() {
        return new ObjectMap.Values<>(DEFINITIONS);
    }

    //This method exists because we don't have like WOOD(...) like the enum comes with
    public static ResourceDefinition getByKey(String key) {
        ResourceDefinition def = DEFINITIONS.get(key);
        if (def == null) throw new MissingResourceTypeException("No resource with key: " + key);
        return def;
    }
    private static void buildTagMap() {
        TAG_MAP.clear();
        for (ResourceDefinition r : getAll()) {
            for (String t : new ObjectSet.ObjectSetIterator<>(r.tags)) {
                if (!TAG_MAP.containsKey(t)) TAG_MAP.put(t, new Array<>());
                TAG_MAP.get(t).add(r);
            }
        }
    }
    public static Array<ResourceDefinition> getResourcesWithTag(String tag) {
        Array<ResourceDefinition> resources = TAG_MAP.get(tag);
        if (resources == null) return new Array<>();
        return resources;
    }
}
