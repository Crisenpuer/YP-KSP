package io.github.crisenpuer.tpksp;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.ChannelMessageActionEvent;

import io.github.crisenpuer.tpksp.util.Cmd;
import krpc.client.Connection;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.CrewMember;
import krpc.client.services.SpaceCenter.LaunchSite;
import krpc.client.services.SpaceCenter.Vessel;

public class Main {

    static Properties properties = new Properties();
    static TwitchClient bot;
    static Logger logger;

    static Process ksp;
    static boolean isKspRunning = false;
    static KosClient telnet = new KosClient();
    static Connection connection;
    static SpaceCenter spaceCenter;

    static String selectedLaunchsite = "LaunchPad";

    public static void main(String[] args) {
        // Loading config
        try {
            FileInputStream input = new FileInputStream("config.cfg");
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Creating logger
        logger = Logger.getLogger("main");

        // Reading bot properties from config
        String botToken = properties.getProperty("chatbot.token");
        String botId = properties.getProperty("chatbot.id");
        String cmdPrefix = properties.getProperty("chatbot.prefix");
        String[] channels = properties.getProperty("chatbot.channels").split(",");

        // Bot creation
        bot = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withClientId(botId)
                .withChatAccount(new OAuth2Credential(botId, botToken))
                .build();

        // Joining channels
        for (String channel : channels) {
            bot.getChat().joinChannel(channel);
            bot.getChat().sendMessage(channel, "Bot online.");
        }

        // /me messages manager (kOS commands)
        bot.getEventManager().onEvent(ChannelMessageActionEvent.class, actionEvent -> {
            bot.getChat().sendMessage(actionEvent.getChannel().getName(), kos_command(actionEvent));
        });

        // Prefix commands manager
        bot.getEventManager().onEvent(ChannelMessageEvent.class, event -> {

            Cmd cmd = new Cmd(event, cmdPrefix, "crisenpuer");

            if (cmd.is("ping")) {
                bot.getChat().sendMessage(event.getChannel().getName(), "Pong " + event.getUser().getName() + "!");
            } else if (cmd.is("commands")) {
                bot.getChat().sendMessage(event.getChannel().getName(), "Normal: $commands");
                bot.getChat().sendMessage(event.getChannel().getName(), "TP-KSP: $infopanel");
                bot.getChat().sendMessage(event.getChannel().getName(), "YP-KSP: $cpu, $craftlist, $vessels, $launchsites, $launchsite, $launch");
                bot.getChat().sendMessage(event.getChannel().getName(), "Protected: $startksp, $killksp, $reconnect");
            } else if (cmd.is("infopanel")) {
                bot.getChat().sendMessage(event.getChannel().getName(), "!runscript c20c19fc0e20b28887e00a7e2a87678f");
                String[] responds = { "Nope", "Nobody is here", "Everybody is dead", "Stream is dead" };
                Random random = new Random();
                bot.getChat().sendMessage(event.getChannel().getName(), responds[random.nextInt(responds.length)]);
            } else if (cmd.is("startksp", true)) {
                bot.getChat().sendMessage(event.getChannel().getName(), kos_startksp());
            } else if (cmd.is("killksp", true)) {
                bot.getChat().sendMessage(event.getChannel().getName(), kos_killksp());
            } else if (cmd.is("reconnect", true)) {
                bot.getChat().sendMessage(event.getChannel().getName(), kos_reconnect());
            } else if (cmd.is("cpu")) {
                bot.getChat().sendMessage(event.getChannel().getName(), kos_cpu(event.getMessage(), cmdPrefix));
            } else if (cmd.is("craftlist")) {
                bot.getChat().sendMessage(event.getChannel().getName(), krpc_craftlist());
            } else if (cmd.is("vessels")) {
                List<String> prints = krpc_vessels();
                for (String print :prints) {
                    bot.getChat().sendMessage(event.getChannel().getName(), print);
                }
            } else if (cmd.is("launchsites")) {
                bot.getChat().sendMessage(event.getChannel().getName(), krpc_launchsites());
            } else if (cmd.is("launchsite")) {
                bot.getChat().sendMessage(event.getChannel().getName(), krpc_launchsite(event.getMessage(), cmdPrefix));
            } else if (cmd.is("launchcraft")) {
                bot.getChat().sendMessage(event.getChannel().getName(), krpc_launchcraft(event.getMessage(), cmdPrefix, event.getUser().getName()));
            }
        });
    }

    // Util functions
    private static byte validByte(String msg) {
        try {
            byte value = Byte.parseByte(msg); // Try to parse the string to a byte
            return value; // Parsing succeeded, valid byte
        } catch (NumberFormatException e) {
            return -69; // Parsing failed, not a valid byte
        }
    }

    private static String listToString(List<String> inputList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String item : inputList) {
            stringBuilder.append(item + ", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length() - 1);
        return stringBuilder.toString();
    }

    private static String getStarsString(float experience) {

        StringBuilder output = new StringBuilder();
        float levelPass;

        for (int i = 1; i<=10; i++) {
            levelPass = 2^i;
            if (i>1) { levelPass*=2; }

            if (experience > levelPass) { output.append("★"); } else { break; }
        }
        
        while (output.length() < 10) { output.append("☆"); }
        return output.toString();
    }

    private static String getExpString(float experience) {
        float levelPass = 0;
        float experienceLeft = experience;
        float levelPassLast = 0;
        int level = 0;
        
        for (int i = 1; i<=10; i++) {
            levelPass = 2^i;
            if (i>1) { levelPass*=2; }

            if (experience > levelPass) {
                levelPassLast = levelPass;
                level+=1;
                
            } else {
                experienceLeft -= levelPassLast;
                break;
            }
        }

        if (level == 10) {
            return  "+" + experienceLeft + "xp";
        } else {
            return  levelPass - experience + "xp to next level";
        }
    }

    // kOS-related commands
    private static String kos_startksp() {
        if (isKspRunning && ksp != null && ksp.isAlive()) {
            return "KSP is already running.";
        }
        try {
            String kspPath = properties.getProperty("ksp.path");
            String kspExec = kspPath + properties.getProperty("ksp.exec");

            ProcessBuilder kspBuilder = new ProcessBuilder(kspExec);
            ksp = kspBuilder.start();
            isKspRunning = true; // Set the flag to true when starting
            return "Starting KSP...";
        } catch (Exception e) {
            e.printStackTrace();
            isKspRunning = false; // Reset flag if there's an error
            return "Error: " + e.toString();
        }
    }

    private static String kos_killksp() {
        if (ksp == null || !ksp.isAlive()) {
            isKspRunning = false; // Ensure flag is reset if not running
            return "KSP is not running.";
        }
        try {
            ksp.destroy();
            ksp.waitFor(); // Ensure the process has terminated
            isKspRunning = false; // Reset the flag after killing
            return "Killing KSP...";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    private static String kos_reconnect() {
        try {
            if (telnet.isConnected()) {
                telnet.disconnect();
            }
            telnet.connect("127.0.0.1", 5410, true);
            Thread.sleep(50);
            telnet.selectCpu(1);

            connection = Connection.newInstance("crisenbot");
            spaceCenter = SpaceCenter.newInstance(connection);

            return "Reconnecting..";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    private static String kos_command(ChannelMessageActionEvent event) {
        String user = event.getUser().getName();
        String cmd = event.getMessage().replaceFirst("/me ", "");
        try {
            if (telnet.isConnected()) {
                if (cmd.endsWith(".")) {
                    telnet.sendCommand(cmd + " // " + user);
                } else {
                    telnet.sendCommand(cmd + ". // " + user);
                }
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    private static String kos_cpu(String msg, String prefix) {
        try {
            String cmd = msg.replace(prefix + "cpu ", "");
            byte cpu = validByte(cmd);
            if (cpu < 0) {
                return "Invalid CPU id";
            }
            telnet.selectCpu(cpu);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    // kRPC commands
    private static String krpc_craftlist() {
        if (spaceCenter == null) {
            return "Not connected to kRPC";
        }
        try {
            List<String> craftsList = spaceCenter.launchableVessels("RPC");
            return "Crafts: " + listToString(craftsList);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    private static List<String> krpc_vessels() {
        if (spaceCenter == null) {
            List<String> output = new ArrayList<>();
            output.add("Not connected to kRPC");
            return output;
        }
        try {
            List<Vessel> vesselsList = spaceCenter.getVessels();
            StringBuilder[] stringBuilders = new StringBuilder[10]; // Changed size to 10
            
            for (int i = 0; i < stringBuilders.length; i++) {
                stringBuilders[i] = new StringBuilder(); // Initialize each StringBuilder
            }
    
            for (Vessel vessel : vesselsList) {
                String vesselType = vessel.getType().toString();
                String vesselName = vessel.getName();
                switch (vesselType) {
                    case "SHIP":
                        if (stringBuilders[0].length() == 0) {
                            stringBuilders[0].append("SHIP: ");
                        }
                        stringBuilders[0].append(vesselName).append(", ");
                        break;
                    case "PLANE":
                        if (stringBuilders[1].length() == 0) {
                            stringBuilders[1].append("PLANE: ");
                        }
                        stringBuilders[1].append(vesselName).append(", ");
                        break;
                    case "LANDER":
                        if (stringBuilders[2].length() == 0) {
                            stringBuilders[2].append("LANDER: ");
                        }
                        stringBuilders[2].append(vesselName).append(", ");
                        break;
                    case "ROVER":
                        if (stringBuilders[3].length() == 0) {
                            stringBuilders[3].append("ROVER: ");
                        }
                        stringBuilders[3].append(vesselName).append(", ");
                        break;
                    case "BASE":
                        if (stringBuilders[4].length() == 0) {
                            stringBuilders[4].append("BASE: ");
                        }
                        stringBuilders[4].append(vesselName).append(", ");
                        break;
                    case "PROBE":
                        if (stringBuilders[5].length() == 0) {
                            stringBuilders[5].append("PROBE: ");
                        }
                        stringBuilders[5].append(vesselName).append(", ");
                        break;
                    case "RELAY":
                        if (stringBuilders[6].length() == 0) {
                            stringBuilders[6].append("RELAY: ");
                        }
                        stringBuilders[6].append(vesselName).append(", ");
                        break;
                    case "STATION":
                        if (stringBuilders[7].length() == 0) {
                            stringBuilders[7].append("STATION: ");
                        }
                        stringBuilders[7].append(vesselName).append(", ");
                        break;
                    case "EVA":
                        if (stringBuilders[8].length() == 0) {
                            stringBuilders[8].append("EVA: ");
                        }
                        stringBuilders[8].append(vesselName).append(", ");
                        break;
                    case "DEBRIS": // Fixed spelling
                        if (stringBuilders[9].length() == 0) {
                            stringBuilders[9].append("DEBRIS: ");
                        }
                        stringBuilders[9].append(vesselName).append(", ");
                        break;
                    default:
                        break;
                }
            }
            
            List<String> output = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                // Remove the last comma and space if present
                if (stringBuilders[i].length() > 0) {
                    stringBuilders[i].setLength(stringBuilders[i].length() - 2); // Remove trailing ", "
                    output.add(stringBuilders[i].toString());
                }
            }
    
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            List<String> output = new ArrayList<>();
            output.add("Error: " + e.toString());
            return output;
        }
    }

    private static String krpc_launchsites() {
        if (spaceCenter == null) {
            return "Not connected to kRPC";
        }
        try {
            List<LaunchSite> launchSitesList = spaceCenter.getLaunchSites();
            StringBuilder launchSitesStr = new StringBuilder();
            for (LaunchSite launchSite :launchSitesList) {
                if (launchSitesStr.length() > 0) {
                    launchSitesStr.append(", ");
                }
                launchSitesStr.append(launchSite.getName());
            }

            return launchSitesStr.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }
    
    private static String krpc_launchsite(String msg, String prefix) {
        if (spaceCenter == null) {
            return "Not connected to kRPC";
        }
        try {
            String launchsite = msg.replace(prefix + "launchsite ", "").trim();
            String[] available_launchsites = krpc_launchsites().toLowerCase().split(", ");
            List<String> launchsiteList = Arrays.asList(available_launchsites);
            
            if (launchsiteList.contains(launchsite.toLowerCase())) {
                selectedLaunchsite = launchsite.toLowerCase();
                return "Launchsite changed";
            }
            return "Invalid launchsite. Use !launchsites for available launchsites";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }
    
    private static String krpc_launchcraft(String msg, String prefix, String sender) {
        if (spaceCenter == null) {
            return "Not connected to kRPC";
        }
        try {
            String craftname = msg.replace(prefix + "launchcraft ", "").trim();
            List<String> craftList_old = spaceCenter.launchableVessels("RPC");
            List<String> craftList = new ArrayList<>();
            for (String item :craftList_old) {
                craftList.add(item.toLowerCase());
            }

            if (craftList.contains(craftname.toLowerCase())) {
                List<String> crew = new ArrayList<>();
                crew.add(sender);
                spaceCenter.launchVessel("RPC", craftname, selectedLaunchsite, true, crew, "");
                return "Launching " + craftname + " from " + selectedLaunchsite;
            }
            return "Invalid craft. Use !crafts for available crafts";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.toString();
        }
    }

    private static List<String> krpc_kerbal(String msg, String prefix, String sender) {
        if (spaceCenter == null) {
            List<String> output = new ArrayList<>();
            output.add("Not connected to kRPC");
            return output;
        }
        try {
            List<String> args = List.of(msg.replace(prefix + "kerbal ", "").trim().split(" "));

            if (args.size() < 1) {
                krpc_kerbal("$kerbal " + sender, prefix, sender);
            } else if (args.size() == 1) {
                CrewMember kerbal = spaceCenter.getKerbal(args.get(0));
                if (kerbal != null) {
                    String name = kerbal.getName();
                    String gender = kerbal.getGender().toString();
                    String type = kerbal.getType().toString();
                    int expirence = (int) kerbal.getExperience();
                    String stars = getStarsString(expirence);
                    String exp = getExpString(expirence);
                    String flights = new StringBuilder().append(kerbal.getCareerLogFlights().size()).toString();

                    StringBuilder output = new StringBuilder();
                    output.append("Kerbal ");
                    output.append(name);
                    output.append(": ");
                    output.append(gender);
                    output.append(" ");
                    output.append(type);
                    output.append(" ");
                    output.append(stars);
                    output.append(" ");
                    output.append(exp);
                    output.append("; ");
                    output.append(flights);
                    output.append(" flights");

                    List <String> outputList = new ArrayList<>();
                    outputList.add(output.toString());
                    return outputList;
                } else {
                    List <String> output = new ArrayList<>();
                    output.add("Kerbal not found");
                    if (args.get(0).equalsIgnoreCase(sender)) {
                        output.add("Use \"$kerbal [specialization] [job]\" to create your kerbal. ex. \"$kerbal male pilot\"");
                    }
                    return output;
                }
            } else if (args.size() == 2) {
                List<String> output = new ArrayList<>();
                CrewMember kerbal = spaceCenter.getKerbal(sender);
                if (kerbal != null) {
                    output.add("Your kerbal already exist");
                    return output;
                }
                String reqGender = args.get(0);
                String reqJob = args.get(1);

                List<String> genders = new ArrayList<>();
                genders.add("male"); genders.add("female"); genders.add("m"); genders.add("f");
                List<String> jobs = new ArrayList<>();
                jobs.add("pilot"); jobs.add("engineer"); jobs.add("scientist"); jobs.add("p"); jobs.add("e"); jobs.add("s");

                if (!(genders.contains(reqGender))) {
                    output.add("Invalid gender");
                    return output;
                }
                if (!(jobs.contains(reqJob))) {
                    output.add("Invalid specialization");
                    return output;
                }

            }

            return new ArrayList<>();

        }  catch (Exception e) {
            e.printStackTrace();
            List<String> output = new ArrayList<>();
            output.add("Error: " + e.toString());
            return output;
        }
    }
}