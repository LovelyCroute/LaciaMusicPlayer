package ovo.baicaijun.laciamusicplayer.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ovo.baicaijun.laciamusicplayer.client.LaciamusicplayerClient;
import ovo.baicaijun.laciamusicplayer.music.MusicData;
import ovo.baicaijun.laciamusicplayer.music.MusicListData;
import ovo.baicaijun.laciamusicplayer.music.MusicPlayer;
import ovo.baicaijun.laciamusicplayer.music.netease.NeteaseMusicLoader;
import ovo.baicaijun.laciamusicplayer.util.MessageUtil;

import java.util.*;

/**
 * @author BaicaijunOvO
 * @description 网易云音乐播放器UI - 现代粉色主题风格
 */
public class NeteaseMusicGUI extends Screen {
    // --- 布局常量 ---
    private static final int HEADER_HEIGHT = 40;
    private static final int SIDEBAR_WIDTH = 160;
    private static final int BOTTOM_PANEL_HEIGHT = 70;
    private static final int ITEM_HEIGHT = 28;
    private static final int PADDING = 12;

    // --- 数据 ---
    private List<MusicListData> playlists = new ArrayList<>();
    private List<MusicListData> recommendPlaylists = new ArrayList<>();
    private List<MusicListData> allPlaylists = new ArrayList<>();
    private List<MusicData> currentSongList = new ArrayList<>();
    private int selectedPlaylistIndex = -1;

    // --- 缓存 ---
    private Map<Long, List<MusicData>> playlistSongsCache = new HashMap<>();
    private List<MusicData> dailySongsCache = null;
    private long lastDailySongsLoadTime = 0;
    private static final long DAILY_SONGS_CACHE_TIME = 30 * 60 * 1000;

    // --- 状态 ---
    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;
    private int maxVisibleLeftItems = 0;
    private int maxVisibleRightItems = 0;
    private boolean volumeSliderDragging = false;
    private boolean progressSliderDragging = false;
    private float volume = 0.5f;

    // --- 悬停状态 ---
    private int hoveredPlaylistIndex = -1;
    private int hoveredSongIndex = -1;
    private boolean playBtnHover, pauseBtnHover, prevBtnHover, nextBtnHover;
    private boolean modeBtnHover, lyricsBtnHover;

    // --- 面板尺寸（70%窗口大小）---
    private int getPanelWidth() { return (int)(this.width * 0.7); }
    private int getPanelHeight() { return (int)(this.height * 0.7); }
    private int getPanelX() { return (this.width - getPanelWidth()) / 2; }
    private int getPanelY() { return (this.height - getPanelHeight()) / 2; }

    private static final MusicPlayer musicPlayer = LaciamusicplayerClient.musicPlayer;

    public NeteaseMusicGUI() {
        super(Text.literal("网易云音乐"));
    }

    @Override
    protected void init() {
        super.init();
        if (NeteaseMusicLoader.cookie == null || LaciamusicplayerClient.cookies == null) {
            MessageUtil.sendMessage("请先使用 *music qrcode 登录网易云账号");
            close();
            return;
        }
        loadNeteasePlaylists();
        loadRecommendations();
        if (musicPlayer != null) {
            volume = musicPlayer.getVolume();
        }
    }

    private void rebuildCombinedPlaylists() {
        allPlaylists.clear();
        allPlaylists.add(new MusicListData("★ 每日推荐", -1, "网易云音乐"));
        if (!recommendPlaylists.isEmpty()) {
            allPlaylists.add(new MusicListData("── 推荐歌单 ──", -2, ""));
            allPlaylists.addAll(recommendPlaylists);
        }
        if (!playlists.isEmpty()) {
            allPlaylists.add(new MusicListData("── 我的歌单 ──", -3, ""));
            allPlaylists.addAll(playlists);
        }
    }

    private void loadNeteasePlaylists() {
        playlists.clear();
        currentSongList.clear();
        new Thread(() -> {
            try {
                long userID = NeteaseMusicLoader.getUserID();
                if (userID == 0) {
                    MessageUtil.sendMessage("无法获取用户ID，请检查登录状态");
                    return;
                }
                List<MusicListData> loaded = NeteaseMusicLoader.getMusicList(userID);
                if (loaded != null && !loaded.isEmpty() && this.client != null) {
                    this.client.execute(() -> {
                        playlists.addAll(loaded);
                        rebuildCombinedPlaylists();
                    });
                }
            } catch (Exception e) {
                MessageUtil.sendMessage("加载歌单失败: " + e.getMessage());
            }
        }, "NeteasePlaylistLoader").start();
    }

    private void loadRecommendations() {
        new Thread(() -> {
            try {
                List<MusicListData> loaded = NeteaseMusicLoader.getRecommendMusicList();
                if (loaded != null && !loaded.isEmpty() && this.client != null) {
                    this.client.execute(() -> {
                        recommendPlaylists.addAll(loaded);
                        rebuildCombinedPlaylists();
                    });
                }
            } catch (Exception e) {
                LaciamusicplayerClient.LOGGER.error("加载推荐歌单失败", e);
            }
        }, "RecommendPlaylistLoader").start();
    }

    private void loadDailySongs() {
        long currentTime = System.currentTimeMillis();
        if (dailySongsCache != null && (currentTime - lastDailySongsLoadTime) < DAILY_SONGS_CACHE_TIME) {
            if (this.client != null) {
                this.client.execute(() -> {
                    currentSongList.clear();
                    currentSongList.addAll(dailySongsCache);
                    rightScrollOffset = 0;
                });
            }
            return;
        }
        new Thread(() -> {
            try {
                List<MusicData> songs = NeteaseMusicLoader.getRecommandListMusics();
                if (songs != null && !songs.isEmpty()) {
                    dailySongsCache = songs;
                    lastDailySongsLoadTime = System.currentTimeMillis();
                    if (this.client != null) {
                        this.client.execute(() -> {
                            currentSongList.clear();
                            currentSongList.addAll(songs);
                            rightScrollOffset = 0;
                        });
                    }
                }
            } catch (Exception e) {
                MessageUtil.sendMessage("§c加载每日推荐失败");
            }
        }, "DailySongsLoader").start();
    }

    private void loadPlaylistSongs(long playlistId) {
        if (playlistSongsCache.containsKey(playlistId)) {
            List<MusicData> cached = playlistSongsCache.get(playlistId);
            if (this.client != null) {
                this.client.execute(() -> {
                    currentSongList.clear();
                    currentSongList.addAll(cached);
                    rightScrollOffset = 0;
                });
            }
            return;
        }
        new Thread(() -> {
            try {
                List<MusicData> songs = NeteaseMusicLoader.getListMusics(playlistId);
                if (songs != null && !songs.isEmpty()) {
                    playlistSongsCache.put(playlistId, songs);
                    if (this.client != null) {
                        this.client.execute(() -> {
                            currentSongList.clear();
                            currentSongList.addAll(songs);
                            rightScrollOffset = 0;
                        });
                    }
                }
            } catch (Exception e) {
                LaciamusicplayerClient.LOGGER.error("加载歌单歌曲失败", e);
            }
        }, "PlaylistSongsLoader").start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 背景透明（不绘制遮罩）
        
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 主面板
        GuiTheme.drawShadow(context, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, GuiTheme.BG_PANEL, 8);
        
        updateHoverState(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
        
        renderHeader(context, panelX, panelY, panelWidth);
        renderSidebar(context, panelX, panelY + HEADER_HEIGHT, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT);
        renderSongList(context, panelX + SIDEBAR_WIDTH, panelY + HEADER_HEIGHT, 
                       panelWidth - SIDEBAR_WIDTH, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT);
        renderBottomPanel(context, panelX, panelY + panelHeight - BOTTOM_PANEL_HEIGHT, panelWidth);
        
        if (allPlaylists.size() <= 1) {
            context.drawCenteredTextWithShadow(this.textRenderer, "正在加载歌单...", 
                    panelX + panelWidth / 2, panelY + panelHeight / 2, GuiTheme.TEXT_GRAY);
        }
    }

    private void renderHeader(DrawContext context, int x, int y, int width) {
        // 粉色顶部栏
        GuiTheme.drawRoundedRect(context, x, y, width, HEADER_HEIGHT, GuiTheme.BG_HEADER, 8);
        context.fill(x, y + HEADER_HEIGHT - 8, x + width, y + HEADER_HEIGHT, GuiTheme.BG_HEADER);
        
        // 标题
        context.drawText(this.textRenderer, "网易云音乐", x + PADDING, y + (HEADER_HEIGHT - 8) / 2, 
                        GuiTheme.TEXT_WHITE, true);
    }


    private void renderSidebar(DrawContext context, int x, int y, int height) {
        // 侧边栏背景
        context.fill(x, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.BG_SIDEBAR);
        context.fill(x + SIDEBAR_WIDTH - 1, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.DIVIDER);
        
        // 标题
        context.drawText(this.textRenderer, "歌单列表", x + PADDING, y + 8, GuiTheme.TEXT_PINK, false);
        GuiTheme.drawDivider(context, x + PADDING, y + 22, SIDEBAR_WIDTH - PADDING * 2);
        
        int listStartY = y + 30;
        int availableHeight = height - 40;
        maxVisibleLeftItems = availableHeight / ITEM_HEIGHT;
        
        int maxScroll = Math.max(0, allPlaylists.size() - maxVisibleLeftItems);
        leftScrollOffset = Math.max(0, Math.min(leftScrollOffset, maxScroll));
        
        for (int i = leftScrollOffset; i < Math.min(allPlaylists.size(), leftScrollOffset + maxVisibleLeftItems); i++) {
            MusicListData playlist = allPlaylists.get(i);
            int itemY = listStartY + (i - leftScrollOffset) * ITEM_HEIGHT;
            boolean isHovered = (i == hoveredPlaylistIndex);
            boolean isSelected = (i == selectedPlaylistIndex);
            
            // 分隔线项
            if (playlist.getId() <= -2) {
                context.drawText(this.textRenderer, playlist.getTitle(), x + PADDING, itemY + 8, GuiTheme.TEXT_GRAY, false);
                continue;
            }
            
            // 可点击项
            if (isSelected) {
                context.fill(x, itemY, x + 3, itemY + ITEM_HEIGHT - 2, GuiTheme.PRIMARY_PINK);
                GuiTheme.drawRoundedRect(context, x + 4, itemY, SIDEBAR_WIDTH - 8, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_SELECTED, 4);
            } else if (isHovered) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, SIDEBAR_WIDTH - 8, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_HOVER, 4);
            }
            
            // 图标和文字
            String icon = playlist.getId() == -1 ? "★" : "♫";
            int textColor = isSelected ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            
            if (playlist.getId() == -1) {
                // 每日推荐特殊样式
                context.drawText(this.textRenderer, icon, x + PADDING, itemY + 8, 0xFFFFAA00, false);
                context.drawText(this.textRenderer, "每日推荐", x + PADDING + 12, itemY + 8, 0xFFFFAA00, false);
            } else {
                context.drawText(this.textRenderer, icon, x + PADDING, itemY + 8, textColor, false);
                String name = this.textRenderer.trimToWidth(playlist.getTitle(), SIDEBAR_WIDTH - 40);
                context.drawText(this.textRenderer, name, x + PADDING + 12, itemY + 8, textColor, false);
            }
        }
        
        // 滚动条
        if (allPlaylists.size() > maxVisibleLeftItems) {
            GuiTheme.drawScrollbar(context, x + SIDEBAR_WIDTH - 6, listStartY, 4, availableHeight,
                                   leftScrollOffset, allPlaylists.size(), maxVisibleLeftItems);
        }
    }

    private void renderSongList(DrawContext context, int x, int y, int width, int height) {
        // 列表区域背景
        context.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        
        // 标题栏
        context.drawText(this.textRenderer, "歌曲列表", x + PADDING, y + 8, GuiTheme.TEXT_PINK, false);
        if (!currentSongList.isEmpty()) {
            String countText = "(" + currentSongList.size() + "首)";
            context.drawText(this.textRenderer, countText, x + PADDING + 55, y + 8, GuiTheme.TEXT_GRAY, false);
        }
        GuiTheme.drawDivider(context, x + PADDING, y + 22, width - PADDING * 2);
        
        // 列表头
        int headerY = y + 28;
        context.drawText(this.textRenderer, "#", x + PADDING, headerY, GuiTheme.TEXT_GRAY, false);
        context.drawText(this.textRenderer, "歌曲", x + 40, headerY, GuiTheme.TEXT_GRAY, false);
        context.drawText(this.textRenderer, "歌手", x + width - 120, headerY, GuiTheme.TEXT_GRAY, false);
        GuiTheme.drawDivider(context, x + PADDING, headerY + 12, width - PADDING * 2);
        
        int listStartY = y + 45;
        int availableHeight = height - 55;
        maxVisibleRightItems = availableHeight / ITEM_HEIGHT;
        
        int maxScroll = Math.max(0, currentSongList.size() - maxVisibleRightItems);
        rightScrollOffset = Math.max(0, Math.min(rightScrollOffset, maxScroll));
        
        if (currentSongList.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "选择歌单加载歌曲", 
                    x + width / 2, y + height / 2, GuiTheme.TEXT_GRAY);
            return;
        }
        
        for (int i = rightScrollOffset; i < Math.min(currentSongList.size(), rightScrollOffset + maxVisibleRightItems); i++) {
            MusicData song = currentSongList.get(i);
            int itemY = listStartY + (i - rightScrollOffset) * ITEM_HEIGHT;
            boolean isHovered = (i == hoveredSongIndex);
            boolean isCurrent = musicPlayer != null && musicPlayer.isPlaying() &&
                               musicPlayer.getCurrentMusicData() != null &&
                               musicPlayer.getCurrentMusicData().equals(song);
            
            if (isCurrent) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, width - 12, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_SELECTED, 4);
            } else if (isHovered) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, width - 12, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_HOVER, 4);
            }
            
            // 序号
            String indexStr = String.format("%02d", i + 1);
            int indexColor = isCurrent ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_GRAY;
            context.drawText(this.textRenderer, indexStr, x + PADDING, itemY + 8, indexColor, false);
            
            // 歌曲名
            String title = this.textRenderer.trimToWidth(song.getTitle(), width - 180);
            int titleColor = isCurrent ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            context.drawText(this.textRenderer, title, x + 40, itemY + 8, titleColor, false);
            
            // 歌手
            String artist = this.textRenderer.trimToWidth(song.getArtist(), 100);
            context.drawText(this.textRenderer, artist, x + width - 120, itemY + 8, GuiTheme.TEXT_GRAY, false);
        }
        
        // 滚动条
        if (currentSongList.size() > maxVisibleRightItems) {
            GuiTheme.drawScrollbar(context, x + width - 6, listStartY, 4, availableHeight,
                                   rightScrollOffset, currentSongList.size(), maxVisibleRightItems);
        }
    }

    private void renderBottomPanel(DrawContext context, int x, int y, int width) {
        // 底部面板背景
        context.fill(x, y, x + width, y + BOTTOM_PANEL_HEIGHT, 0xFFF8F8F8);
        context.fill(x, y, x + width, y + 1, GuiTheme.DIVIDER);
        
        // 当前播放信息
        int infoY = y + 8;
        if (musicPlayer != null && musicPlayer.getCurrentMusicData() != null) {
            MusicData current = musicPlayer.getCurrentMusicData();
            String title = this.textRenderer.trimToWidth(current.getTitle(), 180);
            context.drawText(this.textRenderer, title, x + PADDING, infoY, GuiTheme.TEXT_DARK, false);
            String artist = this.textRenderer.trimToWidth(current.getArtist(), 120);
            context.drawText(this.textRenderer, artist, x + PADDING, infoY + 12, GuiTheme.TEXT_GRAY, false);
        } else {
            context.drawText(this.textRenderer, "未播放", x + PADDING, infoY + 6, GuiTheme.TEXT_GRAY, false);
        }
        
        // 控制按钮
        int btnAreaX = x + width / 2 - 100;
        int btnY = y + 8;
        int btnSize = 26;
        int btnGap = 6;
        
        drawControlButton(context, btnAreaX, btnY, btnSize, "◀◀", prevBtnHover);
        String playIcon = (musicPlayer != null && musicPlayer.isPlaying() && !musicPlayer.isPaused()) ? "⏸" : "▶";
        drawControlButton(context, btnAreaX + btnSize + btnGap, btnY, btnSize + 8, playIcon, playBtnHover);
        drawControlButton(context, btnAreaX + btnSize * 2 + btnGap * 2 + 8, btnY, btnSize, "▶▶", nextBtnHover);
        
        String modeIcon = "↻";
        if (musicPlayer != null) {
            switch (musicPlayer.getPlaybackMode()) {
                case SINGLE_LOOP: modeIcon = "①"; break;
                case SHUFFLE: modeIcon = "⇄"; break;
                default: modeIcon = "↻"; break;
            }
        }
        drawControlButton(context, btnAreaX + btnSize * 3 + btnGap * 3 + 8, btnY, btnSize, modeIcon, modeBtnHover);
        drawControlButton(context, btnAreaX + btnSize * 4 + btnGap * 4 + 8, btnY, btnSize, "词", lyricsBtnHover);
        
        // 进度条
        int progressY = y + 45;
        int progressX = x + PADDING;
        int progressWidth = width - PADDING * 2 - 100;
        
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            long duration = musicPlayer.getDuration();
            long elapsed = musicPlayer.getElapsedTime();
            float progress = duration > 0 ? (float) elapsed / duration : 0;
            GuiTheme.drawProgressBar(context, progressX, progressY, progressWidth, 6, progress);
            String timeText = formatTime(elapsed * 1000) + " / " + formatTime(duration * 1000);
            context.drawText(this.textRenderer, timeText, progressX + progressWidth + 8, progressY - 2, 
                            GuiTheme.TEXT_GRAY, false);
        } else {
            GuiTheme.drawProgressBar(context, progressX, progressY, progressWidth, 6, 0);
            context.drawText(this.textRenderer, "00:00 / 00:00", progressX + progressWidth + 8, progressY - 2,
                            GuiTheme.TEXT_GRAY, false);
        }
        
        // 音量
        int volX = x + width - 120;
        int volY = y + 12;
        context.drawText(this.textRenderer, "♪", volX, volY, GuiTheme.TEXT_GRAY, false);
        GuiTheme.drawProgressBar(context, volX + 15, volY + 2, 80, 6, volume);
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
        hoveredPlaylistIndex = -1;
        hoveredSongIndex = -1;
        playBtnHover = pauseBtnHover = prevBtnHover = nextBtnHover = modeBtnHover = lyricsBtnHover = false;
        
        // 侧边栏悬停
        int sidebarY = panelY + HEADER_HEIGHT + 30;
        int sidebarHeight = panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT - 40;
        if (mouseX >= panelX && mouseX < panelX + SIDEBAR_WIDTH && 
            mouseY >= sidebarY && mouseY < sidebarY + sidebarHeight) {
            int relY = mouseY - sidebarY;
            int idx = leftScrollOffset + relY / ITEM_HEIGHT;
            if (idx >= 0 && idx < allPlaylists.size()) {
                MusicListData playlist = allPlaylists.get(idx);
                if (playlist.getId() > -2) {
                    hoveredPlaylistIndex = idx;
                }
            }
        }
        
        // 歌曲列表悬停
        int listX = panelX + SIDEBAR_WIDTH;
        int listY = panelY + HEADER_HEIGHT + 45;
        int listWidth = panelWidth - SIDEBAR_WIDTH;
        int listHeight = panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT - 55;
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int relY = mouseY - listY;
            int idx = rightScrollOffset + relY / ITEM_HEIGHT;
            if (idx >= 0 && idx < currentSongList.size()) {
                hoveredSongIndex = idx;
            }
        }
        
        // 底部按钮悬停
        int bottomY = panelY + panelHeight - BOTTOM_PANEL_HEIGHT;
        int btnAreaX = panelX + panelWidth / 2 - 100;
        int btnY = bottomY + 8;
        int btnSize = 26;
        int btnGap = 6;
        
        prevBtnHover = isInRect(mouseX, mouseY, btnAreaX, btnY, btnSize, btnSize);
        playBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize + btnGap, btnY, btnSize + 8, btnSize);
        nextBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize * 2 + btnGap * 2 + 8, btnY, btnSize, btnSize);
        modeBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize * 3 + btnGap * 3 + 8, btnY, btnSize, btnSize);
        lyricsBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize * 4 + btnGap * 4 + 8, btnY, btnSize, btnSize);
    }

    private boolean wasMousePressed = false;
    
    @Override
    public void tick() {
        super.tick();
        
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
    
    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 歌单点击
        if (hoveredPlaylistIndex >= 0 && hoveredPlaylistIndex < allPlaylists.size()) {
            MusicListData selected = allPlaylists.get(hoveredPlaylistIndex);
            selectedPlaylistIndex = hoveredPlaylistIndex;
            if (selected.getId() == -1) {
                loadDailySongs();
            } else if (selected.getId() > 0) {
                loadPlaylistSongs(selected.getId());
            }
            return true;
        }
        
        // 歌曲点击
        if (hoveredSongIndex >= 0 && hoveredSongIndex < currentSongList.size()) {
            if (musicPlayer != null) {
                musicPlayer.setPlaylistAndPlay(currentSongList, hoveredSongIndex);
            }
            return true;
        }
        
        // 控制按钮
        if (prevBtnHover) {
            if (musicPlayer != null) musicPlayer.playPrevious();
            return true;
        }
        if (playBtnHover) {
            if (musicPlayer != null) {
                if (musicPlayer.isPlaying() && !musicPlayer.isPaused()) {
                    // 正在播放，点击暂停
                    musicPlayer.pause();
                } else if (musicPlayer.isPaused()) {
                    // 已暂停，点击继续播放
                    musicPlayer.webplay();
                } else if (!currentSongList.isEmpty()) {
                    // 未播放，开始播放列表
                    musicPlayer.setPlaylistAndPlay(currentSongList, 0);
                }
            }
            return true;
        }
        if (nextBtnHover) {
            if (musicPlayer != null) musicPlayer.playNext();
            return true;
        }
        if (modeBtnHover) {
            if (musicPlayer != null) musicPlayer.togglePlaybackMode();
            return true;
        }
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
        return handleMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    private boolean handleMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 侧边栏滚动
        if (mouseX >= panelX && mouseX < panelX + SIDEBAR_WIDTH && 
            mouseY >= panelY + HEADER_HEIGHT && mouseY < panelY + panelHeight - BOTTOM_PANEL_HEIGHT) {
            leftScrollOffset = (int) Math.max(0, Math.min(leftScrollOffset - verticalAmount,
                    Math.max(0, allPlaylists.size() - maxVisibleLeftItems)));
            return true;
        }
        
        // 歌曲列表滚动
        if (mouseX >= panelX + SIDEBAR_WIDTH && mouseX < panelX + panelWidth &&
            mouseY >= panelY + HEADER_HEIGHT && mouseY < panelY + panelHeight - BOTTOM_PANEL_HEIGHT) {
            rightScrollOffset = (int) Math.max(0, Math.min(rightScrollOffset - verticalAmount,
                    Math.max(0, currentSongList.size() - maxVisibleRightItems)));
            return true;
        }
        
        return false;
    }

    private void updateVolume(int mouseX, int sliderX) {
        volume = Math.max(0, Math.min(1, (float)(mouseX - sliderX) / 80));
        if (musicPlayer != null) musicPlayer.setVolume(volume);
    }

    private void updateProgress(int mouseX, int progressX, int progressWidth) {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            float progress = Math.max(0, Math.min(1, (float)(mouseX - progressX) / progressWidth));
            long duration = musicPlayer.getDuration();
            musicPlayer.seek((long)(duration * progress));
        }
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

<<<<<<< Updated upstream
    private void drawCustomButton(DrawContext context, int x, int y, int width, int height, String text, boolean hovered) {
        int color = hovered ? 0xFF5555AA : 0xFF444477;
        context.fill(x, y, x + width, y + height, color);
        drawBorder(context, x, y, width, height, 0xFF8888CC);
        context.drawCenteredTextWithShadow(this.textRenderer, text, x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF);
    }

    private void handleSongEnd() {
        if (musicPlayer != null) {
            musicPlayer.playNext();
        }
    }

    // --- 新增鼠标滚轮事件处理 ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < LEFT_PANEL_WIDTH && mouseY < this.height - BOTTOM_PANEL_HEIGHT) {
            // 左侧歌单列表滚动
            leftScrollOffset = (int) Math.max(0, Math.min(leftScrollOffset - verticalAmount,
                    Math.max(0, allPlaylists.size() - maxVisibleLeftItems)));
            return true;
        } else if (mouseX > LEFT_PANEL_WIDTH && mouseY < this.height - BOTTOM_PANEL_HEIGHT) {
            // 右侧歌曲列表滚动
            rightScrollOffset = (int) Math.max(0, Math.min(rightScrollOffset - verticalAmount,
                    Math.max(0, currentSongList.size() - maxVisibleRightItems)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }


    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        // [修复] 将独立的if语句改为if-else if结构，防止穿透
        if (mouseX < LEFT_PANEL_WIDTH && mouseY < this.height - BOTTOM_PANEL_HEIGHT) {
            int startY = 25;
            if (mouseY < startY) {
                return true;
            }
            int relativeY = (int) (mouseY - startY);
            int index = leftScrollOffset + (relativeY / ITEM_HEIGHT);

            if (index >= 0 && index < allPlaylists.size()) {
                MusicListData selected = allPlaylists.get(index);
                if (selected.getId() == -1) {
                    loadDailySongs();
                    selectedPlaylistIndex = index;
                    return true;
                } else if (selected.getId() > 0) {
                    selectedPlaylistIndex = index;
                    selectedPlaylistName = selected.getTitle();
                    loadPlaylistSongs(selected.getId());
                    return true;
                }
            }
        } else if (mouseX > LEFT_PANEL_WIDTH && mouseY < this.height - BOTTOM_PANEL_HEIGHT && !currentSongList.isEmpty()) {
            int startY = 30;
            if (mouseY < startY) {
                return true;
            }
            int relativeY = (int) (mouseY - startY);
            int index = rightScrollOffset + (relativeY / ITEM_HEIGHT);

            if (index >= 0 && index < currentSongList.size()) {
                if (musicPlayer != null) {
                    musicPlayer.setPlaylistAndPlay(currentSongList, index);
                }
            }
            return true;
        } else if (mouseY > this.height - BOTTOM_PANEL_HEIGHT) {
            int buttonY = this.height - BOTTOM_PANEL_HEIGHT + PADDING;
            int centerX = this.width / 2;

            if (playButtonHovered) {
                if (musicPlayer != null) {
                    if (musicPlayer.isPaused()) {
                        musicPlayer.webplay();
                    } else if (!currentSongList.isEmpty()) {
                        musicPlayer.setPlaylistAndPlay(currentSongList, 0);
                    } else {
                        MessageUtil.sendMessage("§c请先选择歌单加载歌曲");
                    }
                }
                return true;
            }
            if (pauseButtonHovered) {
                if (musicPlayer != null) musicPlayer.pause();
                return true;
            }
            if (stopButtonHovered) {
                if (musicPlayer != null) musicPlayer.stop();
                return true;
            }
            if (prevButtonHovered) {
                if (musicPlayer != null) musicPlayer.playPrevious();
                return true;
            }
            if (nextButtonHovered) {
                if (musicPlayer != null) musicPlayer.playNext();
                return true;
            }
            if (modeButtonHovered) {
                if (musicPlayer != null) musicPlayer.togglePlaybackMode();
                return true;
            }
            if (lyricsButtonHovered) {
                LyricRenderer.toggleVisible();
                MessageUtil.sendMessage("§a歌词显示: " +
                        (LyricRenderer.getVisible() ? "开启" : "关闭"));
                return true;
            }

            int sliderX = PADDING + 35;
            int sliderY = this.height - BOTTOM_PANEL_HEIGHT + 40;
            if (isPointInRect((int) mouseX, (int) mouseY, sliderX, sliderY, 150, 10)) {
                volumeSliderDragging = true;
                updateVolumeFromMouseX((int) mouseX);
                return true;
            }

            if (musicPlayer != null && musicPlayer.isPlaying()) {
                int progressX = PADDING;
                int progressY = this.height - BOTTOM_PANEL_HEIGHT + 60;
                if (isPointInRect((int) mouseX, (int) mouseY, progressX, progressY, this.width - PADDING * 2, 8)) {
                    progressSliderDragging = true;
                    updateProgressFromMouseX((int) mouseX);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }



    private void updateProgressFromMouseX(int mouseX) {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            int progressX = PADDING;
            int progressWidth = this.width - PADDING * 2;
            float progress = (float) (mouseX - progressX) / progressWidth;
            progress = Math.max(0, Math.min(1, progress));

            long duration = musicPlayer.getDuration();
            long seekPosition = (long) (duration * progress);
            musicPlayer.seek(seekPosition);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        if (volumeSliderDragging) {
            updateVolumeFromMouseX((int) mouseX);
            return true;
        }
        if (progressSliderDragging) {
            updateProgressFromMouseX((int) mouseX);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }


    @Override
    public boolean mouseReleased(Click click) {
        volumeSliderDragging = false;
        progressSliderDragging = false;
        return super.mouseReleased(click);
    }

    private void updateButtonHoverState(int mouseX, int mouseY) {
        playButtonHovered = false;
        pauseButtonHovered = false;
        stopButtonHovered = false;
        prevButtonHovered = false;
        nextButtonHovered = false;
        modeButtonHovered = false;
        lyricsButtonHovered = false;

        if (mouseY > this.height - BOTTOM_PANEL_HEIGHT) {
            int panelY = this.height - BOTTOM_PANEL_HEIGHT;
            int buttonY = panelY + PADDING;
            int centerX = this.width / 2;

            playButtonHovered = isPointInRect(mouseX, mouseY, centerX - 130, buttonY, 80, 20);
            pauseButtonHovered = isPointInRect(mouseX, mouseY, centerX - 40, buttonY, 80, 20);
            stopButtonHovered = isPointInRect(mouseX, mouseY, centerX + 50, buttonY, 80, 20);
            prevButtonHovered = isPointInRect(mouseX, mouseY, centerX - 180, buttonY, 40, 20);
            nextButtonHovered = isPointInRect(mouseX, mouseY, centerX + 140, buttonY, 40, 20);
            modeButtonHovered = isPointInRect(mouseX, mouseY, centerX + 190, buttonY, 80, 20);
            lyricsButtonHovered = isPointInRect(mouseX, mouseY, centerX + 280, buttonY, 60, 20);
        }

        if (volumeSliderDragging) {
            updateVolumeFromMouseX(mouseX);
        }
        if (progressSliderDragging) {
            updateProgressFromMouseX(mouseX);
        }
    }

    private void updateVolumeFromMouseX(int mouseX) {
        int sliderX = PADDING + 35;
        volume = (float) (mouseX - sliderX) / 150.0f;
        volume = Math.max(0, Math.min(1, volume));
        if (musicPlayer != null) musicPlayer.setVolume(volume);
    }

    private boolean isPointInRect(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight) {
        return x >= rectX && x <= rectX + rectWidth && y >= rectY && y <= rectY + rectHeight;
=======
    private boolean isInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
>>>>>>> Stashed changes
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
