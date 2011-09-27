/*
 *  Copyright (C) 2011 Kilian Gaertner
 *  Modified      2011 Domenic Horner
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

package threading;

import org.bukkit.plugin.Plugin;
import backup.Strings;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.bukkit.Server;
import io.FileUtils;
import java.util.Calendar;
import backup.Properties;
import backup.PropertyConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static io.FileUtils.FILE_SEPARATOR;

/**
 * The Task copies and backups the worlds and delete older backups. This task
 * is only runes once in backup and doing all the thread safe options.
 * The PrepareBackupTask and BackupTask are two threads to find a compromiss between
 * security and performance.
 * @author Kilian Gaertner
 */
public class BackupTask implements Runnable, PropertyConstants {

    private final Properties properties;
    private Strings strings;
    private Plugin plugin;
    private final LinkedList<String> worldsToBackup;
    private final Server server;
    private final String backupName;
    public BackupTask (Properties properties, LinkedList<String> worldsToBackup, Server server, String backupName) {
        this.properties = properties;
        this.worldsToBackup = worldsToBackup;
        this.server = server;
        this.backupName = backupName;
        this.plugin = server.getPluginManager().getPlugin("Backup");
        this.strings = new Strings(plugin);
    }

    @Override
    public void run () {
        try {
            backup();
        }
        catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
    // Do the backup
    public void backup() throws Exception {
        
        // Backup directory name
        String backupDirName = properties.getStringProperty(STRING_BACKUP_FOLDER).concat(FILE_SEPARATOR);
        
        //TODO: FINISH FIXING/COMMENTING THIS.
        if (properties.getBooleanProperty(BOOL_SUMMARIZE_CONTENT)) {
            
            if (backupName != null)
                backupDirName = backupDirName.concat("custom").concat(FILE_SEPARATOR).concat(backupName);
            else
                backupDirName = backupDirName.concat(getDate());
            
            File backupDir = new File(backupDirName);
            backupDir.mkdir();
            while (!worldsToBackup.isEmpty()) {
                String worldName = worldsToBackup.removeFirst();
                try {
                    FileUtils.copyDirectory(worldName, backupDirName.concat(FILE_SEPARATOR).concat(worldName));
                } catch(FileNotFoundException ex) {
                    
                } catch (IOException e) {
                    System.out.println(strings.getStringWOPT("errorcreatetemp", worldName));
                    e.printStackTrace(System.out);
                    server.broadcastMessage(strings.getString("backupfailed"));
                }
            }
            if (properties.getBooleanProperty(BOOL_BACKUP_PLUGINS))
                FileUtils.copyDirectory("plugins", backupDirName.concat(FILE_SEPARATOR).concat("plugins"));

            if (properties.getBooleanProperty(BOOL_ZIP)) {
                FileUtils.zipDir(backupDirName, backupDirName);
                FileUtils.deleteDirectory(backupDir);
            }
            
        } else { //single backup
            
            // Prefs
            boolean ShouldZIP = properties.getBooleanProperty(BOOL_ZIP);
            boolean BackupWorlds = properties.getBooleanProperty(BOOL_BACKUP_WORLDS);
            boolean BackupPlugins = properties.getBooleanProperty(BOOL_BACKUP_PLUGINS);
            
            if(BackupWorlds) {
                while (!worldsToBackup.isEmpty()) {
                    String worldName = worldsToBackup.removeFirst();
                    String destDir = backupDirName.concat(FILE_SEPARATOR).concat(worldName).concat("-").concat(getDate());
                    
                    
                    FileUtils.copyDirectory(worldName, destDir);
                    
                    
                    if (ShouldZIP) {
                      FileUtils.zipDir(destDir, destDir);
                      FileUtils.deleteDirectory(new File(destDir));
                    }
                }
            } else {
                System.out.println("world backup is disabled");
            }
            if (BackupPlugins) {
                    String destDir = backupDirName.concat(FILE_SEPARATOR).concat("plugins").concat("-").concat(getDate());
                    FileUtils.copyDirectory("plugins", destDir);
                    if (ShouldZIP) {
                        FileUtils.zipDir(destDir, destDir);
                        FileUtils.deleteDirectory(new File(destDir));
                    }
                
            } else {
                System.out.println("plugin backup is disabled");
            }

        }
        deleteOldBackups();
        finish();
    }

    /**
     * @return String representing the current Date in configured format
     */
    private String getDate () {

        Calendar cal = Calendar.getInstance();
        String formattedDate = new String();
        // Java string (and date) formatting:
        // http://download.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
        try {
            formattedDate = String.format(properties.getStringProperty(STRING_CUSTOM_DATE_FORMAT),cal);
        } catch (Exception e) {
            System.out.println(strings.getString("errordateformat"));
            formattedDate = String.format("%1$td%1$tm%1$tY-%1$tH%1$tM%1$tS", cal);
            System.out.println(e);
        }
        return formattedDate;
    }

    /**
     * Check wether there are more backups as allowed to store. When this case
     * is true, it deletes oldest ones
     */
    private void deleteOldBackups () {
        try {
            File backupDir = new File(properties.getStringProperty(STRING_BACKUP_FOLDER));
           
            // List files
            File[] tempArray = backupDir.listFiles();
            File[] array = new File[tempArray.length - 1];
            for (int i = 0 , j = 0; i < tempArray.length-1 ; ++i) {
                File file = tempArray[i];
                array[j++] = tempArray[i];
            }
            tempArray = array;
            
            final int maxBackups = properties.getIntProperty(INT_MAX_BACKUPS);
            
            // When there are more than the max.
            if (tempArray.length > maxBackups) {
                // Store the to delete backups in a list
                ArrayList<File> backups = new ArrayList<File>(tempArray.length);
                // For this add all backups in the list and remove later the newest ones
                backups.addAll(Arrays.asList(tempArray));

                // the current index of the newest backup
                int maxModifiedIndex;
                // the current time of the newest backup
                long maxModified;

                //remove all newest backups from the list to delete
                for (int i = 0 ; i < maxBackups ; ++i) {
                    maxModifiedIndex = 0;
                    maxModified = backups.get(0).lastModified();
                    for (int j = 1 ; j < backups.size() ; ++j) {
                        File currentFile = backups.get(j);
                        if (currentFile.lastModified() > maxModified) {
                            maxModified = currentFile.lastModified();
                            maxModifiedIndex = j;
                        }
                    }
                    backups.remove(maxModifiedIndex);
                }
                System.out.println(strings.getString("removeold"));
                System.out.println(Arrays.toString(backups.toArray()));
                // this are the oldest backups, so delete them
                for (File backupToDelete : backups)
                    backupToDelete.delete();
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates a temporary Runnable that is running on the main thread by the
     * sceduler to prevent thread problems.
     */
    private void finish() {
        Runnable run = new Runnable() {
            @Override
            public void run () {
                if (properties.getBooleanProperty(BOOL_ACTIVATE_AUTOSAVE))
                    server.dispatchCommand(server.getConsoleSender(), "save-on");
                String completedBackupMessage = strings.getString("backupfinished");
                if (completedBackupMessage != null && !completedBackupMessage.trim().isEmpty()) {
                    server.broadcastMessage(completedBackupMessage);
                    //System.out.println(completedBackupMessage);
                }
            }
        };
        server.getScheduler().scheduleSyncDelayedTask(plugin, run);
    }
}
