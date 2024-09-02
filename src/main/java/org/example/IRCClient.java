package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class IRCClient {
    private static final String SERVER_ADDRESS = "irc.freenode.net";
    private static final int SERVER_PORT = 6667;
    private static final String NICKNAME = "CCClient";
    private static final String REALNAME = "Coding Challenges Client";

    private static String currentChannel = null;
    private static String currentNickname = NICKNAME;
    private static Map<String, String> userNicknames = new HashMap<>();
    private static boolean isRunning = true;

    public static void main(String[] args) {
        try {

            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);


            out.println("NICK " + currentNickname);
            out.println("USER guest 0 * :" + REALNAME);

            Thread responseHandler = new Thread(() -> {
                try {
                    String line;
                    while (isRunning && (line = in.readLine()) != null) {
                        System.out.println("Server: " + line);
                        if (line.startsWith("PING")) {

                            String pongResponse = "PONG " + line.substring(5);
                            out.println(pongResponse);
                        } else {

                            handleServerMessage(line);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            responseHandler.start();

            while (isRunning) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine();
                if (command.startsWith("/join ")) {
                    String channel = command.substring(6).trim();
                    joinChannel(out, channel);
                } else if (command.startsWith("/part")) {
                    partChannel(out);
                } else if (command.startsWith("/nick ")) {
                    String newNickname = command.substring(6).trim();
                    changeNickname(out, newNickname);
                } else if (command.startsWith("/quit")) {
                    quitIRC(out, command);
                    isRunning = false;
                } else if (currentChannel != null) {
                    sendMessage(out, command);
                } else {
                    System.out.println("You are not in a channel. Join a channel to send messages.");
                }
            }


            scanner.close();
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleServerMessage(String message) {
        if (message.startsWith(":") && message.contains(" JOIN ")) {
            String[] parts = message.split(" ");
            String origin = parts[0].substring(1);
            String channel = parts[2];
            System.out.println(origin + " has joined " + channel);
            if (channel.equals(currentChannel)) {
                System.out.println("You are now in " + channel);
            }
        } else if (message.startsWith(":") && message.contains(" PART ")) {
            String[] parts = message.split(" ");
            String origin = parts[0].substring(1);
            String channel = parts[2];
            System.out.println(origin + " has left " + channel);
            if (channel.equals(currentChannel)) {
                System.out.println("You have left " + channel);
                currentChannel = null;
            }
        } else if (message.startsWith(":") && message.contains(" NICK ")) {
            String[] parts = message.split(" ");
            String origin = parts[0].substring(1);
            String newNickname = parts[2].substring(1);
            String oldNickname = userNicknames.getOrDefault(origin, origin);
            userNicknames.put(origin, newNickname);
            System.out.println(oldNickname + " is now known as " + newNickname);
        } else if (message.startsWith(":") && message.contains(" PRIVMSG ")) {
            String[] parts = message.split(" ");
            String origin = parts[0].substring(1);
            String channel = parts[2];
            String messageText = message.substring(message.indexOf(" :") + 2);

            if (channel.equals(currentChannel)) {
                System.out.println(origin + ": " + messageText);
            }
        } else if (message.startsWith(":") && message.contains(" QUIT ")) {
            String[] parts = message.split(" ");
            String origin = parts[0].substring(1);
            String quitMessage = message.substring(message.indexOf(" :") + 2);
            System.out.println(origin + " has left IRC " + quitMessage);
        }
    }

    private static void joinChannel(PrintWriter out, String channel) {
        if (currentChannel != null) {
            System.out.println("You are already in " + currentChannel + ". Use /part to leave it first.");
        } else {
            out.println("JOIN " + channel);
            currentChannel = channel;
        }
    }

    private static void partChannel(PrintWriter out) {
        if (currentChannel != null) {
            out.println("PART " + currentChannel);
            currentChannel = null;
        } else {
            System.out.println("You are not in any channel.");
        }
    }

    private static void changeNickname(PrintWriter out, String newNickname) {
        if (newNickname.equals(currentNickname)) {
            System.out.println("You are already using that nickname.");
        } else {
            out.println("NICK " + newNickname);
            currentNickname = newNickname;
        }
    }

    private static void sendMessage(PrintWriter out, String message) {
        out.println("PRIVMSG " + currentChannel + " :" + message);
    }

    private static void quitIRC(PrintWriter out, String command) {
        String quitMessage = command.length() > 6 ? command.substring(6).trim() : "Quit";
        out.println("QUIT :" + quitMessage);
        System.out.println("Quitting IRC with message: " + quitMessage);
    }
}
