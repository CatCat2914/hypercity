package com.catcat.hypercity.campaign;

import com.badlogic.gdx.utils.Array;
import com.catcat.hypercity.definitions.campaign.LevelData;
import com.catcat.hypercity.definitions.campaign.StartingCondition;
import com.catcat.hypercity.definitions.campaign.WinningCondition;
import com.catcat.hypercity.loaders.ResourceLoader;
//not serialized

/**
 * This is not a story mode. I (2025-12-01) have not added a story mode yet, but I do plan to. It will involve tasks, much like campaign, but uses the same city throughout.
 */
public class CampaignManager {
    private transient final Array<LevelData> levels = new Array<>();
    private int highestLevelBeaten = -1;

    public CampaignManager() {
        //<editor-fold desc="Level Creation">

        levels.add(
            new LevelData("New Beginnings",
                new StartingCondition(1000f),
                new WinningCondition(null, ResourceLoader.getByKey("base.WATER"), 10f, null),
                "Welcome to the game! In this level, your only goal is to place a water pump and deliver 10 water to the city inventory. No need to worry about population or electricity yet!\n\nWhen you place a water pump, click on it to open a window. This window has options and info. \nYou will see an \"Add/Remove Road\" button. When you click that button, click the building you want the road to go to to add the road.")
                .banAll()
                .unbanBuilding("base.WATER_PUMP", "base.DEPOT")
                .addStartingBuilding("base.DEPOT", 0, 0)
                .addWinningCondition(new WinningCondition(null, null, null, null))
        );
//too soon
        levels.add(
            new LevelData("Saw Town",
                new StartingCondition(),
                new WinningCondition(null, ResourceLoader.getByKey("base.PLANKS"), null, 2.9f),
                "It's time to learn the production chain. You can figure these out on your own by exploring recipes and rate tabs, but the first one you'll learn makes planks. Factories make planks from wood. Wood comes from trees which need water to grow. Water comes from water pumps. You'll also need workers and a stable source of electricity. Reach a stable production of 3 planks/s to win"
            ).banBuilding("base.SCRIPTING_MODULE", "base.MULTICITY_LINK")
        );
        //</editor-fold>
    }

    public LevelData getNextLevel() {
        if (highestLevelBeaten >= levels.size - 1) {
            return null; //sandbox mode idk
        }
        return levels.get(highestLevelBeaten + 1);
    }

    public void beatLevel(LevelData data) {
        int index = levels.indexOf(data, false); // can override equals() later
        if (index == highestLevelBeaten + 1) {
            highestLevelBeaten = index;
        }
    }

    public Array<LevelData> getBeatenLevels() {
        Array<LevelData> beaten = new Array<>();
        for (int i = 0; i <= highestLevelBeaten; i++) {
            beaten.add(levels.get(i));
        }
        return beaten;
    }
}
