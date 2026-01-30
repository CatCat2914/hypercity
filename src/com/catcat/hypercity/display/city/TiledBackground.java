package com.catcat.hypercity.display.city;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

public class TiledBackground extends Actor {

    private final Texture texture;
    private final Camera camera;

    public TiledBackground(Texture texture, Camera camera) {
        setTouchable(Touchable.disabled);
        this.texture = texture;
        this.camera = camera;

        setSize(Gdx.graphics.getWidth() * 3, Gdx.graphics.getHeight() * 3);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

        float tileWidth = texture.getWidth();
        float tileHeight = texture.getHeight();

        // Offset based on camera position
        float startX = camera.position.x - getWidth() / 2f;
        float startY = camera.position.y - getHeight() / 2f;

        // Important: wrap starting positions
        float offsetX = (startX % tileWidth + tileWidth) % tileWidth;
        float offsetY = (startY % tileHeight + tileHeight) % tileHeight;

        // draw enough tiles to cover the whole area
        for (float x = startX - offsetX; x < startX + getWidth(); x += tileWidth) {
            for (float y = startY - offsetY; y < startY + getHeight(); y += tileHeight) {
                batch.draw(texture, x, y, tileWidth, tileHeight);
            }
        }
    }
}
