import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;

public interface PlayerInterface extends Remote {
    public void setPlayerID(String playerID) throws RemoteException;

    public String getPlayerID() throws RemoteException;

    public String getServerType() throws RemoteException;

    public void setServerType(String serverType) throws RemoteException;

    public String getPrimaryID() throws RemoteException;

    public String getBackupID() throws RemoteException;

    public PlayerInterface getPrimaryServer() throws RemoteException;

    public void becomePrimaryServer(String playerId) throws RemoteException;

    public void becomeBackupServer(String playerId, PlayerInterface primaryServer) throws RemoteException;

    public GameState addToGame(PlayerInterface player) throws RemoteException;

    public GameState getGameState() throws RemoteException;

    public PlayerInterface getBackupServer() throws RemoteException;

    public void setPrimaryServer(PlayerInterface player) throws RemoteException;

    public void setBackupServer(PlayerInterface player) throws RemoteException;

    public void setPrimaryID(String primaryID) throws RemoteException;

    public void setBackupID(String backupID) throws RemoteException;

    public void setGameStartTime() throws RemoteException;

    public void copyState(GameState gameState) throws RemoteException;

    public void removePlayer(String id) throws RemoteException;

    public ConcurrentHashMap<String, PlayerInterface> getPlayers() throws RemoteException;

    public String[][] getMaze() throws RemoteException;

    public void setPositions(String id, Point position) throws RemoteException;

    public Point getPositions(String id) throws RemoteException;

    public void generateTreasure() throws RemoteException;

    public void setMaze(int x, int y, String newVal) throws RemoteException;

    public int getScores(String id) throws RemoteException;

    public void setScores(String id, int score) throws RemoteException;

    public ConcurrentHashMap<String, Integer> getAllScores() throws RemoteException;

    public ArrayList<PlayerInterface> getPlayerList() throws RemoteException;

    public GameState performAction(String id, String command, Player.ActionType actionType) throws RemoteException;

    public void selectNewBackup() throws RemoteException;

}
