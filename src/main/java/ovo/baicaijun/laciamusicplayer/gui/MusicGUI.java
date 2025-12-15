package ovo.baicaijun.laciamusicplayer.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ovo.baicaijun.laciamusicplayer.client.LaciamusicplayerClient;
import ovo.baicaijun.laciamusicplayer.music.MusicData;
import ovo.baicaijun.laciamusicplayer.music.MusicManager;
import ovo.baicaijun.laciamusicplayer.music.MusicPlayer;
import ovo.baicaijun.laciamusicplayer.util.MessageUtil;

import java.util.*;

/**
 * @author BaicaijunOvO
 * @modified 现代粉色主题风格UI
 */
public class MusicGUI extends Screen {
    // --- 布局常量 ---
    private static final int HEADER_HEIGHT = 40;
    private static final int SIDEBAR_WIDTH = 140;
    private static final int BOTTOM_PANEL_HEIGHT = 70;
    private static final int ITEM_HEIGHT = 24;
    private static final int PADDING = 12;
    private static final int BUTTON_HEIGHT = 24;

    // --- 播放器引用 ---
    private static final MusicPlayer musicPlayer = LaciamusicplayerClient.musicPlayer;

    // --- 数据 ---
    private static final List<String> musicNames = new ArrayList<>();
    private static final HashMap<String, String> musicRealName = new HashMap<>();
    private static int selectedIndex = -1;
    private final Random random = new Random();

    // --- 状态 ---
    private int scrollOffset = 0;
    private int maxVisibleItems = 0;
    private boolean volumeSliderDragging = false;
    private boolean progressSliderDragging = false;
    private float volume = 0.5f;

    // --- 侧边栏菜单项 ---
    private static final String[] MENU_ITEMS = {"全部音乐"};
    private int selectedMenuIndex = 0;

    // --- 按钮悬停状态 ---
    private boolean playBtnHover, pauseBtnHover, prevBtnHover, nextBtnHover;
    private boolean modeBtnHover, lyricsBtnHover;
    private int hoveredMusicIndex = -1;
    private int hoveredMenuIndex = -1;
    
    // --- 鼠标状态 ---
    private boolean wasMousePressed = false;

    // --- 面板尺寸（70%窗口大小）---
    private int getPanelWidth() { return (int)(this.width * 0.7); }
    private int getPanelHeight() { return (int)(this.height * 0.7); }
    private int getPanelX() { return (this.width - getPanelWidth()) / 2; }
    private int getPanelY() { return (this.height - getPanelHeight()) / 2; }

    public MusicGUI() {
        super(Text.literal("LaciaMusicPlayer"));
    }

    public static void playSongByIndex(int index) {
        if (index >= 0 && index < musicNames.size()) {
            selectedIndex = index;
            String musicTitle = musicNames.get(selectedIndex);
            String realName = musicRealName.get(musicTitle);
            MusicData musicData = MusicManager.musics.get(realName);
            if (musicData != null && musicPlayer != null) {
                musicPlayer.load(musicData.getFile(), null, musicData);
                musicPlayer.play();
            }
        }
    }

    public static void playNext() {
        if (musicNames.isEmpty()) return;
        playSongByIndex((selectedIndex + 1) % musicNames.size());
    }

    public static void playPrevious() {
        if (musicNames.isEmpty()) return;
        playSongByIndex((selectedIndex - 1 + musicNames.size()) % musicNames.size());
    }

    @Override
    protected void init() {
        super.init();
        musicNames.clear();
        musicRealName.clear();
        MusicManager.musics.forEach((name, data) -> {
            musicNames.add(data.getTitle());
            musicRealName.put(data.getTitle(), name);
        });
        Collections.sort(musicNames);

        if (musicPlayer != null) {
            volume = musicPlayer.getVolume();
            musicPlayer.setOnSongEndCallback(this::handleSongEnd);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景透明（不绘制遮罩）
        
        // 主面板区域（70%窗口大小，居中）
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 绘制主面板背景（带阴影）
        GuiTheme.drawShadow(context, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, GuiTheme.BG_PANEL, 8);
        
        // 更新悬停状态
        updateHoverState(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
        
        // 渲染各部分
        renderHeader(context, panelX, panelY, panelWidth);
        renderSidebar(context, panelX, panelY + HEADER_HEIGHT, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT);
        renderMusicList(context, panelX + SIDEBAR_WIDTH, panelY + HEADER_HEIGHT, 
                        panelWidth - SIDEBAR_WIDTH, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT, mouseX, mouseY);
        renderBottomPanel(context, panelX, panelY + panelHeight - BOTTOM_PANEL_HEIGHT, panelWidth, mouseX, mouseY);
        
        // 处理拖动
        if (volumeSliderDragging || progressSliderDragging) {
            handleMouseDragged(mouseX, mouseY, 0, 0, 0);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 检测鼠标点击
        if (this.client != null) {
            long window = this.client.getWindow().getHandle();
            boolean leftPressed = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            org.lwjgl.glfw.GLFW.glfwGetCursorPos(window, xpos, ypos);
            double scaleFactor = this.client.getWindow().getScaleFactor();
            double mouseX = xpos[0] / scaleFactor;
            double mouseY = ypos[0] / scaleFactor;
            
            if (leftPressed && !wasMousePressed) {
                handleMouseClicked(mouseX, mouseY, 0);
            }
            if (!leftPressed && wasMousePressed) {
                handleMouseReleased(mouseX, mouseY, 0);
            }
            if (leftPressed) {
                handleMouseDragged(mouseX, mouseY, 0, 0, 0);
            }
            wasMousePressed = leftPressed;
        }
    }

    private void renderHeader(DrawContext context, int x, int y, int width) {
        // 粉色顶部栏
        GuiTheme.drawRoundedRect(context, x, y, width, HEADER_HEIGHT, GuiTheme.BG_HEADER, 8);
        // 底部补齐（因为圆角）
        context.fill(x, y + HEADER_HEIGHT - 8, x + width, y + HEADER_HEIGHT, GuiTheme.BG_HEADER);
        
        // 标题
        context.drawText(this.textRenderer, "LaciaMusicPlayer", x + PADDING, y + (HEADER_HEIGHT - 8) / 2, 
                        GuiTheme.TEXT_WHITE, true);
        
        // 右侧信息
        String info = musicNames.size() + " 首音乐";
        int infoWidth = this.textRenderer.getWidth(info);
        context.drawText(this.textRenderer, info, x + width - infoWidth - PADDING, y + (HEADER_HEIGHT - 8) / 2,
                        GuiTheme.TEXT_WHITE, false);
    }

    private void renderSidebar(DrawContext context, int x, int y, int height) {
        // 侧边栏背景
        context.fill(x, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.BG_SIDEBAR);
        // 右边框
        context.fill(x + SIDEBAR_WIDTH - 1, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.DIVIDER);
        
        int itemY = y + PADDING;
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            boolean isSelected = (i == selectedMenuIndex);
            boolean isHovered = (i == hoveredMenuIndex);
            
            if (isSelected) {
                // 选中状态 - 左边粉色条 + 背景
                context.fill(x, itemY, x + 3, itemY + ITEM_HEIGHT, GuiTheme.PRIMARY_PINK);
                context.fill(x, itemY, x + SIDEBAR_WIDTH - 1, itemY + ITEM_HEIGHT, GuiTheme.LIST_ITEM_SELECTED);
            } else if (isHovered) {
                context.fill(x, itemY, x + SIDEBAR_WIDTH - 1, itemY + ITEM_HEIGHT, GuiTheme.LIST_ITEM_HOVER);
            }
            
            // 菜单图标（简化用符号代替）
            String icon = i == 0 ? "♫" : (i == 1 ? "♪" : "★");
            int textColor = isSelected ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            context.drawText(this.textRenderer, icon, x + PADDING, itemY + (ITEM_HEIGHT - 8) / 2, textColor, false);
            context.drawText(this.textRenderer, MENU_ITEMS[i], x + PADDING + 15, itemY + (ITEM_HEIGHT - 8) / 2, textColor, false);
            
            itemY += ITEM_HEIGHT + 4;
        }
    }

    private void renderMusicList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        // 列表区域背景
        context.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        
        // 计算可见项数
        int listStartY = y + PADDING;
        int availableHeight = height - PADDING * 2;
        maxVisibleItems = availableHeight / ITEM_HEIGHT;
        
        // 确保滚动范围
        int maxScroll = Math.max(0, musicNames.size() - maxVisibleItems);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        if (musicNames.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "暂无音乐", x + width / 2, y + height / 2, GuiTheme.TEXT_GRAY);
            return;
        }
        
        // 渲染列表项
        for (int i = scrollOffset; i < Math.min(musicNames.size(), scrollOffset + maxVisibleItems); i++) {
            int itemY = listStartY + (i - scrollOffset) * ITEM_HEIGHT;
            boolean isSelected = (i == selectedIndex);
            boolean isHovered = (i == hoveredMusicIndex);
            
            // 背景
            if (isSelected) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, width - 12, ITEM_HEIGHT - 2, GuiTheme.LIST_ITEM_SELECTED, 4);
            } else if (isHovered) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, width - 12, ITEM_HEIGHT - 2, GuiTheme.LIST_ITEM_HOVER, 4);
            }
            
            // 序号
            String indexStr = String.valueOf(i + 1);
            context.drawText(this.textRenderer, indexStr, x + PADDING, itemY + (ITEM_HEIGHT - 10) / 2,
                            isSelected ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_GRAY, false);
            
            // 歌曲名
            String displayName = musicNames.get(i);
            String realName = musicRealName.get(displayName);
            MusicData data = MusicManager.musics.get(realName);
            
            displayName = this.textRenderer.trimToWidth(displayName, width - 150);
            int titleColor = isSelected ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            context.drawText(this.textRenderer, displayName, x + 40, itemY + (ITEM_HEIGHT - 10) / 2, titleColor, false);
            
            // 歌手
            if (data != null && data.getArtist() != null) {
                String artist = this.textRenderer.trimToWidth(data.getArtist(), 80);
                context.drawText(this.textRenderer, artist, x + width - 100, itemY + (ITEM_HEIGHT - 10) / 2, 
                                GuiTheme.TEXT_GRAY, false);
            }
        }
        
        // 滚动条
        if (musicNames.size() > maxVisibleItems) {
            GuiTheme.drawScrollbar(context, x + width - 6, listStartY, 4, availableHeight,
                                   scrollOffset, musicNames.size(), maxVisibleItems);
        }
    }

    private void renderBottomPanel(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        // 底部面板背景
        context.fill(x, y, x + width, y + BOTTOM_PANEL_HEIGHT, 0xFFF5F5F5);
        // 顶部分割线
        context.fill(x, y, x + width, y + 1, GuiTheme.DIVIDER);
        
        // 当前播放信息
        int infoY = y + 8;
        if (musicPlayer != null && selectedIndex >= 0 && selectedIndex < musicNames.size()) {
            String title = musicNames.get(selectedIndex);
            String realName = musicRealName.get(title);
            MusicData data = MusicManager.musics.get(realName);
            
            if (data != null) {
                // 歌曲标题
                String displayTitle = this.textRenderer.trimToWidth(data.getTitle(), 200);
                context.drawText(this.textRenderer, displayTitle, x + PADDING, infoY, GuiTheme.TEXT_DARK, false);
                // 歌手
                String artist = this.textRenderer.trimToWidth(data.getArtist(), 150);
                context.drawText(this.textRenderer, artist, x + PADDING, infoY + 12, GuiTheme.TEXT_GRAY, false);
            }
        } else {
            context.drawText(this.textRenderer, "未选择音乐", x + PADDING, infoY + 6, GuiTheme.TEXT_GRAY, false);
        }
        
        // 控制按钮区域（居中）
        int btnSize = 28;
        int btnGap = 8;
        int btnAreaX = x + width / 2 - (btnSize * 5 + btnGap * 4) / 2;
        int btnY = y + 10;
        
        // 上一首
        drawControlButton(context, btnAreaX, btnY, btnSize, "◀◀", prevBtnHover);
        // 播放/暂停
        String playIcon = (musicPlayer != null && musicPlayer.isPlaying() && !musicPlayer.isPaused()) ? "⏸" : "▶";
        drawControlButton(context, btnAreaX + btnSize + btnGap, btnY, btnSize, playIcon, playBtnHover);
        // 下一首
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 2, btnY, btnSize, "▶▶", nextBtnHover);
        
        // 播放模式
        String modeIcon = "↻";
        if (musicPlayer != null) {
            switch (musicPlayer.getPlaybackMode()) {
                case SINGLE_LOOP: modeIcon = "①"; break;
                case SHUFFLE: modeIcon = "⇄"; break;
                default: modeIcon = "↻"; break;
            }
        }
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 3, btnY, btnSize, modeIcon, modeBtnHover);
        
        // 歌词按钮
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 4, btnY, btnSize, "词", lyricsBtnHover);
        
        // 进度条
        int progressY = y + 45;
        int progressX = x + PADDING;
        int progressWidth = width - PADDING * 2 - 100;
        
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            long duration = musicPlayer.getDuration();
            long elapsed = musicPlayer.getElapsedTime();
            float progress = duration > 0 ? (float) elapsed / duration : 0;
            
            GuiTheme.drawProgressBar(context, progressX, progressY, progressWidth, 6, progress);
            
            // 时间显示
            String timeText = formatTime(elapsed * 1000) + " / " + formatTime(duration * 1000);
            context.drawText(this.textRenderer, timeText, progressX + progressWidth + 8, progressY - 2, 
                            GuiTheme.TEXT_GRAY, false);
        } else {
            GuiTheme.drawProgressBar(context, progressX, progressY, progressWidth, 6, 0);
            context.drawText(this.textRenderer, "00:00 / 00:00", progressX + progressWidth + 8, progressY - 2,
                            GuiTheme.TEXT_GRAY, false);
        }
        
        // 音量控制
        int volX = x + width - 120;
        int volY = y + 12;
        context.drawText(this.textRenderer, "♪", volX, volY, GuiTheme.TEXT_GRAY, false);
        GuiTheme.drawProgressBar(context, volX + 15, volY + 2, 80, 6, volume);
        String volText = (int)(volume * 100) + "%";
        context.drawText(this.textRenderer, volText, volX + 100, volY, GuiTheme.TEXT_GRAY, false);
    }

    private void drawControlButton(DrawContext context, int x, int y, int size, String icon, boolean hovered) {
        int bgColor = hovered ? GuiTheme.BTN_HOVER : GuiTheme.BTN_NORMAL;
        int borderColor = hovered ? GuiTheme.PRIMARY_PINK : GuiTheme.DIVIDER;
        
        GuiTheme.drawRoundedRectWithBorder(context, x, y, size, size, bgColor, borderColor, size / 2);
        
        int textColor = hovered ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
        int textX = x + (size - this.textRenderer.getWidth(icon)) / 2;
        int textY = y + (size - 8) / 2;
        context.drawText(this.textRenderer, icon, textX, textY, textColor, false);
    }

    private void updateHoverState(int mouseX, int mouseY, int panelX, int panelY, int panelWidth, int panelHeight) {
        hoveredMusicIndex = -1;
        hoveredMenuIndex = -1;
        playBtnHover = pauseBtnHover = prevBtnHover = nextBtnHover = modeBtnHover = lyricsBtnHover = false;
        
        int sidebarX = panelX;
        int sidebarY = panelY + HEADER_HEIGHT;
        int sidebarHeight = panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT;
        
        // 侧边栏菜单悬停
        if (mouseX >= sidebarX && mouseX < sidebarX + SIDEBAR_WIDTH && 
            mouseY >= sidebarY && mouseY < sidebarY + sidebarHeight) {
            int relY = mouseY - sidebarY - PADDING;
            if (relY >= 0) {
                int idx = relY / (ITEM_HEIGHT + 4);
                if (idx >= 0 && idx < MENU_ITEMS.length) {
                    hoveredMenuIndex = idx;
                }
            }
        }
        
        // 音乐列表悬停
        int listX = panelX + SIDEBAR_WIDTH;
        int listY = panelY + HEADER_HEIGHT + PADDING;
        int listWidth = panelWidth - SIDEBAR_WIDTH;
        int listHeight = panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT - PADDING * 2;
        
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int relY = mouseY - listY;
            int idx = scrollOffset + relY / ITEM_HEIGHT;
            if (idx >= 0 && idx < musicNames.size()) {
                hoveredMusicIndex = idx;
            }
        }
        
        // 底部按钮悬停
        int bottomY = panelY + panelHeight - BOTTOM_PANEL_HEIGHT;
        int btnSize = 28;
        int btnGap = 8;
        int btnAreaX = panelX + panelWidth / 2 - (btnSize * 5 + btnGap * 4) / 2;
        int btnY = bottomY + 10;
        
        prevBtnHover = isInRect(mouseX, mouseY, btnAreaX, btnY, btnSize, btnSize);
        playBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize + btnGap, btnY, btnSize, btnSize);
        nextBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 2, btnY, btnSize, btnSize);
        modeBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 3, btnY, btnSize, btnSize);
        lyricsBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 4, btnY, btnSize, btnSize);
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 侧边栏点击
        if (hoveredMenuIndex >= 0) {
            selectedMenuIndex = hoveredMenuIndex;
            return true;
        }
        
        // 音乐列表点击
        if (hoveredMusicIndex >= 0 && hoveredMusicIndex < musicNames.size()) {
            if (!(selectedIndex == hoveredMusicIndex && musicPlayer.isPlaying())) {
                playSongByIndex(hoveredMusicIndex);
            }
            return true;
        }
        
        // 控制按钮点击
        if (prevBtnHover) { playPrevious(); return true; }
        if (playBtnHover) {
            if (musicPlayer.isPlaying() && !musicPlayer.isPaused()) {
                // 正在播放，点击暂停
                musicPlayer.pause();
            } else if (musicPlayer.isPaused()) {
                // 已暂停，点击继续播放
                musicPlayer.play();
            } else if (selectedIndex >= 0) {
                // 未播放，开始播放选中的歌曲
                musicPlayer.play();
            }
            return true;
        }
        if (nextBtnHover) { playNext(); return true; }
        if (modeBtnHover) { musicPlayer.togglePlaybackMode(); return true; }
        if (lyricsBtnHover) {
            LyricRenderer.toggleVisible();
            MessageUtil.sendMessage("§a歌词显示: " + (LyricRenderer.getVisible() ? "开启" : "关闭"));
            return true;
        }
        
        // 音量滑块
        int volX = panelX + panelWidth - 120 + 15;
        int volY = panelY + panelHeight - BOTTOM_PANEL_HEIGHT + 14;
        if (isInRect((int)mouseX, (int)mouseY, volX, volY, 80, 6)) {
            volumeSliderDragging = true;
            updateVolume((int)mouseX, volX);
            return true;
        }
        
        // 进度条
        int progressX = panelX + PADDING;
        int progressY = panelY + panelHeight - BOTTOM_PANEL_HEIGHT + 45;
        int progressWidth = panelWidth - PADDING * 2 - 100;
        if (isInRect((int)mouseX, (int)mouseY, progressX, progressY, progressWidth, 6)) {
            progressSliderDragging = true;
            updateProgress((int)mouseX, progressX, progressWidth);
            return true;
        }
        
        return false;
    }

    private boolean handleMouseReleased(double mouseX, double mouseY, int button) {
        volumeSliderDragging = false;
        progressSliderDragging = false;
        return false;
    }

    private boolean handleMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        int panelX = getPanelX();
        int panelWidth = getPanelWidth();
        
        if (volumeSliderDragging) {
            int volX = panelX + panelWidth - 120 + 15;
            updateVolume((int)mouseX, volX);
            return true;
        }
        if (progressSliderDragging) {
            int progressX = panelX + PADDING;
            int progressWidth = panelWidth - PADDING * 2 - 100;
            updateProgress((int)mouseX, progressX, progressWidth);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        int listX = panelX + SIDEBAR_WIDTH;
        int listY = panelY + HEADER_HEIGHT;
        int listWidth = panelWidth - SIDEBAR_WIDTH;
        int listHeight = panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT;
        
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            if (verticalAmount > 0) scrollOffset = Math.max(0, scrollOffset - 1);
            else scrollOffset = Math.min(Math.max(0, musicNames.size() - maxVisibleItems), scrollOffset + 1);
            return true;
        }
        return false;
    }

    private void updateVolume(int mouseX, int sliderX) {
        volume = Math.max(0, Math.min(1, (float)(mouseX - sliderX) / 80));
        MusicPlayer.volume = volume;
        if (musicPlayer != null) musicPlayer.setVolume(volume);
    }

    private void updateProgress(int mouseX, int progressX, int progressWidth) {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            float progress = Math.max(0, Math.min(1, (float)(mouseX - progressX) / progressWidth));
            long duration = musicPlayer.getDuration();
            musicPlayer.seek((long)(duration * progress));
        }
    }

    private void handleSongEnd() {
        if (musicPlayer == null || musicNames.isEmpty()) return;
        switch (musicPlayer.getPlaybackMode()) {
            case SINGLE_LOOP: playSongByIndex(selectedIndex); break;
            case SHUFFLE: playSongByIndex(random.nextInt(musicNames.size())); break;
            default: playNext(); break;
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private boolean isInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
