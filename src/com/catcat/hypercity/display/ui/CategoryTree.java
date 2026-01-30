package com.catcat.hypercity.display.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.Arrays;

public class CategoryTree extends Tree<CategoryNode, Actor> {

    private final ObjectMap<String, CategoryNode> nodeMap = new ObjectMap<>();
    private final Skin skin;

    public CategoryTree(Skin skin) {
        super(skin);
        this.skin = skin;
    }

    public void addItem(String path, Actor content) {
        String[] parts = path.split("/");
        String fullPath = "";
        CategoryNode parent = null;
        for (int i = 0; i < parts.length; i++) {
            fullPath = (i == 0) ? parts[i] : fullPath + "/" + parts[i];
            CategoryNode node = nodeMap.get(fullPath);

            if (node == null) {
                node = CategoryNode.createLabelNode(parts[i], skin);

                if (parent == null) {
                    add(node); // top-level node
                } else {
                    parent.add(node); // child node
                }

                nodeMap.put(fullPath, node);
            }

            parent = node;
        }

        // Add actual content as a child of the last node
        CategoryNode leaf = CategoryNode.createTableNode((Table) content);
        parent.add(leaf);
    }

    public void removeItem(String path, Actor content) {
        CategoryNode node = nodeMap.get(path);
        if (node == null) return;

        // Find the child node containing this actor
        CategoryNode target = null;
        for (CategoryNode child : new Array.ArrayIterable<>(node.getChildren())) {
            if (child.getActor() == content) { // identity check
                target = child;
                break;
            }
        }

        if (target != null) {
            node.remove(target); // remove from tree
        }

        // Optional: remove empty parent nodes up the path
        cleanupEmptyNodes(path);
    }

    private void cleanupEmptyNodes(String path) {
        String[] parts = path.split("/");

        for (int i = parts.length - 1; i >= 0; i--) {
            String fullPath = String.join("/", Arrays.copyOfRange(parts, 0, i + 1));
            CategoryNode node = nodeMap.get(fullPath);
            if (node == null) continue;

            if (node.getChildren().size == 0) {
                CategoryNode parent = node.getParent();
                if (parent == null) {
                    remove(node);
                } else {
                    parent.remove(node);
                }
                nodeMap.remove(fullPath);
            }
        }
    }

    public void clearNodes(){
        for (CategoryNode root : new ObjectMap.Values<>(nodeMap)) {
            remove(root); // goes through Tree logic
        }
        nodeMap.clear();
    }
    @Override
    public void clear() {
        clearNodes();
        super.clear();
    }

    @Override
    public float getPrefWidth()//idk how to pad stuff
    {
        return super.getPrefWidth()+5f;
    }
}

