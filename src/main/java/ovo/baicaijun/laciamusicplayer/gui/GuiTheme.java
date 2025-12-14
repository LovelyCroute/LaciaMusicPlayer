package ovo.baicaijun.laciamusicplayer.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * GUI主题配色 - 模仿粉色现代风格
 */
public class GuiTheme {
    // 主色调 - 粉色系
    public static final int PRIMARY_PINK = 0xFFE91E63;        // 主粉色
    public static final int PRIMARY_PINK_LIGHT = 0xFFF48FB1;  // 浅粉色
    public static final int PRIMARY_PINK_DARK = 0xFFC2185B;   // 深粉色
    
    // 背景色
    public static final int BG_DARK = 0xF0202020;             // 深色背景
    public static final int BG_PANEL = 0xE6F5F5F5;            // 面板背景（浅灰白）
    public static final int BG_SIDEBAR = 0xF0FAFAFA;          // 侧边栏背景
    public static final int BG_HEADER = 0xFFE91E63;           // 顶部栏背景（粉色）
    
    // 文字颜色
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_DARK = 0xFF333333;
    public static final int TEXT_GRAY = 0xFF888888;
    public static final int TEXT_LIGHT_GRAY = 0xFFAAAAAA;
    public static final int TEXT_PINK = 0xFFE91E63;
    
    // 按钮颜色
    public static final int BTN_NORMAL = 0xFFFFFFFF;
    public static final int BTN_HOVER = 0xFFFCE4EC;           // 悬停时浅粉色
    public static final int BTN_BORDER = 0xFFE91E63;
    public static final int BTN_ACTIVE = 0xFFE91E63;
    
    // 列表项颜色
    public static final int LIST_ITEM_HOVER = 0x33E91E63;     // 悬停半透明粉色
    public static final int LIST_ITEM_SELECTED = 0x66E91E63;  // 选中半透明粉色
    
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
     * 绘制圆角矩形（通过多层填充模拟）
     */
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int color, int radius) {
        // 主体
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);
        
        // 四个角的圆角效果（简化版，用小矩形填充）
        if (radius > 0) {
            // 左上角
            context.fill(x + 1, y + 1, x + radius, y + radius, color);
            // 右上角
            context.fill(x + width - radius, y + 1, x + width - 1, y + radius, color);
            // 左下角
            context.fill(x + 1, y + height - radius, x + radius, y + height - 1, color);
            // 右下角
            context.fill(x + width - radius, y + height - radius, x + width - 1, y + height - 1, color);
        }
    }

    /**
     * 绘制带边框的圆角矩形
     */
    public static void drawRoundedRectWithBorder(DrawContext context, int x, int y, int width, int height, 
                                                   int fillColor, int borderColor, int radius) {
        // 先绘制边框（稍大一点）
        drawRoundedRect(context, x, y, width, height, borderColor, radius);
        // 再绘制内部填充
        drawRoundedRect(context, x + 1, y + 1, width - 2, height - 2, fillColor, radius > 0 ? radius - 1 : 0);
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
        // 背景
        drawRoundedRect(context, x, y, width, height, PROGRESS_BG, height / 2);
        // 进度
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
        
        // 背景
        context.fill(x, y, x + width, y + height, SCROLLBAR_BG);
        
        // 滑块
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
        // 底部阴影
        context.fill(x + 2, y + height, x + width + 2, y + height + 2, 0x22000000);
        // 右侧阴影
        context.fill(x + width, y + 2, x + width + 2, y + height + 2, 0x22000000);
    }
}
