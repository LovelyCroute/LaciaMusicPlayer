package ovo.baicaijun.laciamusicplayer.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * GUI主题配色 - 模仿粉色现代风格
 */
public class GuiTheme {
    // 主色调 - 粉色系
    public static final int PRIMARY_PINK = 0xFFE91E63;
    public static final int PRIMARY_PINK_LIGHT = 0xFFF48FB1;
    public static final int PRIMARY_PINK_DARK = 0xFFC2185B;
    
    // 背景色
    public static final int BG_DARK = 0xF0202020;
    public static final int BG_PANEL = 0xE6F5F5F5;
    public static final int BG_SIDEBAR = 0xF0FAFAFA;
    public static final int BG_HEADER = 0xFFE91E63;
    
    // 文字颜色
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_DARK = 0xFF333333;
    public static final int TEXT_GRAY = 0xFF888888;
    public static final int TEXT_LIGHT_GRAY = 0xFFAAAAAA;
    public static final int TEXT_PINK = 0xFFE91E63;
    
    // 按钮颜色
    public static final int BTN_NORMAL = 0xFFFFFFFF;
    public static final int BTN_HOVER = 0xFFFCE4EC;
    public static final int BTN_BORDER = 0xFFE91E63;
    public static final int BTN_ACTIVE = 0xFFE91E63;
    
    // 列表项颜色
    public static final int LIST_ITEM_HOVER = 0x33E91E63;
    public static final int LIST_ITEM_SELECTED = 0x66E91E63;
    
    // 进度条颜色
    public static final int PROGRESS_BG = 0xFFE0E0E0;
    public static final int PROGRESS_FILL = 0xFFE91E63;
    
    // 滚动条颜色
    public static final int SCROLLBAR_BG = 0x33000000;
    public static final int SCROLLBAR_THUMB = 0x66E91E63;
    
    // 分割线
    public static final int DIVIDER = 0xFFE0E0E0;
    
    // 阴影
    public static final int SHADOW = 0x33000000;

    /**
     * 绘制平滑圆角矩形（通过多层填充实现更好的圆角效果）
     */
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color, int radius) {
        if (radius <= 0 || width <= 0 || height <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        
        radius = Math.min(radius, Math.min(width, height) / 2);
        
        // 中间主体部分
        context.fill(x + radius, y, x + width - radius, y + height, color);
        // 左侧
        context.fill(x, y + radius, x + radius, y + height - radius, color);
        // 右侧
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
        
        // 绘制四个圆角（使用多层小矩形模拟平滑圆角）
        drawCornerPixels(context, x, y, radius, color, true, true);                    // 左上
        drawCornerPixels(context, x + width - radius, y, radius, color, false, true);  // 右上
        drawCornerPixels(context, x, y + height - radius, radius, color, true, false); // 左下
        drawCornerPixels(context, x + width - radius, y + height - radius, radius, color, false, false); // 右下
    }
    
    /**
     * 绘制圆角像素（基于圆形方程计算）
     */
    private static void drawCornerPixels(DrawContext context, int cornerX, int cornerY, int radius, 
                                          int color, boolean isLeft, boolean isTop) {
        // 圆心位置
        float cx = isLeft ? cornerX + radius : cornerX;
        float cy = isTop ? cornerY + radius : cornerY;
        
        for (int py = 0; py < radius; py++) {
            for (int px = 0; px < radius; px++) {
                // 计算当前像素到圆心的距离
                float dx = isLeft ? (radius - px - 0.5f) : (px + 0.5f);
                float dy = isTop ? (radius - py - 0.5f) : (py + 0.5f);
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                
                // 如果在圆内，绘制像素
                if (dist <= radius) {
                    int drawX = cornerX + px;
                    int drawY = cornerY + py;
                    context.fill(drawX, drawY, drawX + 1, drawY + 1, color);
                }
            }
        }
    }

    /**
     * 绘制带边框的圆角矩形
     */
    public static void drawRoundedRectWithBorder(DrawContext context, int x, int y, int width, int height, 
                                                   int fillColor, int borderColor, int radius) {
        drawRoundedRect(context, x, y, width, height, borderColor, radius);
        drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, fillColor, Math.max(0, radius - 1));
    }

    /**
     * 绘制现代风格按钮
     */
    public static void drawModernButton(DrawContext context, int x, int y, int width, int height, 
                                         String text, boolean hovered, boolean active, 
                                         net.minecraft.client.font.TextRenderer textRenderer) {
        int bgColor, textColor, borderColor;
        
        if (active) {
            bgColor = BTN_ACTIVE;
            textColor = TEXT_WHITE;
            borderColor = PRIMARY_PINK_DARK;
        } else if (hovered) {
            bgColor = BTN_HOVER;
            textColor = TEXT_PINK;
            borderColor = BTN_BORDER;
        } else {
            bgColor = BTN_NORMAL;
            textColor = TEXT_DARK;
            borderColor = DIVIDER;
        }
        
        drawRoundedRectWithBorder(context, x, y, width, height, bgColor, borderColor, 3);
        
        int textX = x + (width - textRenderer.getWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        context.drawText(textRenderer, text, textX, textY, textColor, false);
    }

    /**
     * 绘制粉色边框按钮（空心）
     */
    public static void drawOutlineButton(DrawContext context, int x, int y, int width, int height,
                                          String text, boolean hovered,
                                          net.minecraft.client.font.TextRenderer textRenderer) {
        int bgColor = hovered ? 0x22E91E63 : 0x00000000;
        int borderColor = PRIMARY_PINK;
        int textColor = PRIMARY_PINK;
        
        drawRoundedRectWithBorder(context, x, y, width, height, bgColor, borderColor, 3);
        
        int textX = x + (width - textRenderer.getWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        context.drawText(textRenderer, text, textX, textY, textColor, false);
    }

    /**
     * 绘制进度条
     */
    public static void drawProgressBar(DrawContext context, int x, int y, int width, int height, float progress) {
        drawRoundedRect(context, x, y, width, height, PROGRESS_BG, height / 2);
        int progressWidth = (int) (width * Math.max(0, Math.min(1, progress)));
        if (progressWidth > 0) {
            drawRoundedRect(context, x, y, progressWidth, height, PROGRESS_FILL, height / 2);
        }
    }

    /**
     * 绘制滚动条
     */
    public static void drawScrollbar(DrawContext context, int x, int y, int width, int height,
                                      int scrollOffset, int totalItems, int visibleItems) {
        if (totalItems <= visibleItems) return;
        
        context.fill(x, y, x + width, y + height, SCROLLBAR_BG);
        
        float scrollPercentage = (float) scrollOffset / Math.max(1, totalItems - visibleItems);
        int thumbHeight = Math.max(20, (int) (height * ((float) visibleItems / totalItems)));
        int thumbY = y + (int) (scrollPercentage * (height - thumbHeight));
        
        drawRoundedRect(context, x, thumbY, width, thumbHeight, SCROLLBAR_THUMB, width / 2);
    }

    /**
     * 绘制分割线
     */
    public static void drawDivider(DrawContext context, int x, int y, int width) {
        context.fill(x, y, x + width, y + 1, DIVIDER);
    }

    /**
     * 绘制阴影效果
     */
    public static void drawShadow(DrawContext context, int x, int y, int width, int height) {
        context.fill(x + 2, y + height, x + width + 2, y + height + 2, 0x22000000);
        context.fill(x + width, y + 2, x + width + 2, y + height + 2, 0x22000000);
    }
}
