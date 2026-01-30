package com.catcat.hypercity.display.city;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.catcat.hypercity.building.Building;
import com.catcat.hypercity.transport.Road;
import com.catcat.hypercity.city.City;

public class Minimap extends Actor {
    private final City city;
    private final TextureRegion circle;
    private final TextureRegion white;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;

    public Minimap(City city) {
        this.city = city;
        int resolution = 100;
        Pixmap c = new Pixmap(resolution * 2 + 1, resolution * 2 + 1, Pixmap.Format.RGBA8888);
        c.setColor(Color.WHITE);
        c.fillCircle(resolution, resolution, resolution);
        circle = new TextureRegion(new Texture(c));
        c.dispose();
        Pixmap bg = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bg.setColor(Color.WHITE);
        bg.fill();
        white = new TextureRegion(new Texture(bg));
        bg.dispose();
        setBounds(circle.getRegionX(), circle.getRegionY(), circle.getRegionWidth(), circle.getRegionHeight());
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        float circleRadius = 2.5f;
        Color before = batch.getColor();
        batch.setColor(new Color(0xD1BE9DFF));
        batch.draw(white, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        float frameMinX = Float.MAX_VALUE;
        float frameMaxX = -Float.MAX_VALUE;
        float frameMinY = Float.MAX_VALUE;
        float frameMaxY = -Float.MAX_VALUE;
        for (Building building : city.getAllBuildings()) {
            //see bounds
            frameMinX = Math.min(frameMinX, building.getX() - 100);
            frameMaxX = Math.max(frameMaxX, building.getX() + 100);
            frameMinY = Math.min(frameMinY, building.getY() - 100);
            frameMaxY = Math.max(frameMaxY, building.getY() + 100);
            float scaledX = getX() + (building.getX() - minX) / ((maxX - minX) / getWidth()) - circleRadius;
            float scaledY = getY() + (building.getY() - minY) / ((maxY - minY) / getHeight()) - circleRadius;
            for (Road road : new Array.ArrayIterable<>(building.roads)) {
                if (building == road.getTargetBuilding()) continue;
                float x2 = getX() + (road.getTargetBuilding().getX() - minX) / ((maxX - minX) / getWidth()) - circleRadius;
                float y2 = getY() + (road.getTargetBuilding().getY() - minY) / ((maxY - minY) / getHeight()) - circleRadius;
                drawRoad(batch, scaledX, scaledY, x2, y2, circleRadius);
                batch.setColor(road.getTargetBuilding().behavior.getDefinition().getColorObj());
                batch.draw(circle, x2, y2, getOriginX(), getOriginY(), circleRadius * 2, circleRadius * 2, getScaleX(), getScaleY(), getRotation());//just draw target again so it's definitely on top
            }
            batch.setColor(building.behavior.getDefinition().getColorObj());
            batch.draw(circle, scaledX, scaledY, getOriginX(), getOriginY(), 5, 5, getScaleX(), getScaleY(), getRotation());

        }
        frameMinX = Math.min(frameMinX, city.screen.stage.getCamera().position.x - 100);
        frameMaxX = Math.max(frameMaxX, city.screen.stage.getCamera().position.x + 100);
        frameMinY = Math.min(frameMinY, city.screen.stage.getCamera().position.y - 100);
        frameMaxY = Math.max(frameMaxY, city.screen.stage.getCamera().position.y + 100);
        batch.setColor(Color.RED);
        batch.draw(circle, getX() + (city.screen.stage.getCamera().position.x - minX) / ((maxX - minX) / getWidth()), getY() + (city.screen.stage.getCamera().position.y - minY) / ((maxY - minY) / getHeight()), getOriginX(), getOriginY(), circleRadius * 2, circleRadius * 2, getScaleX(), getScaleY(), getRotation());
        minX = frameMinX;
        maxX = frameMaxX;
        minY = frameMinY;
        maxY = frameMaxY;
        //I think the dynamic scaling being different for x and y is cute and unique but in case I didn't this is a quick fix
        //minX = minY = Math.min(minX,minY);
        //maxX = maxY = Math.max(maxX,maxY);

        batch.setColor(before); // reset after drawing
    }

    private void drawRoad(Batch batch, float x1, float y1, float x2, float y2, float radius) {
        float thickness = 1.5f;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));

        // Adjust the center of the line, accounting for line thickness
        float offsetX = (thickness / 2) * (float)Math.cos(Math.toRadians(angle + 90)) - radius;
        float offsetY = (thickness / 2) * (float)Math.sin(Math.toRadians(angle + 90)) - radius;

        // Move both x1 and y1 by the offset to center the road properly
        batch.setColor(Color.WHITE);
        batch.draw(white, x1 - offsetX, y1 - offsetY, 0, 0, length, thickness, 1f, 1f, angle);
    }
}
