import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Game {
    private static TrackerInterface tracker;
    private String trackerIp;
    private Integer trackerPort;
    private Integer GridN;
    private Integer TreasureK;
    private String playerID;

    private PlayerInterface currPlayer;
    private Timer timer = new Timer();
    private List<PlayerInterface> playerList = new ArrayList<PlayerInterface>();

    private ReentrantLock lock = new ReentrantLock();

    public PlayerInterface getCurrPlayer() {
        return currPlayer;
    }

    public void setCurrPlayer(PlayerInterface currPlayer) {
        this.currPlayer = currPlayer;
    }
    public static void setTracker(TrackerInterface look_up) {
        Game.tracker = look_up;
    }

    public void setTrackerIp(String trackerIp) {
        this.trackerIp = trackerIp;
    }

    public void setTrackerPort(Integer trackerPort) {
        this.trackerPort = trackerPort;
    }

    public void setGridN(Integer gridN) {
        GridN = gridN;
    }

    public void setTreasureK(Integer treasureK) {
        TreasureK = treasureK;
    }

    public void setPlayerId(String playerID) {
        this.playerID = playerID;
    }

    public static TrackerInterface getTracker() {
        return tracker;
    }

    public String getTrackerIp() {
        return trackerIp;
    }

    public Integer getTrackerPort() {
        return trackerPort;
    }

    public Integer getGridN() {
        return GridN;
    }

    public Integer getTreasureK() {
        return TreasureK;
    }

    public String getPlayerId() {
        return playerID;
    }

    public List<PlayerInterface> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<PlayerInterface> playerList) {
        this.playerList = playerList;
    }

    public Game(String trackerIp, Integer trackerPort) {
        this.trackerIp = trackerIp;
        this.trackerPort = trackerPort;
    }


    public static PlayerInterface findPrimaryServer(List<PlayerInterface> allPlayers) {
        for (PlayerInterface remotePlayer : allPlayers) {
            try {
                PlayerInterface primaryServer = remotePlayer.getPrimaryServer();
                if (primaryServer != null) {
                    return primaryServer;
                }
            } catch (Exception e) {
                System.out.println("Error: " + e);
                continue;
            }
        }
        return null;
    }

    public void pingTask() {

        TimerTask timerTask = (new TimerTask() {
            @Override
            public void run() {

                GameState gameState = null;
                try {
                    gameState = currPlayer.getGameState();
                } catch (Exception e) {
                }
                final String primaryID = gameState.getPrimaryID();
                final String backupID = gameState.getBackupID();
//                System.out.println(String.format("Now primaryID: %s, backupID: %s", primaryID, backupID));
                // System.out.println("backupID: " + backupID);

                if (playerID.equals(primaryID)) {
                    // check all players
                    Set<String> deadPlayers = new HashSet<>();
                    try {
                        for (String pid : currPlayer.getPlayers().keySet()) {
                            try {
                                String ping = currPlayer.getPlayers().get(pid).getPlayerID();
                            } catch (Exception e) {
                                System.out.println("Primary says: ping failed for " + pid);
                                deadPlayers.add(pid);
                            }
                        }
                    } catch (Exception e) {

                    }

                    // remove dead players
                    for (String pid : deadPlayers) {
                        try {
                            currPlayer.removePlayer(pid);
                            tracker.exitGame(pid);
                        } catch (Exception e) {

                        }
                        System.out.println("pid: " + pid);
                    }
                    if (deadPlayers.contains(backupID) || backupID == null) {
                        selectNewBackup();
                    }

                } else if (playerID.equals(backupID)) {
                    // check primary server
                    final int maxRetries = 2;
                    int retryCount = 0;
                    boolean success = false;

                    while (retryCount < maxRetries && !success) {
                        try {
                            String ping = currPlayer.getPrimaryServer().getPlayerID();
                            success = true;  // If the above line doesn't throw an exception, set success to true
                        } catch (Exception e) {
                            System.out.println("Attempt " + (retryCount + 1) + ": Backup says: ping failed for primary " + primaryID);
                            if (++retryCount < maxRetries) {
                                try {
                                    Thread.sleep(50);  // Sleep for 100ms before retrying
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                handlePrimaryCrash(primaryID);
                                break;
                            }
                        }
                    }

                }
            }
        });
        timer.schedule(timerTask, 0, 100);
    }

    public synchronized void handlePrimaryCrash(String oldPrimaryId) {
        // Backup server becomes primary server
        try {
            currPlayer.becomePrimaryServer(currPlayer.getPlayerID());
            tracker.exitGame(oldPrimaryId);
            selectNewBackup();
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public void selectNewBackup() {
        System.out.println("Start to select new backup");
        try {
            currPlayer.selectNewBackup();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    
    public static void main(String[] args) throws MalformedURLException, RemoteException, NotBoundException {
        // Check input arguments
        if(args.length < 3 ) {
            System.err.println("One or more command line options missing");
            System.err.println("Usage:"+"\n"+"java Tracker <IP-address> <port> <player-id>");
            return;
        }
        
        // Get input arguments
        String trackerIp = args[0];
        int trackerPort = Integer.parseInt(args[1]);
        String playerID = args[2];

        // Check player id
        if (playerID.length() != 2) {
            System.out.println("Player ID should be exactly 2 characters long");
            return;
        }

        PlayerInterface player = new Player(playerID);

        // Contact tracker to join game
        Registry registry = LocateRegistry.getRegistry(trackerIp, trackerPort);
        tracker = (TrackerInterface) registry.lookup("Tracker");
        PlayerInterface playerStub = (PlayerInterface) UnicastRemoteObject.exportObject(player, 0);
        List<PlayerInterface> playerList = tracker.joinGame(playerStub);

        Integer N = tracker.getN();
        Integer K = tracker.getK();


        // Create Game Instance
        Game mazeGame = new Game(trackerIp, trackerPort);
        mazeGame.setPlayerList(playerList);
        mazeGame.setGridN(N);
        mazeGame.setTreasureK(K);
        mazeGame.setPlayerId(playerID);
        mazeGame.setTrackerIp(trackerIp);
        mazeGame.setTrackerPort(trackerPort);
        mazeGame.setCurrPlayer(player);
        
        // initialize player identity
        PlayerInterface primaryServer;
        if(playerList.size() == 1){
            playerStub.becomePrimaryServer(playerID);
            primaryServer = playerStub;
            primaryServer.setGameStartTime();

        } else if (playerList.size() == 2){
            primaryServer = findPrimaryServer(playerList);
            playerStub.becomeBackupServer(playerID, primaryServer);
        } else {
            final int maxRetries = 5;
            int retryCount = 0;
            boolean success = false;

            while (retryCount < maxRetries && !success) {
                try {
                    primaryServer = findPrimaryServer(playerList);
                    GameState primaryState = primaryServer.addToGame(playerStub);
                    playerStub.copyState(primaryState);

                    success = true;  // If the above lines don't throw an exception, set success to true
                } catch (Exception e) {
                    System.out.println("Attempt " + (retryCount + 1) + ": Failed to fetch or copy the game state.");
                    retryCount++;

                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(100);  // Sleep for 100ms before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        System.out.println("Exceeded maximum retries. Exiting...");
                        throw e;  // Rethrow the exception or handle it appropriately
                    }
                }
            }

        }

        System.out.println("Player " + playerID + " joined the game");
        System.out.println("Primary server is " + playerStub.getPrimaryID());
        System.out.println("Backup server is " + playerStub.getBackupID());
        GameEngine gameEngine = new GameEngine(playerStub, tracker);

        mazeGame.pingTask();

//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                tracker.exitGame(playerID);
//                primaryServer.removePlayer(playerID);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }));
        
        // Get input from the commend line
        Scanner scanner = new Scanner(System.in);
        String command = "";
        while (scanner.hasNext()) {
            command = scanner.next();
            switch (command) {
                case "0":
                case "1":
                case "2":
                case "3":
                case "4":
                    gameEngine.operateWithRetry(command);
                    break;
                case "9":
                    tracker.exitGame(playerID);
                    player.getPrimaryServer().removePlayer(playerID);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Wrong command");
                    break;
            }
        }
    }
}
