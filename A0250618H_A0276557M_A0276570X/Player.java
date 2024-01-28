import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class Player implements Serializable, PlayerInterface {
    private String playerID;
    private String serverType;
    private volatile GameState gameState;
    // Avoid executing performAction during primary/backup server switch
    private ReentrantLock lock = new ReentrantLock();

    public Player(String playerID) throws RemoteException {
        this.playerID = playerID;
        this.gameState = new GameState(15, 10);
    }

    public PlayerInterface getPrimaryServer() {
        return this.gameState.getPrimaryServer();
    }

    public PlayerInterface getBackupServer() {
        return this.gameState.getBackupServer();
    }

    @Override
    public void setPrimaryServer(PlayerInterface player) throws RemoteException {
        this.gameState.setPrimaryServer(player);
    }

    public String getServerType() {
        return this.serverType;
    }

    public void setServerType(String serverType){
        this.serverType = serverType;
    }


    public synchronized void becomePrimaryServer(String playerId) throws RemoteException {
        lock.lock();
        try {
            // If current player is backup server, remove from it first
            String oldPrimaryId = gameState.getPrimaryID();
            if (playerId.equals(gameState.getBackupID())) {
                gameState.setBackupServer(null);
                gameState.setBackupID(null);
            }
            System.out.println(playerId + " become new primary server");
            this.setServerType("primary");
            this.gameState.setPrimaryServer(this);
            this.gameState.setPrimaryID(playerId);
            this.addToGame(this);

            if (oldPrimaryId != null) {
                removePlayer(oldPrimaryId);
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized void becomeBackupServer(String playerId, PlayerInterface primaryServer) throws RemoteException {
        lock.lock();
        try {
            System.out.println(playerId + " Become new backup server");
            this.setServerType("backup");
            this.gameState.setBackupServer(this);
            this.gameState.setBackupID(playerId);
            this.setPrimaryServer(primaryServer);
            primaryServer.setBackupServer(this);
            this.copyState(primaryServer.getGameState());
        } finally {
            lock.unlock();
        }
    }

    public GameState addToGame(PlayerInterface player) throws RemoteException {
        this.gameState.addNewPlayer(player);
        if ("primary".equals(this.getServerType()) && this.getBackupServer() != null) {
            try {
                this.gameState.getBackupServer().addToGame(player);
            } catch (RemoteException e) {
                System.out.println("RemoteException in addToGame");
                throw e;
            }
        }
        return this.gameState;
    }


    public void setPlayerID(String playerID) throws RemoteException{
        this.playerID = playerID;
    }


    @Override
    public String getPlayerID() throws RemoteException{
        return this.playerID;
    }

    public String getPrimaryID() {
        return this.gameState.getPrimaryID();
    }

    public String getBackupID() {
        return this.gameState.getBackupID();
    }

    public void setPrimaryID(String primaryID) {
        this.gameState.setPrimaryID(primaryID);
    }

    public void setBackupID(String backupID) {
        this.gameState.setBackupID(backupID);
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setBackupServer(PlayerInterface player) throws RemoteException{
        this.gameState.setBackupServer(player);
        this.gameState.setBackupID(player.getPlayerID());
        GameState primaryState = this.addToGame(player);
    }


    public void setGameStartTime() {
        this.gameState.setGameStartTime();
    }

    public synchronized void copyState(GameState gameState) {
        System.out.println(playerID + " Copy from primary " + gameState.getPrimaryID());
        this.gameState.copyState(gameState);
    }

    public synchronized GameState performAction(String playId, String command, ActionType actionType) throws RemoteException {
        if (playId == null) {
            throw new RemoteException("Player id is null");
        }
        lock.lock();
        try {
            if (this.playerID.equals(gameState.getPrimaryID()) && actionType == ActionType.WRITE) {
                doPerformAction(playId, command);
                // copy action to backup server
                if (getBackupServer() != null) {
                    System.out.println(String.format("%s copy to %s", gameState.getPrimaryID(), gameState.getBackupID()));
                    gameState.getBackupServer().performAction(playId, command, ActionType.COPY);
                }
            } else if (this.playerID.equals(gameState.getBackupID()) && actionType == ActionType.COPY) {
                System.out.println(String.format("%s copy from %s", gameState.getBackupID(), gameState.getPrimaryID()));
                doPerformAction(playId, command);
            } else if (this.playerID.equals(gameState.getBackupID()) && actionType == ActionType.WRITE) {
                System.out.println(String.format("Transfer request from %s to %s", gameState.getBackupID(), gameState.getPrimaryID()));
                gameState.getPrimaryServer().performAction(playId, command, ActionType.WRITE);
            }
    //        gameState.printMaze();
            return gameState;
        }finally {
            lock.unlock();
        }
    }

    private synchronized void doPerformAction(String playId, String command) throws RemoteException {
        try {
            Point currentPosition = gameState.getPositions(playId);
            int oldX = (int) currentPosition.getX();
            int oldY = (int) currentPosition.getY();
            int newX = oldX, newY = oldY;
            switch (command) {
                case "0":
                    System.out.println(serverType + " move " + playId + " 0");
                    return;
                case "1":
                    System.out.println(serverType + " move " + playId + " 1");
                    newX = oldX - 1;
                    break;
                case "2":
                    System.out.println(serverType + " move " + playId + " 2");
                    newY = oldY + 1;
                    break;
                case "3":
                    System.out.println(serverType + " move " + playId + " 3");
                    newX = oldX + 1;
                    break;
                case "4":
                    System.out.println(serverType + " move " + playId + " 4");
                    newY = oldY - 1;
                    break;
                default:
                    return;
            }
//            System.out.println("oldX: " + oldX + " oldY: " + oldY + " newX: " + newX + " newY: " + newY);
            gameState.move(oldX, oldY, newX, newY, playId);
        } catch (Exception e) {
            throw new RemoteException();
        }
    }

    @Override
    public synchronized void selectNewBackup() throws RemoteException {
        if (playerID.equals(gameState.getPrimaryID())) {
            if (gameState.getPlayers().size() == 1) {
                return;
            }
            for (PlayerInterface player : gameState.getPlayerList()) {
                try {
                    if (player.getPlayerID().equals(playerID)) {
                        continue;
                    }
                    player.becomeBackupServer(player.getPlayerID(), this);
                    break;
                } catch (RemoteException e) {
                    System.out.println("RemoteException in selectNewBackup");
                    continue;
                }
            }
        }
    }

    public void removePlayer(String id) throws RemoteException {
        if (this.playerID.equals(gameState.getPrimaryID())) {
            this.gameState.removePlayer(id);
            if (id.equals(gameState.getBackupID())) {
                gameState.setBackupID(null);
                gameState.setBackupServer(null);
            }
            if (gameState.getBackupServer() != null) {
                gameState.getBackupServer().removePlayer(id);
            }
        }
    }

    public ConcurrentHashMap<String, PlayerInterface> getPlayers() {
        return this.gameState.getPlayers();
    }

    public String[][] getMaze() {
        return this.gameState.getMaze();
    }

    public int getScores(String id) {
        return this.gameState.getScores(id);
    }

    public void setScores(String id, int score) {
        this.gameState.setScores(id, score);
    }

    public Point getPositions(String id) {
        return this.gameState.getPositions(id);
    }

    public void generateTreasure() {
        this.gameState.generateTreasure();
    }

    public void setMaze(int x, int y, String newVal) {

    }

    public void setPositions(String id, Point position) {
        this.gameState.setPositions(id, position);
    }

    public ConcurrentHashMap<String, Integer> getAllScores() {
        return this.gameState.getAllScores();
    }

    public ArrayList<PlayerInterface> getPlayerList() {
        return this.gameState.getPlayerList();
    }

    public enum ActionType {
        WRITE,
        COPY
    }

}