package me.elapsed.homedetection;

import lombok.Getter;
import lombok.Setter;
import me.elapsed.homedetection.objects.HouseHoldMember;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HomeDetection extends TimerTask {

    @Getter
    private static HomeDetection instance;

    @Getter @Setter
    private boolean status;

    @Getter
    private ConcurrentHashMap<String, HouseHoldMember> houseHoldMembers;
    private ConcurrentHashMap<TrayIcon, Long> autoDeleteIcon;

    @Getter
    private Image enabledImage, disabledImage;

    protected Executor executor;
    protected SystemTray tray;
    protected Timer timerTask;


    public static void main(String[] args) {

        new HomeDetection().init();
    }

    public void init() {

        System.out.println(
                "██╗  ██╗ ██████╗ ███╗   ███╗███████╗    ██████╗ ███████╗████████╗███████╗ ██████╗████████╗██╗ ██████╗ ███╗   ██╗\n" +
                "██║  ██║██╔═══██╗████╗ ████║██╔════╝    ██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝██║██╔═══██╗████╗  ██║\n" +
                "███████║██║   ██║██╔████╔██║█████╗      ██║  ██║█████╗     ██║   █████╗  ██║        ██║   ██║██║   ██║██╔██╗ ██║\n" +
                "██╔══██║██║   ██║██║╚██╔╝██║██╔══╝      ██║  ██║██╔══╝     ██║   ██╔══╝  ██║        ██║   ██║██║   ██║██║╚██╗██║\n" +
                "██║  ██║╚██████╔╝██║ ╚═╝ ██║███████╗    ██████╔╝███████╗   ██║   ███████╗╚██████╗   ██║   ██║╚██████╔╝██║ ╚████║\n" +
                "╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝    ╚═════╝ ╚══════╝   ╚═╝   ╚══════╝ ╚═════╝   ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝\n"
        );

        System.out.println("──────────────────────────────────────────────────────");

        instance = this;

        status = true;

        // Init the collections and other sh*t...
        this.houseHoldMembers = new ConcurrentHashMap<>();
        this.autoDeleteIcon = new ConcurrentHashMap<>();

        // Init the images
        this.enabledImage = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("icon-enabled.png"));
        this.disabledImage = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("icon-disabled.png"));

        // Setting allowed threads
        this.executor = Executors.newFixedThreadPool(10);
        this.tray = SystemTray.getSystemTray();

        // Init the Timer
        this.timerTask = new Timer();

        // Init the Scheduled Timer
        this.timerTask.scheduleAtFixedRate(this, 1000 * 5, 1000 * 5);

        System.out.println("Loading up house hold members...");
        this.houseHoldMembers.put("192.168.0.3", new HouseHoldMember("Elapsed"));
        this.houseHoldMembers.put("192.168.0.21", new HouseHoldMember("Mom"));
        this.houseHoldMembers.put("192.168.0.14", new HouseHoldMember("Dad"));

        System.out.println("");
        System.out.println("We are looking after [" + this.houseHoldMembers.size() + "] house hold members!");

        displayStartup();
        //scapeLocalNetwork();

        System.out.println("──────────────────────────────────────────────────────");
    }

    public void scapeLocalNetwork() {
        /*
            This is to scrape the local network
            and find what devices are connected
            Pretty nice to debug what devices are what...
         */

        for (int i = 0; i < 256; i++) {

            int atomicInt = i;
            CompletableFuture.runAsync(() -> {
                try {
                    if (InetAddress.getByName("192.168.0." + atomicInt).isReachable(2000)) {
                        System.out.println("192.168.0." + atomicInt + " is active");
                        System.out.println(InetAddress.getByName("192.168.0." + atomicInt));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, executor);

        }

    }

    public void run() {

        // Removing the Tray Icon Notification on a delay!
        for (Map.Entry<TrayIcon, Long> trayIcons : this.autoDeleteIcon.entrySet()) {

            // We are leaving it at 3s
            if (System.currentTimeMillis() - trayIcons.getValue() < 3000) continue;

            // removing it from the System Tray
            this.tray.remove(trayIcons.getKey());
            // remove it from the list
            this.autoDeleteIcon.remove(trayIcons.getKey());
        }

        // Toggle Check
        if (!status) return;

        for (Map.Entry<String, HouseHoldMember> data : this.houseHoldMembers.entrySet()) {
            CompletableFuture.runAsync(() -> {
                try {

                    // The Local IP Addresses
                    String IP = data.getKey();
                    // The House Hold Object
                    HouseHoldMember member = data.getValue();

                    long start = System.currentTimeMillis();

                    // We are checking if the IP is reachable with a 4s Timeout
                    if (InetAddress.getByName(IP).isReachable(3000)){

                        // Lets reset the the disconnect count!
                        member.resetDisconnectCount();

                        // If it's already connected no need to notify again
                        if (member.isConnected()) {
//                            System.out.println("(" + LocalDate.now() + ") " + member.getName() + " Pinged....");
//                            System.out.println("(" + LocalDate.now() + ") Status: ALIVE");
                            return;
                        }

                        // Setting that they are connected
                        member.setConnected(true);
                        // Notifying us that they are connected
                        displayNotification(member);
                        System.out.println("(" + LocalDate.now() + ") " + member.getName() + " connected! Ping (" + (System.currentTimeMillis() - start + "ms)"));
                    } else {

                        // If they are already disconnected no need to notify again
                        if (!member.isConnected()) return;
                        // We add a disconnect count
                        member.addDisconnectCount();
                        // We wanna make sure that they are fully disconnected and do twice a check
                        if (member.getDisconnectCount() != 2) return;
                        // Setting they are fully disconnected
                        member.setConnected(false);
                        // Notify us that they are disconnected
                        displayNotification(member);
                        System.out.println("(" + LocalDate.now() + ") " + member.getName() + " disconnected!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, executor);
        }
    }

    public void displayStartup() {

        System.out.println("");
        System.out.println("Desktop Startup Display has been activated!");

        try {
            // Custom Message
            TrayIcon icon = new TrayIcon(getEnabledImage(), "House Hold Status");
            // Allowing windows to set the autosize
            icon.setImageAutoSize(true);
            // Adding a toggle button method

            icon.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent event) {

                    // If they shift click we disable the program
                    if (event.isShiftDown()) {
                        System.exit(0);
                        return;
                    }

                    // Toggle through the status
                    if (status) {
                        icon.setImage(getDisabledImage());
                        status = false;
                    } else {
                        icon.setImage(getDisabledImage());
                        status = true;
                    }

                }

                @Override
                public void mousePressed(MouseEvent event) {
                }
                @Override
                public void mouseReleased(MouseEvent event) {

                }
                @Override
                public void mouseEntered(MouseEvent event) {

                }
                @Override
                public void mouseExited(MouseEvent event) {
                }
            });

            // Adding the TrayIcon to the System to manage
            tray.add(icon);
            // Display that the program is active
            icon.displayMessage("House Hold is now active", "Watching " + getHouseHoldMembers().size() + " house hold member(s)!", TrayIcon.MessageType.NONE);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void displayNotification(HouseHoldMember member) {
        try {

            TrayIcon trayIcon = new TrayIcon(member.isConnected() ? getEnabledImage() : getDisabledImage(), "House Hold Status");

            trayIcon.setToolTip(member.getName() + (member.isConnected() ? " Arrived Home" : " Left Home"));
            trayIcon.setImageAutoSize(true);

            trayIcon.addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    HomeDetection.getInstance().autoDeleteIcon.remove(trayIcon);
                    tray.remove(trayIcon);
                }

                @Override
                public void mousePressed(MouseEvent e) {}
                @Override
                public void mouseReleased(MouseEvent e) {}
                @Override
                public void mouseEntered(MouseEvent e) {}
                @Override
                public void mouseExited(MouseEvent e) {}
            });

            tray.add(trayIcon);

            trayIcon.displayMessage(member.isConnected() ? "\uD83C\uDFE0 " + member.getName() + " arrived home!" : "\uD83D\uDEAA " + member.getName() + " left home!", "House Hold Status", TrayIcon.MessageType.NONE);

            this.autoDeleteIcon.put(trayIcon, System.currentTimeMillis());
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

}
