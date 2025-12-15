package ovo.baicaijun.laciamusicplayer.gui;

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

    // --- 数据（静态缓存，避免重复加载）---
    private static List<MusicListData> playlists = new ArrayList<>();
    private static List<MusicListData> recommendPlaylists = new ArrayList<>();
    private static List<MusicListData> customPlaylists = new ArrayList<>();  // 手动添加的歌单（刷新时保留）
    private static List<MusicListData> allPlaylists = new ArrayList<>();
    private static List<MusicData> currentSongList = new ArrayList<>();
    private static int selectedPlaylistIndex = -1;
    private static boolean playlistsLoaded = false;

    // --- 缓存 ---
    private static Map<Long, List<MusicData>> playlistSongsCache = new HashMap<>();
    private static Map<Long, List<MusicData>> customPlaylistSongsCache = new HashMap<>();  // 手动添加歌单的歌曲缓存
    private static List<MusicData> dailySongsCache = null;
    private static long lastDailySongsLoadTime = 0;
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
    private boolean addPlaylistBtnHover = false;  // 添加歌单按钮悬停
    private boolean refreshBtnHover = false;      // 刷新按钮悬停
    
    // --- 添加歌单输入状态 ---
    private boolean showAddPlaylistInput = false;
    private String playlistIdInput = "";
    private boolean inputFocused = false;

    // --- 面板尺寸（70%窗口大小）---
    private int getPanelWidth() { return (int)(this.width * 0.7); }
    private int getPanelHeight() { return (int)(this.height * 0.7); }
    private int getPanelX() { return (this.width - getPanelWidth()) / 2; }
    private int getPanelY() { return (this.height - getPanelHeight()) / 2; }

    private static final MusicPlayer musicPlayer = LaciamusicplayerClient.musicPlayer;
    private boolean wasMousePressed = false;

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
        
        if (!playlistsLoaded) {
            loadNeteasePlaylists();
            loadRecommendations();
            playlistsLoaded = true;
        }
        
        if (musicPlayer != null) {
            volume = musicPlayer.getVolume();
        }
    }
    
    public static void clearPlaylistCache() {
        playlists.clear();
        recommendPlaylists.clear();
        // 不清除 customPlaylists 和 customPlaylistSongsCache，保留手动添加的歌单
        allPlaylists.clear();
        currentSongList.clear();
        playlistSongsCache.clear();
        dailySongsCache = null;
        selectedPlaylistIndex = -1;
        playlistsLoaded = false;
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
        if (!customPlaylists.isEmpty()) {
            allPlaylists.add(new MusicListData("── 添加的歌单 ──", -4, ""));
            allPlaylists.addAll(customPlaylists);
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
        // 先检查普通缓存
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
        // 再检查手动添加歌单的缓存
        if (customPlaylistSongsCache.containsKey(playlistId)) {
            List<MusicData> cached = customPlaylistSongsCache.get(playlistId);
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
    
    // 添加歌单功能
    private void addPlaylistById(String idStr) {
        try {
            long playlistId = Long.parseLong(idStr.trim());
            
            // 检查是否已存在
            for (MusicListData p : customPlaylists) {
                if (p.getId() == playlistId) {
                    MessageUtil.sendMessage("§e该歌单已添加过");
                    return;
                }
            }
            
            MessageUtil.sendMessage("§a正在添加歌单...");
            new Thread(() -> {
                try {
                    List<MusicData> songs = NeteaseMusicLoader.getListMusics(playlistId);
                    if (songs != null && !songs.isEmpty()) {
                        // 保存到手动添加歌单的缓存（刷新时不会清除）
                        customPlaylistSongsCache.put(playlistId, songs);
                        if (this.client != null) {
                            this.client.execute(() -> {
                                // 添加到手动添加的歌单列表（刷新时保留）
                                MusicListData newPlaylist = new MusicListData("歌单 " + playlistId, playlistId, "已添加");
                                customPlaylists.add(newPlaylist);
                                rebuildCombinedPlaylists();
                                currentSongList.clear();
                                currentSongList.addAll(songs);
                                rightScrollOffset = 0;
                                MessageUtil.sendMessage("§a成功添加歌单，共 " + songs.size() + " 首歌曲");
                            });
                        }
                    } else {
                        MessageUtil.sendMessage("§c歌单为空或不存在");
                    }
                } catch (Exception e) {
                    MessageUtil.sendMessage("§c添加歌单失败: " + e.getMessage());
                }
            }, "AddPlaylistLoader").start();
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage("§c请输入有效的歌单ID");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        GuiTheme.drawShadow(context, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, GuiTheme.BG_PANEL, 8);
        
        updateHoverState(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
        
        renderHeader(context, panelX, panelY, panelWidth, mouseX, mouseY);
        renderSidebar(context, panelX, panelY + HEADER_HEIGHT, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT);
        renderSongList(context, panelX + SIDEBAR_WIDTH, panelY + HEADER_HEIGHT, 
                       panelWidth - SIDEBAR_WIDTH, panelHeight - HEADER_HEIGHT - BOTTOM_PANEL_HEIGHT);
        renderBottomPanel(context, panelX, panelY + panelHeight - BOTTOM_PANEL_HEIGHT, panelWidth);
        
        // 渲染添加歌单输入框
        if (showAddPlaylistInput) {
            renderAddPlaylistDialog(context, panelX, panelY, panelWidth, panelHeight);
        }
        
        if (allPlaylists.size() <= 1) {
            context.drawCenteredTextWithShadow(this.textRenderer, "正在加载歌单...", 
                    panelX + panelWidth / 2, panelY + panelHeight / 2, GuiTheme.TEXT_GRAY);
        }
    }

    private void renderHeader(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        GuiTheme.drawRoundedRect(context, x, y, width, HEADER_HEIGHT, GuiTheme.BG_HEADER, 8);
        context.fill(x, y + HEADER_HEIGHT - 8, x + width, y + HEADER_HEIGHT, GuiTheme.BG_HEADER);
        
        // 标题
        context.drawText(this.textRenderer, "网易云音乐", x + PADDING, y + (HEADER_HEIGHT - 8) / 2, 
                        GuiTheme.TEXT_WHITE, true);
        
        // 右侧按钮区域
        int btnY = y + 8;
        int btnHeight = 24;
        
        // 添加歌单按钮
        int addBtnX = x + width - 160;
        int addBtnWidth = 70;
        int addBtnColor = addPlaylistBtnHover ? 0xFFFFFFFF : 0xFFFFE0E8;
        GuiTheme.drawRoundedRect(context, addBtnX, btnY, addBtnWidth, btnHeight, addBtnColor, 4);
        int addTextColor = addPlaylistBtnHover ? GuiTheme.PRIMARY_PINK : 0xFFCC4477;
        context.drawText(this.textRenderer, "+ 添加歌单", addBtnX + 6, btnY + 8, addTextColor, false);
        
        // 刷新按钮
        int refreshBtnX = x + width - 80;
        int refreshBtnWidth = 60;
        int refreshBtnColor = refreshBtnHover ? 0xFFFFFFFF : 0xFFFFE0E8;
        GuiTheme.drawRoundedRect(context, refreshBtnX, btnY, refreshBtnWidth, btnHeight, refreshBtnColor, 4);
        int refreshTextColor = refreshBtnHover ? GuiTheme.PRIMARY_PINK : 0xFFCC4477;
        context.drawText(this.textRenderer, "↻ 刷新", refreshBtnX + 8, btnY + 8, refreshTextColor, false);
    }
    
    private void renderAddPlaylistDialog(DrawContext context, int panelX, int panelY, int panelWidth, int panelHeight) {
        // 半透明遮罩
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);
        
        // 对话框
        int dialogWidth = 280;
        int dialogHeight = 120;
        int dialogX = panelX + (panelWidth - dialogWidth) / 2;
        int dialogY = panelY + (panelHeight - dialogHeight) / 2;
        
        GuiTheme.drawRoundedRect(context, dialogX, dialogY, dialogWidth, dialogHeight, 0xFFFFFFFF, 8);
        GuiTheme.drawRoundedRectWithBorder(context, dialogX, dialogY, dialogWidth, dialogHeight, 0xFFFFFFFF, GuiTheme.PRIMARY_PINK, 8);
        
        // 标题
        context.drawText(this.textRenderer, "添加歌单", dialogX + PADDING, dialogY + 12, GuiTheme.TEXT_PINK, false);
        
        // 输入框
        int inputX = dialogX + PADDING;
        int inputY = dialogY + 40;
        int inputWidth = dialogWidth - PADDING * 2;
        int inputHeight = 24;
        int inputBorderColor = inputFocused ? GuiTheme.PRIMARY_PINK : GuiTheme.DIVIDER;
        context.fill(inputX, inputY, inputX + inputWidth, inputY + inputHeight, 0xFFF5F5F5);
        GuiTheme.drawRoundedRectWithBorder(context, inputX, inputY, inputWidth, inputHeight, 0xFFF5F5F5, inputBorderColor, 4);
        
        // 输入文字或提示
        String displayText = playlistIdInput.isEmpty() ? "请输入歌单ID..." : playlistIdInput;
        int textColor = playlistIdInput.isEmpty() ? GuiTheme.TEXT_GRAY : GuiTheme.TEXT_DARK;
        context.drawText(this.textRenderer, displayText, inputX + 6, inputY + 8, textColor, false);
        
        // 光标
        if (inputFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = inputX + 6 + this.textRenderer.getWidth(playlistIdInput);
            context.fill(cursorX, inputY + 6, cursorX + 1, inputY + inputHeight - 6, GuiTheme.TEXT_DARK);
        }
        
        // 按钮
        int btnY = dialogY + 80;
        int btnWidth = 60;
        int btnHeight = 24;
        
        // 确定按钮
        int okBtnX = dialogX + dialogWidth / 2 - btnWidth - 10;
        GuiTheme.drawRoundedRect(context, okBtnX, btnY, btnWidth, btnHeight, GuiTheme.PRIMARY_PINK, 4);
        context.drawText(this.textRenderer, "确定", okBtnX + 20, btnY + 8, GuiTheme.TEXT_WHITE, false);
        
        // 取消按钮
        int cancelBtnX = dialogX + dialogWidth / 2 + 10;
        GuiTheme.drawRoundedRectWithBorder(context, cancelBtnX, btnY, btnWidth, btnHeight, 0xFFFFFFFF, GuiTheme.DIVIDER, 4);
        context.drawText(this.textRenderer, "取消", cancelBtnX + 20, btnY + 8, GuiTheme.TEXT_DARK, false);
    }

    private void renderSidebar(DrawContext context, int x, int y, int height) {
        context.fill(x, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.BG_SIDEBAR);
        context.fill(x + SIDEBAR_WIDTH - 1, y, x + SIDEBAR_WIDTH, y + height, GuiTheme.DIVIDER);
        
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
            
            if (playlist.getId() <= -2) {
                context.drawText(this.textRenderer, playlist.getTitle(), x + PADDING, itemY + 8, GuiTheme.TEXT_GRAY, false);
                continue;
            }
            
            if (isSelected) {
                context.fill(x, itemY, x + 3, itemY + ITEM_HEIGHT - 2, GuiTheme.PRIMARY_PINK);
                GuiTheme.drawRoundedRect(context, x + 4, itemY, SIDEBAR_WIDTH - 8, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_SELECTED, 4);
            } else if (isHovered) {
                GuiTheme.drawRoundedRect(context, x + 4, itemY, SIDEBAR_WIDTH - 8, ITEM_HEIGHT - 2, 
                                         GuiTheme.LIST_ITEM_HOVER, 4);
            }
            
            String icon = playlist.getId() == -1 ? "★" : "♫";
            int textColor = isSelected ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            
            if (playlist.getId() == -1) {
                context.drawText(this.textRenderer, icon, x + PADDING, itemY + 8, 0xFFFFAA00, false);
                context.drawText(this.textRenderer, "每日推荐", x + PADDING + 12, itemY + 8, 0xFFFFAA00, false);
            } else {
                context.drawText(this.textRenderer, icon, x + PADDING, itemY + 8, textColor, false);
                String name = this.textRenderer.trimToWidth(playlist.getTitle(), SIDEBAR_WIDTH - 40);
                context.drawText(this.textRenderer, name, x + PADDING + 12, itemY + 8, textColor, false);
            }
        }
        
        if (allPlaylists.size() > maxVisibleLeftItems) {
            GuiTheme.drawScrollbar(context, x + SIDEBAR_WIDTH - 6, listStartY, 4, availableHeight,
                                   leftScrollOffset, allPlaylists.size(), maxVisibleLeftItems);
        }
    }

    private void renderSongList(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0xFFFFFFFF);
        
        context.drawText(this.textRenderer, "歌曲列表", x + PADDING, y + 8, GuiTheme.TEXT_PINK, false);
        if (!currentSongList.isEmpty()) {
            String countText = "(" + currentSongList.size() + "首)";
            context.drawText(this.textRenderer, countText, x + PADDING + 55, y + 8, GuiTheme.TEXT_GRAY, false);
        }
        GuiTheme.drawDivider(context, x + PADDING, y + 22, width - PADDING * 2);
        
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
            
            String indexStr = String.format("%02d", i + 1);
            int indexColor = isCurrent ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_GRAY;
            context.drawText(this.textRenderer, indexStr, x + PADDING, itemY + 8, indexColor, false);
            
            String title = this.textRenderer.trimToWidth(song.getTitle(), width - 180);
            int titleColor = isCurrent ? GuiTheme.TEXT_PINK : GuiTheme.TEXT_DARK;
            context.drawText(this.textRenderer, title, x + 40, itemY + 8, titleColor, false);
            
            String artist = this.textRenderer.trimToWidth(song.getArtist(), 100);
            context.drawText(this.textRenderer, artist, x + width - 120, itemY + 8, GuiTheme.TEXT_GRAY, false);
        }
        
        if (currentSongList.size() > maxVisibleRightItems) {
            GuiTheme.drawScrollbar(context, x + width - 6, listStartY, 4, availableHeight,
                                   rightScrollOffset, currentSongList.size(), maxVisibleRightItems);
        }
    }

    private void renderBottomPanel(DrawContext context, int x, int y, int width) {
        context.fill(x, y, x + width, y + BOTTOM_PANEL_HEIGHT, 0xFFF8F8F8);
        context.fill(x, y, x + width, y + 1, GuiTheme.DIVIDER);
        
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
        
        int btnSize = 26;
        int btnGap = 6;
        int btnAreaX = x + width / 2 - (btnSize * 5 + btnGap * 4) / 2;
        int btnY = y + 8;
        
        drawControlButton(context, btnAreaX, btnY, btnSize, "◀◀", prevBtnHover);
        String playIcon = (musicPlayer != null && musicPlayer.isPlaying() && !musicPlayer.isPaused()) ? "⏸" : "▶";
        drawControlButton(context, btnAreaX + btnSize + btnGap, btnY, btnSize, playIcon, playBtnHover);
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 2, btnY, btnSize, "▶▶", nextBtnHover);
        
        String modeIcon = "↻";
        if (musicPlayer != null) {
            switch (musicPlayer.getPlaybackMode()) {
                case SINGLE_LOOP: modeIcon = "①"; break;
                case SHUFFLE: modeIcon = "⇄"; break;
                default: modeIcon = "↻"; break;
            }
        }
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 3, btnY, btnSize, modeIcon, modeBtnHover);
        drawControlButton(context, btnAreaX + (btnSize + btnGap) * 4, btnY, btnSize, "词", lyricsBtnHover);
        
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
        addPlaylistBtnHover = false;
        refreshBtnHover = false;
        
        // 顶部按钮悬停检测
        int btnY = panelY + 8;
        int btnHeight = 24;
        int addBtnX = panelX + panelWidth - 160;
        int addBtnWidth = 70;
        int refreshBtnX = panelX + panelWidth - 80;
        int refreshBtnWidth = 60;
        
        addPlaylistBtnHover = isInRect(mouseX, mouseY, addBtnX, btnY, addBtnWidth, btnHeight);
        refreshBtnHover = isInRect(mouseX, mouseY, refreshBtnX, btnY, refreshBtnWidth, btnHeight);
        
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
        int btnSize = 26;
        int btnGap = 6;
        int btnAreaX = panelX + panelWidth / 2 - (btnSize * 5 + btnGap * 4) / 2;
        int ctrlBtnY = bottomY + 8;
        
        prevBtnHover = isInRect(mouseX, mouseY, btnAreaX, ctrlBtnY, btnSize, btnSize);
        playBtnHover = isInRect(mouseX, mouseY, btnAreaX + btnSize + btnGap, ctrlBtnY, btnSize, btnSize);
        nextBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 2, ctrlBtnY, btnSize, btnSize);
        modeBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 3, ctrlBtnY, btnSize, btnSize);
        lyricsBtnHover = isInRect(mouseX, mouseY, btnAreaX + (btnSize + btnGap) * 4, ctrlBtnY, btnSize, btnSize);
    }

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
            
            // 处理键盘输入
            handleKeyInput();
        }
    }

    private boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();
        
        // 处理添加歌单对话框点击
        if (showAddPlaylistInput) {
            int dialogWidth = 280;
            int dialogHeight = 120;
            int dialogX = panelX + (panelWidth - dialogWidth) / 2;
            int dialogY = panelY + (panelHeight - dialogHeight) / 2;
            
            // 输入框点击
            int inputX = dialogX + PADDING;
            int inputY = dialogY + 40;
            int inputWidth = dialogWidth - PADDING * 2;
            int inputHeight = 24;
            if (isInRect((int)mouseX, (int)mouseY, inputX, inputY, inputWidth, inputHeight)) {
                inputFocused = true;
                return true;
            }
            
            // 确定按钮
            int btnY = dialogY + 80;
            int btnWidth = 60;
            int okBtnX = dialogX + dialogWidth / 2 - btnWidth - 10;
            if (isInRect((int)mouseX, (int)mouseY, okBtnX, btnY, btnWidth, 24)) {
                if (!playlistIdInput.isEmpty()) {
                    addPlaylistById(playlistIdInput);
                }
                showAddPlaylistInput = false;
                playlistIdInput = "";
                inputFocused = false;
                return true;
            }
            
            // 取消按钮
            int cancelBtnX = dialogX + dialogWidth / 2 + 10;
            if (isInRect((int)mouseX, (int)mouseY, cancelBtnX, btnY, btnWidth, 24)) {
                showAddPlaylistInput = false;
                playlistIdInput = "";
                inputFocused = false;
                return true;
            }
            
            inputFocused = false;
            return true;
        }
        
        // 添加歌单按钮点击
        if (addPlaylistBtnHover) {
            showAddPlaylistInput = true;
            inputFocused = true;
            playlistIdInput = "";
            return true;
        }
        
        // 刷新按钮点击
        if (refreshBtnHover) {
            clearPlaylistCache();
            loadNeteasePlaylists();
            loadRecommendations();
            playlistsLoaded = true;
            MessageUtil.sendMessage("§a正在刷新歌单...");
            return true;
        }
        
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
                    musicPlayer.pause();
                } else if (musicPlayer.isPaused()) {
                    musicPlayer.webplay();
                } else if (!currentSongList.isEmpty()) {
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
        if (showAddPlaylistInput) return true;
        
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
    
    // 使用tick()中的GLFW检测键盘输入，因为1.21.10的keyPressed签名变了
    private void handleKeyInput() {
        if (this.client == null) return;
        long window = this.client.getWindow().getHandle();
        
        if (showAddPlaylistInput && inputFocused) {
            // 检测Backspace
            if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                if (!backspacePressed && !playlistIdInput.isEmpty()) {
                    playlistIdInput = playlistIdInput.substring(0, playlistIdInput.length() - 1);
                }
                backspacePressed = true;
            } else {
                backspacePressed = false;
            }
            
            // 检测Enter
            if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                if (!enterPressed && !playlistIdInput.isEmpty()) {
                    addPlaylistById(playlistIdInput);
                    showAddPlaylistInput = false;
                    playlistIdInput = "";
                    inputFocused = false;
                }
                enterPressed = true;
            } else {
                enterPressed = false;
            }
            
            // 检测Escape
            if (org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                if (!escapePressed) {
                    showAddPlaylistInput = false;
                    playlistIdInput = "";
                    inputFocused = false;
                }
                escapePressed = true;
            } else {
                escapePressed = false;
            }
            
            // 检测数字键输入
            for (int i = 0; i <= 9; i++) {
                int key = org.lwjgl.glfw.GLFW.GLFW_KEY_0 + i;
                if (org.lwjgl.glfw.GLFW.glfwGetKey(window, key) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                    if (!numberKeysPressed[i] && playlistIdInput.length() < 20) {
                        playlistIdInput += String.valueOf(i);
                    }
                    numberKeysPressed[i] = true;
                } else {
                    numberKeysPressed[i] = false;
                }
            }
        }
    }
    
    private boolean backspacePressed = false;
    private boolean enterPressed = false;
    private boolean escapePressed = false;
    private boolean[] numberKeysPressed = new boolean[10];

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

    private boolean isInRect(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
