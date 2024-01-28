import java.rmi.RemoteException;
import java.util.*;

public class GameEngine {
    PlayerInterface currPlayer;
    String playerID;
    Timer timer = new Timer();
    GUI gui;
    private String command;
    TrackerInterface tracker;

    // Getters and setters
    public GameState getGameState() {
        try {
            return currPlayer.getGameState();
        } catch (Exception e) {
            return null;
        }

    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    // Constructor methods for GameEngine
    public GameEngine(PlayerInterface player, TrackerInterface tracker) {
        this.currPlayer = player;
        try {
            this.playerID = player.getPlayerID();
//            System.out.println(this.playerID);
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        this.tracker = tracker;
        gui = new GUI(currPlayer, playerID);
    }

//    public synchronized void operate(String input) {
//        this.command = input; // Just added
//
//        try {
//            GameState gameState = currPlayer.getPrimaryServer().performAction(playerID, command, Player.ActionType.WRITE);
//            copyState(gameState);
//            gui.update(gameState);
//        } catch (Exception e) {
//            try {
//                System.out.println("Primary " + currPlayer.getPrimaryID() +  " performAction fail");
//                e.printStackTrace();
//                GameState gameState = currPlayer.getBackupServer().performAction(playerID, command, Player.ActionType.WRITE);
//                copyState(gameState);
//                gui.update(gameState);
//            } catch (RemoteException ex) {
//                try {
//                    System.out.println("Backup " + currPlayer.getBackupID() + " performAction fail");
//                } catch (RemoteException exc) {
//                    exc.printStackTrace();
//                }
//                e.printStackTrace();
//                throw new RuntimeException(ex);
//            }
//
//        }
//
//    }
    public synchronized void operateWithRetry(String input) {
        this.command = input; // Just added

        final int maxRetries = 10;
        int retryCount = 0;
        boolean success = false;
        System.out.println("Player move " + playerID + " " + command);

        while (retryCount < maxRetries && !success) {
            try {
                GameState gameState = currPlayer.getPrimaryServer().performAction(playerID, command, Player.ActionType.WRITE);
                copyState(gameState);
                gui.update(gameState);
                success = true;
            } catch (Exception e) {
                try {
                    System.out.println("Primary " + currPlayer.getPrimaryID() + " performAction fail");
                    PlayerInterface backupServer = currPlayer.getBackupServer();
                    if (backupServer != null) {
                        GameState gameState = backupServer.performAction(playerID, command, Player.ActionType.WRITE);
                        copyState(gameState);
                        gui.update(gameState);
                        success = true;
                    }
                } catch (RemoteException ex) {
                    try {
                        System.out.println("Backup " + currPlayer.getBackupID() + " performAction fail");
                    } catch (RemoteException exc) {
                        exc.printStackTrace();
                    }

                    if (++retryCount < maxRetries) {
                        try {
                            Thread.sleep(100);  // Sleep for 100ms before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("Retrying... (" + retryCount + ")");
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }


    private void copyState(GameState gameState) throws RemoteException {
        if (!playerID.equals(gameState.getPrimaryID()) && !playerID.equals(gameState.getBackupID())) {
            currPlayer.copyState(gameState);
        }
    }


}