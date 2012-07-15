package com.bukkitbackup.full.utils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Class used for all logging.
 * 
 * @author Domenic Horner
 */
public class LogUtils {

    private static Plugin plugin;
    private static Logger logger;
    private static boolean logToConsole = true;
    private static String lastMesage;
    
    /**
     * Setup the logger class with required settings.
     * 
     * @param plugin he plugin's object. 
    */
    public static void initLogUtils(Plugin plugin) {
        LogUtils.plugin = plugin;
        LogUtils.logger = Logger.getLogger(plugin.getServer().getLogger().getName() + "." + plugin.getServer().getName());
        lastMesage = null;
    }

    /**
     * Finish initalizing the LogUtils class.
     * 
     * @param logToConsole Whether or not to output to the console.
     */
    public static void finishInitLogUtils(boolean logToConsole) {
        
        // If we should send output to the console.
        LogUtils.logToConsole = logToConsole;
    }

    /**
     * This will send a message to the console using the logger.
     * 
     * @param message The message to be send.
     */
    public static void sendLog(String message) {
        
        // Check if this is a split-message.
        if (message.contains(";;")) {
            
            // Split the message list.
            List<String> messageList = Arrays.asList(message.split(";;"));
            
            // Loop each message in the array.
            for (int i = 0; i < messageList.size(); i++) {
                
                String thisMessage = messageList.get(i);
                
                // Check it should be sent to the console.
                if (logToConsole && !lastMesage.equals(message)) {
                    logger.log(Level.INFO, "[".concat(plugin.getDescription().getName()).concat("] ").concat(thisMessage));
                }
            }
        } else {
            
            // Check it should be sent to the console.
            if (logToConsole && !lastMesage.equals(message)) {
                logger.log(Level.INFO, "[".concat(plugin.getDescription().getName()).concat("] ").concat(message));
            }
        }
        
        // Set the last message so they don't duplicate.
        lastMesage = message;
    }
    
    /**
     * This posts a tidy stack trace to the console, along with a message.
     * 
     * @param ste The Stack Trace object.
     * @param message Message to accompany this exception.
     */
    public static void exceptionLog(Throwable ste, String message) {
        sendLog(message);
        exceptionLog(ste);
    }

    /**
     * This posts a tidy stack trace to the console.
     * 
     * @param ste The Stack Trace object.
     */
    public static void exceptionLog(Throwable ste) {
        sendLog("Please provide following error with support request:");
        ste.printStackTrace(System.out);
    }
}