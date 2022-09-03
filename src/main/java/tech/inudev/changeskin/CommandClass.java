package tech.inudev.changeskin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandClass implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reloadskin")) {
            Bukkit.getServer().getOnlinePlayers().forEach(e ->
                    ChangeSkin.getTeamData().forEach((t) -> {
                        ChangeSkin.getInstance().ChangePlayerSkin(e, t.get("team"), t.get("skin"));
                        ChangeSkin.getInstance().getLogger().info(t.get("team") + " " + t.get("skin"));
                    }));
            return true;
        }
        return false;
    }
}
