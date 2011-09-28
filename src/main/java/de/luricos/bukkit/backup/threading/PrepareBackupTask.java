/*
 *  Backup - CraftBukkit server Backup plugin (continued)
 *  Copyright (C) 2011 Domenic Horner <https://github.com/gamerx/Backup>
 *  Copyright (C) 2011 Lycano <https://github.com/gamerx/Backup>
 *
 *  Backup - CraftBukkit server Backup plugin (original author)
 *  Copyright (C) 2011 Kilian Gaertner <https://github.com/Meldanor/Backup>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.luricos.bukkit.backup.threading;

import de.luricos.bukkit.backup.config.Properties;
import de.luricos.bukkit.backup.config.Strings;
import de.luricos.bukkit.backup.utils.BackupLogger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/**
 * This task is running by a syncronized thread from the sheduler. It prepare
 * everything for the BackupTask. It checks, whether it can run a backup now,
 * stop the autosave, make a server wide save of all player, save all world data
 * from the RAM to the disc and collects finnaly all worlds and directories to
 * backup. If this is done, it create an asyncronized thread, the BackupTask.
 * @author Kilian Gaertner
 * @see BackupTask
 */
public class PrepareBackupTask implements Runnable {

    private final Server server;
    private final Properties properties;
    public Strings strings;
    private String backupName;
    private boolean isManualBackup;
    private Plugin plugin;

    /**
     * The only constructor for the BackupTask.
     * @param server The server where the Task is running on
     * @param properties This must be a loaded PropertiesSystem
     */
    public PrepareBackupTask (Server server, Properties properties) {
        this.server = server;
        this.properties = properties;
        this.plugin = server.getPluginManager().getPlugin("Backup");
        this.strings = new Strings(plugin);
    }

    @Override
    public void run () {

        // Check if we should be doing backup
        boolean backupOnlyWithPlayer = properties.getBooleanProperty("backuponlywithplayer");
        if ((backupOnlyWithPlayer && server.getOnlinePlayers().length > 0) || !backupOnlyWithPlayer || isManualBackup || backupName != null) {
            prepareBackup();
        } else {
            BackupLogger.prettyLog(Level.INFO, false, strings.getStringWOPT("abortedbackup", Integer.toString(properties.getIntProperty("backupinterval") / 1200)));
        }
    }

    protected void prepareBackup() {

        // Inform players backup is about to happen.
        String startBackupMessage = strings.getString("backupstarted");
        if (startBackupMessage != null && !startBackupMessage.trim().isEmpty()) {
            server.broadcastMessage(startBackupMessage);
        }

        // Save to file, and then turn saving off.
        ConsoleCommandSender consoleCommandSender = new ConsoleCommandSender(server);
        server.dispatchCommand(consoleCommandSender, "save-all");
        server.dispatchCommand(consoleCommandSender, "save-off");

        // Save players current values.
        server.savePlayers();

        // Determine if backups should be ZIP'd.
        boolean hasToZIP = properties.getBooleanProperty("zipbackup");

        // Send a message advising that it is disabled.
        if (!hasToZIP)
            System.out.println(strings.getString("zipdisabled"));

        // Create list of worlds to ignore.
        List<String> ignoredWorldNames = getIgnoredWorldNames();
        LinkedList<String> worldsToBackup = null;
        for (World world : server.getWorlds()) {
            if ((world.getName() != null) && (world.getName() != "") && (!ignoredWorldNames.contains(world.getName()))) {
                worldsToBackup.add(world.getName());
            }
        }

        server.getScheduler().scheduleAsyncDelayedTask(plugin, new BackupTask(properties, worldsToBackup, server, backupName));
        backupName = null;
        isManualBackup = false;
    }

    private List<String> getIgnoredWorldNames() {
        List<String> worldNames = Arrays.asList(properties.getStringProperty("skipworlds").split(";"));
        if (worldNames.size() > 0 && !worldNames.isEmpty()) {
            // Log what worlds are disabled.
            BackupLogger.prettyLog(strings.getString("disabledworlds"));
            BackupLogger.prettyLog(worldNames.toString());
        }

        return worldNames;
    }

    public void setBackupName (String backupName) {
        this.backupName = backupName;
    }

    public void setAsManualBackup () {
        this.isManualBackup = true;
    }
}