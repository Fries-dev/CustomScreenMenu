package com.cmenu.ui;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Transforms a TextDisplay's local bounding box into a world-space OBB
 * using its transformation matrix, then checks whether a point lies inside
 * (with a 0.2-block expansion for leniency, adjustable).
 */
public final class TextDisplayHitBox {

    /** Default local width/height; a single-line TextDisplay is ~0.25 blocks tall,
     *  width is estimated linearly based on character count. */
    private static final float LOCAL_HEIGHT = 0.25f;
    private static final float LOCAL_WIDTH_PER_CHAR = 0.15f;

    /**
     * Main entry point: checks whether the cursor is within the text's "true" bounds.
     */
    public static boolean isInside(TextDisplay display, Location cursor) {
        if (!display.isValid()) return false;

        // 1. Estimate local dimensions (width = char count * coefficient, height = fixed single-line value)
        String text = display.getText() != null ? display.getText() : "";
        float localWidth = Math.max(0.1f, text.length() * LOCAL_WIDTH_PER_CHAR);
        float localHeight = LOCAL_HEIGHT;

        // 2. Get the transformation matrix
        Matrix4f mat = matrixFromTransformation(display);

        // 3. Build 8 vertices of the local bounding box centered at the origin
        Vector3f[] localVerts = buildLocalVertices(localWidth, localHeight);

        // 4. Transform vertices to world coordinates
        Vector3f[] worldVerts = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector4f v = new Vector4f(localVerts[i], 1.0f);
            mat.transform(v);
            worldVerts[i] = new Vector3f(v.x, v.y, v.z);
        }

        // 5. Compute the axis-aligned bounding box extents
        float minX = worldVerts[0].x, minY = worldVerts[0].y, minZ = worldVerts[0].z;
        float maxX = worldVerts[0].x, maxY = worldVerts[0].y, maxZ = worldVerts[0].z;

        for (int i = 1; i < 8; i++) {
            minX = Math.min(minX, worldVerts[i].x);
            minY = Math.min(minY, worldVerts[i].y);
            minZ = Math.min(minZ, worldVerts[i].z);
            maxX = Math.max(maxX, worldVerts[i].x);
            maxY = Math.max(maxY, worldVerts[i].y);
            maxZ = Math.max(maxZ, worldVerts[i].z);
        }

        // 6. Use Bukkit's built-in BoundingBox for a quick containment check (with expansion)
        BoundingBox obb = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        obb.expand(0.2);   // Give the player a little leniency

        return obb.contains(cursor.toVector());
    }

    /* ---------------- Internal Utilities ---------------- */

    private static Matrix4f matrixFromTransformation(TextDisplay td) {
        Transformation tr = td.getTransformation();
        Vector3f translation = tr.getTranslation();
        org.joml.Quaternionf leftRot = tr.getLeftRotation();
        Vector3f scale = tr.getScale();

        // Use the TextDisplay's location as the base for translation
        Location location = td.getLocation();
        translation.add((float)location.getX(), (float)location.getY(), (float)location.getZ());

        Matrix4f T = new Matrix4f().translate(translation);
        Matrix4f R = new Matrix4f().rotate(leftRot);
        Matrix4f S = new Matrix4f().scale(scale);

        // World matrix = T * R * S
        return T.mul(R).mul(S);
    }

    private static Vector3f[] buildLocalVertices(float w, float h) {
        float x = w / 2f;
        float y = h / 2f;
        float z = 0.02f; // TextDisplay has virtually no thickness; a small depth is added to avoid division by zero
        return new Vector3f[]{
                new Vector3f(-x, -y, -z), new Vector3f(x, -y, -z),
                new Vector3f(x, y, -z),   new Vector3f(-x, y, -z),
                new Vector3f(-x, -y, z),  new Vector3f(x, -y, z),
                new Vector3f(x, y, z),    new Vector3f(-x, y, z)
        };
    }

    private TextDisplayHitBox() {}
}