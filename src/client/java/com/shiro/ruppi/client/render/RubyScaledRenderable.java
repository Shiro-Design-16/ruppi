package com.shiro.ruppi.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/**
 * 把一个已烘好的字形按 scale 绕 (anchorX, anchorY) 缩放。锚点必须是该字形的笔位：
 * BakedSheetGlyph 的顶点是 (笔位 + left .. 笔位 + right)（BakedSheetGlyph.java:124-127）。
 */
public final class RubyScaledRenderable implements TextRenderable.Styled {
    private final TextRenderable.Styled delegate;
    private final float scale;
    private final float anchorX;
    private final float anchorY;

    public RubyScaledRenderable(final TextRenderable.Styled delegate, final float scale,
                                final float anchorX, final float anchorY) {
        this.delegate = delegate;
        this.scale = scale;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    @Override
    public void render(final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat) {
        // 只缩 x/y、z 原样透传：世界模式（flat=false）下字形各层 z 是 0 / 0.001 / 0.03，
        // 若改用 scaleAround(s, ax, ay, 0) 会把 z 也乘掉（0.03 -> 0.024），注音就会被别处正文盖住。
        Matrix4f scaled = new Matrix4f(pose)
            .translate(this.anchorX, this.anchorY, 0.0F)
            .scale(this.scale, this.scale, 1.0F)
            .translate(-this.anchorX, -this.anchorY, 0.0F);
        this.delegate.render(scaled, buffer, packedLightCoords, flat);
    }

    @Override
    public RenderType renderType(final Font.DisplayMode displayMode) {
        return this.delegate.renderType(displayMode);
    }

    @Override
    public GpuTextureView textureView() {
        return this.delegate.textureView();
    }

    @Override
    public RenderPipeline guiPipeline() {
        return this.delegate.guiPipeline();
    }

    @Override
    public float left() {
        return this.anchorX + (this.delegate.left() - this.anchorX) * this.scale;
    }

    @Override
    public float top() {
        return this.anchorY + (this.delegate.top() - this.anchorY) * this.scale;
    }

    @Override
    public float right() {
        return this.anchorX + (this.delegate.right() - this.anchorX) * this.scale;
    }

    @Override
    public float bottom() {
        return this.anchorY + (this.delegate.bottom() - this.anchorY) * this.scale;
    }

    // GlyphInstance 覆盖了 activeRight（= 笔位 + advance），这里同样按锚点缩放，命中框才跟墨迹一致
    @Override
    public float activeRight() {
        return this.anchorX + (this.delegate.activeRight() - this.anchorX) * this.scale;
    }

    @Override
    public float activeBottom() {
        return this.anchorY + (this.delegate.activeBottom() - this.anchorY) * this.scale;
    }

    @Override
    public Style style() {
        return this.delegate.style();
    }
}
