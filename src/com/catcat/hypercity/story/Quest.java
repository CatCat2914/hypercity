package com.catcat.hypercity.story;

import com.catcat.hypercity.city.City;

import java.util.function.Predicate;


public class Quest {
    //I don't think it's serialized but just to be safe I've marked things as transient
    private String questInfo;
    private transient Predicate<City> condition;
    private transient Runnable onComplete;
    private transient City city;

    private Quest(){}//reflect
    Quest(City city, String questInfo, Predicate<City> condition) {
        this(city, questInfo, condition, ()->{});
    }
    Quest(City city, String questInfo, Predicate<City> condition, Runnable onComplete) {
        this.city = city;
        this.questInfo = questInfo;
        this.condition = condition;
        this.onComplete = onComplete;
    }


    boolean check() {
        boolean win = condition.test(city);
        if(win) {
            onComplete.run();
        }
        return win;
    }


    public String getQuestInfo() {
        return questInfo;
    }
}
