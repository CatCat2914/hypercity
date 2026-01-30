package com.catcat.hypercity.definitions.campaign;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.catcat.hypercity.city.City;
import com.catcat.hypercity.definitions.resource.ResourceDefinition;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;

/**
 * This class represents a winning condition. This could be reaching a certain population, having a certain
 * amount of resources of a type, having a certain dx (over like a time so you can't just save up)
 * <p>
 * If it's null in a city that would mean it's sandbox
 */
public class WinningCondition implements Json.Serializable {
    //<editor-fold desc="Instance Variables">
    private Integer targetPopulation;
    private ResourceDefinition targetResource;
    private Float targetAmount;
    private Float requiredRate;

    private final transient Deque<Float> pastRates = new ArrayDeque<>();
    private final transient Deque<Integer> pastPopulation = new ArrayDeque<>();
    private final transient EnumSet<ConditionType> conditionTypes = EnumSet.noneOf(ConditionType.class);
    //</editor-fold>
    @SuppressWarnings("unused")
    public WinningCondition(){}
    /**
     * Creates a new WinningCondition.
     *
     * @param targetPopulation Target population to win
     * @param targetResource   Resource type to monitor
     * @param targetAmount     Amount of the resource to win
     * @param requiredRate     Required rate of change
     */
    public WinningCondition(Integer targetPopulation, ResourceDefinition targetResource, Float targetAmount, Float requiredRate) {
        this.targetPopulation = targetPopulation;
        this.targetResource = targetResource;
        this.targetAmount = targetAmount;
        this.requiredRate = requiredRate;
        buildConditionTypes();
    }

    private void buildConditionTypes() {
        if (targetPopulation != null) {
            conditionTypes.add(ConditionType.POPULATION);
        }
        if (targetAmount != null) {
            if (targetResource == null) {
                throw new RuntimeException("malformatted winning condition, missing resource.");
            }
            conditionTypes.add(ConditionType.RESOURCE_AMOUNT);
        }
        if (requiredRate != null) {
            if (targetResource == null) {
                throw new RuntimeException("malformatted winning condition, missing resource.");
            }
            conditionTypes.add(ConditionType.RESOURCE_RATE);
        }
    }

    boolean checkWinningCondition(City city) {
        //population target
        if (conditionTypes.contains(ConditionType.POPULATION)) {
            if (!checkPopulationTest(city.getWorkers())) {
                return false;
            }
        }

        //amount target
        if (conditionTypes.contains(ConditionType.RESOURCE_AMOUNT)) {
            if (city.getResourceAmount(targetResource) < targetAmount) {
                return false;
            }
        }

        //
        if (conditionTypes.contains(ConditionType.RESOURCE_RATE)) {
            //noinspection RedundantIfStatement (because I might add more checks)
            if (!checkRatesTest(city.getResourceChangeRate(targetResource))) {
                return false;
            }
        }

        //didn't fail any of the win condition checks
        return true;
    }

    private boolean checkRatesTest(float latestRate) {
        pastRates.add(latestRate);
        if (pastRates.size() > Gdx.graphics.getFramesPerSecond() * 10) { //must be high enough for a full 10 seconds
            pastRates.removeFirst();
        } else {
            return false;
        }
        if (latestRate < requiredRate)
            return false; //quit early, it would fail with this rate no matter what
        return (pastRates.stream().min(Float::compareTo).orElse(0.0f) > requiredRate);
    }

    private boolean checkPopulationTest(int pop) {
        pastPopulation.add(pop);

        int maxSamples = Gdx.graphics.getFramesPerSecond() * 10;
        if (pastPopulation.size() > maxSamples) { //must be high enough for a full 10 seconds
            pastPopulation.removeFirst();
        } else {
            return false;
        }
        if (pop < targetPopulation) return false;
        return pastPopulation.stream().min(Integer::compareTo).orElse(0) >= targetPopulation;
    }

    public Set<ConditionType> getConditionTypes() {
        return Collections.unmodifiableSet(conditionTypes);
    }

    public Integer getTargetPopulation() {
        return targetPopulation;
    }

    public ResourceDefinition getTargetResource() {
        return targetResource;
    }

    public Float getRequiredRate() {
        return requiredRate;
    }

    public Float getTargetAmount() {
        return targetAmount;
    }

    @Override
    public void write(Json json) {
        if (targetPopulation != null) json.writeValue("targetPopulation", targetPopulation);
        if (targetResource != null) json.writeValue("targetResource", targetResource, ResourceDefinition.class);
        if (targetAmount != null) json.writeValue("targetAmount", targetAmount);
        if (requiredRate != null) json.writeValue("requiredRate", requiredRate);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        targetPopulation = json.readValue("targetPopulation", Integer.class, jsonData);
        targetResource = json.readValue("targetResource", ResourceDefinition.class, jsonData);
        targetAmount = json.readValue("targetAmount", Float.class, jsonData);
        requiredRate = json.readValue("requiredRate", Float.class, jsonData);
        // Rebuild conditionTypes
        conditionTypes.clear();
        buildConditionTypes();
        pastRates.clear();
        pastPopulation.clear();
    }

    public enum ConditionType {
        POPULATION,
        RESOURCE_AMOUNT,
        RESOURCE_RATE
    }
}
