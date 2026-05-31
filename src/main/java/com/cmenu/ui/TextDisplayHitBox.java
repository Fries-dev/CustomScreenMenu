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
 * 把 TextDisplay 的本地包围盒按变换矩阵转成世界 OBB，
 * 再判断点是否在内（含 0.2 格外延，可调）。
 */
public final class TextDisplayHitBox {

    /** 本地默认宽高；TextDisplay 1 行时大约 0.25 格高，宽度按字符数线性估算 */
    private static final float LOCAL_HEIGHT = 0.25f;
    private static final float LOCAL_WIDTH_PER_CHAR = 0.15f;

    /**
     * 主入口：光标是否在文本的"真实"范围内
     */
    public static boolean isInside(TextDisplay display, Location cursor) {
        if (!display.isValid()) return false;

        // 1. 估算本地尺寸（宽=字符数*系数，高=单行固定值）
        String text = display.getText() != null ? display.getText() : "";
        float localWidth = Math.max(0.1f, text.length() * LOCAL_WIDTH_PER_CHAR);
        float localHeight = LOCAL_HEIGHT;

        // 2. 取变换矩阵
        Matrix4f mat = matrixFromTransformation(display);

        // 3. 构建本地中心在原点的 8 个顶点
        Vector3f[] localVerts = buildLocalVertices(localWidth, localHeight);

        // 4. 转到世界坐标
        Vector3f[] worldVerts = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector4f v = new Vector4f(localVerts[i], 1.0f);
            mat.transform(v);
            worldVerts[i] = new Vector3f(v.x, v.y, v.z);
        }

        // 5. 计算包围盒的边界
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

        // 6. 用 Bukkit 自带的 BoundingBox 快速判断（含外延）
        BoundingBox obb = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        obb.expand(0.2);   // 给玩家一点容错

        return obb.contains(cursor.toVector());
    }

    /* ---------------- 内部工具 ---------------- */

    private static Matrix4f matrixFromTransformation(TextDisplay td) {
        Transformation tr = td.getTransformation();
        Vector3f translation = tr.getTranslation();
        org.joml.Quaternionf leftRot = tr.getLeftRotation();
        Vector3f scale = tr.getScale();

        // 获取TextDisplay的位置作为平移的基础
        Location location = td.getLocation();
        translation.add((float)location.getX(), (float)location.getY(), (float)location.getZ());

        Matrix4f T = new Matrix4f().translate(translation);
        Matrix4f R = new Matrix4f().rotate(leftRot);
        Matrix4f S = new Matrix4f().scale(scale);

        // 世界矩阵 = T * R * S
        return T.mul(R).mul(S);
    }

    private static Vector3f[] buildLocalVertices(float w, float h) {
        float x = w / 2f;
        float y = h / 2f;
        float z = 0.02f; // TextDisplay 几乎无厚度，给一点深度避免除 0
        return new Vector3f[]{
                new Vector3f(-x, -y, -z), new Vector3f(x, -y, -z),
                new Vector3f(x, y, -z),   new Vector3f(-x, y, -z),
                new Vector3f(-x, -y, z),  new Vector3f(x, -y, z),
                new Vector3f(x, y, z),    new Vector3f(-x, y, z)
        };
    }

    private TextDisplayHitBox() {}
}