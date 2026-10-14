package dev.nitro.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Batched through-walls line renderer. Call box()/line() any number of times, then end(). */
public final class Render3D {
	private static BufferBuilder buf;

	private Render3D() {}

	private static BufferBuilder b() {
		if (buf == null) {
			buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
		}
		return buf;
	}

	public static void line(Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
		BufferBuilder v = b();
		v.vertex(m, x1, y1, z1).color(argb);
		v.vertex(m, x2, y2, z2).color(argb);
	}

	public static void box(Matrix4f m, Vec3d cam, Box bx, int argb) {
		float x1 = (float) (bx.minX - cam.x), y1 = (float) (bx.minY - cam.y), z1 = (float) (bx.minZ - cam.z);
		float x2 = (float) (bx.maxX - cam.x), y2 = (float) (bx.maxY - cam.y), z2 = (float) (bx.maxZ - cam.z);
		line(m, x1, y1, z1, x2, y1, z1, argb);
		line(m, x2, y1, z1, x2, y1, z2, argb);
		line(m, x2, y1, z2, x1, y1, z2, argb);
		line(m, x1, y1, z2, x1, y1, z1, argb);
		line(m, x1, y2, z1, x2, y2, z1, argb);
		line(m, x2, y2, z1, x2, y2, z2, argb);
		line(m, x2, y2, z2, x1, y2, z2, argb);
		line(m, x1, y2, z2, x1, y2, z1, argb);
		line(m, x1, y1, z1, x1, y2, z1, argb);
		line(m, x2, y1, z1, x2, y2, z1, argb);
		line(m, x2, y1, z2, x2, y2, z2, argb);
		line(m, x1, y1, z2, x1, y2, z2, argb);
	}

	public static void end() {
		if (buf == null) return;
		BuiltBuffer built = buf.endNullable();
		buf = null;
		if (built == null) return;
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
		RenderSystem.lineWidth(2.0f);
		BufferRenderer.drawWithGlobalProgram(built);
		RenderSystem.lineWidth(1.0f);
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
	}
}
