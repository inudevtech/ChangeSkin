package tech.inudev.changeskin;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ChangeSkin extends JavaPlugin implements Listener {

    private String projectId;
    private String downloadUrl;
    private String bucketName;
    private String prefix;
    @Getter
    private static List<Map<String,String>> teamData;
    @Getter
    private static ChangeSkin instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        projectId = config.getString("projectId");
        downloadUrl = config.getString("downloadUrl");
        bucketName = config.getString("bucketName");
        prefix = config.getString("prefix");
        teamData = (List<Map<String, String>>) config.getList("teamData");
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:messagechannel");
        Objects.requireNonNull(getCommand("reloadskin")).setExecutor(new CommandClass());
    }

    @EventHandler
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent e) {
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            teamData.forEach((t) -> ChangePlayerSkin(e.getPlayer(), t.get("team"), t.get("skin")));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, "sr:messagechannel");
    }

    public void ChangePlayerSkin(Player player, String team, String baseUrl) {
        try {
            getLogger().info(String.valueOf(Objects.requireNonNull(player.getScoreboard().getTeam(team)).getEntries()));
            if (Objects.requireNonNull(player.getScoreboard().getTeam(team)).getEntries().contains(player.getName())) {
                BufferedImage img1 = ImageIO.read(new URL("https://crafatar.com/skins/" + player.getUniqueId()));
                BufferedImage img2 = ImageIO.read(new URL(baseUrl));
                BufferedImage user_skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                img1 = img1.getSubimage(0, 0, 64, 32);
                Graphics g = user_skin.getGraphics();
                g.drawImage(img1, 0, 0, null);
                g.drawImage(img2, 0, 16, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(user_skin, "png", baos);
                baos.flush();

                Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(GoogleCredentials.fromStream(new
                        FileInputStream(getDataFolder().getAbsolutePath()+"/key.json"))).build().getService();
                BlobId blobId = BlobId.of(bucketName, prefix+"/"+player.getUniqueId() +".png");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/png").build();
                try (WriteChannel writer = storage.writer(blobInfo)) {
                    writer.write(ByteBuffer.wrap(baos.toByteArray()));
                }
                baos.close();
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(bytes);

                    out.writeUTF("setSkin");
                    out.writeUTF(player.getName());
                    out.writeUTF(downloadUrl + "/"+prefix+"/"+player.getUniqueId() + ".png");

                    player.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
