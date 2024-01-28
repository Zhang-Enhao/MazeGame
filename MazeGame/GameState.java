import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;



public class GameState implements Serializable {

    private int N;

    private int K;

    public ConcurrentHashMap<String, Integer> scores;

    public ConcurrentHashMap<String, Point> positions;

    private ConcurrentHashMap<String, PlayerInterface> players;

    private volatile PlayerInterface primaryServer;

    private volatile PlayerInterface backupServer;

    private volatile String primaryID;

    private volatile String backupID;

    private volatile String[][] maze;

    private volatile Date startTime;

    private ReentrantLock mazeLock = new ReentrantLock();

    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
        this.scores = new ConcurrentHashMap<>();
        this.positions = new ConcurrentHashMap<>();
        this.maze = new String[N][N];
        this.players = new ConcurrentHashMap<>();
        this.primaryServer = null;
        this.backupServer = null;
        this.primaryID = null;
        this.backupID = null;

        for (int i = 0; i < K; i++) {
            generateTreasure();
        }
    }


    public synchronized void copyState(GameState otherState) {
        this.N = otherState.getN();
        this.K = otherState.getK();
        this.scores = new ConcurrentHashMap<>(otherState.scores);
        this.positions = new ConcurrentHashMap<>(otherState.positions);
        this.maze = otherState.getMaze();
        this.players = otherState.getPlayers();
        this.primaryServer = otherState.getPrimaryServer();
        this.backupServer = otherState.getBackupServer();
        this.primaryID = otherState.getPrimaryID();
        this.backupID = otherState.getBackupID();
        this.startTime = otherState.getStartTime();
//        SimpleDateFormat sdf= new SimpleDateFormat("HH:mm:ss");
//        System.out.println("pri" + sdf.format(otherState.getStartTime()));
//        System.out.println("cur" + sdf.format(newState.startTime));
    }

    public void setGameStartTime() {
        Date currTime = new Date(System.currentTimeMillis());
        this.startTime = currTime;
    }

    public int getN() {
        return this.N;
    }

    public int getK() {
        return this.K;
    }

    public String[][] getMaze() {
        return this.maze;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public int getScores(String id) {
        return this.scores.get(id);
    }

    public ConcurrentHashMap<String, Integer> getAllScores() {
        return this.scores;
    }

    public void setScores(String id, int score) {
        this.scores.put(id, score);
    }

    public synchronized void move(int oldX, int oldY, int newX, int newY, String playId) {
        if (newX < 0 || newX >= N || newY < 0 || newY >= N) {
            return;
        }
        mazeLock.lock();
        try {
            String value = maze[newX][newY];
            if (value != null && !"*".equals(value)) {
                return;
            } else {
                if (value != null) {
                    this.setScores(playId, this.getScores(playId) + 1);
                    this.generateTreasure();
                }
                this.setMaze(oldX, oldY, null);
                Point currentPosition = new Point(newX, newY);
                this.setPositions(playId, currentPosition);
                this.setMaze(newX, newY, playId);
            }
        } finally {
            mazeLock.unlock();
        }
    }

    public Point getPositions(String id) {
        return this.positions.get(id);
    }

    public void setPositions(String id, Point position) {
        this.positions.put(id, position);
    }

    public ConcurrentHashMap<String, PlayerInterface> getPlayers() {
        return this.players;
    }

    public PlayerInterface getPrimaryServer() {
        return this.primaryServer;
    }

    public void setPrimaryServer(PlayerInterface player){
        this.primaryServer = player;
    }

    public PlayerInterface getBackupServer() {
        return this.backupServer;
    }

    public void setBackupServer(PlayerInterface backupServer) {
        this.backupServer = backupServer;
    }

    public String getPrimaryID() {
        return this.primaryID;
    }

    public void setPrimaryID(String primaryID) {
        this.primaryID = primaryID;
    }

    public String getBackupID() {
        return this.backupID;
    }

    public void setBackupID(String backupID) {
        this.backupID = backupID;
    }

    public synchronized void generateTreasure() {
        while (true) {
            Point newPos = randomPos();
            int newX = (int) newPos.getX();
            int newY = (int) newPos.getY();
            if (maze[newX][newY] == null) {
                maze[newX][newY] = "*";
                break;
            }
        }
    }

    public synchronized void addNewPlayer(PlayerInterface player) {
        String id;
        try {
            id = player.getPlayerID();
        } catch (Exception e) {
            return;
        }

        if (players.containsKey(id)) {
            return;
        }

        Point newPos = randomPos();
        players.put(id, player);
        this.maze[(int)newPos.getX()][(int)newPos.getY()] = id;

        positions.put(id, newPos);
        scores.put(id, 0);
    }

    public synchronized void removePlayer(String id){
        if (! players.containsKey(id)) {
            return;
        }
        mazeLock.lock();
        try {
            maze[(int)positions.get(id).getX()][(int)positions.get(id).getY()] = null;
            positions.remove(id);
            scores.remove(id);
            players.remove(id);
            System.out.println("Player " + id + " has been removed.");
        } finally {
            mazeLock.unlock();
        }
    }

    public Point randomPos() {
        while (true) {
			Point randomPos = new Point(new Random().nextInt(N), new Random().nextInt(N));
            if (maze[(int)randomPos.getX()][(int)randomPos.getY()] == null) {
                return randomPos;
            }
        }
	} 

    public synchronized void setMaze(int x, int y, String newVal) {
        this.maze[x][y] = newVal;
    }

    public ArrayList<PlayerInterface> getPlayerList() {
        ArrayList<PlayerInterface> ret = new ArrayList<>(this.players.values());
        return ret;
    }

    public void printMaze() {
        System.out.println("-----------------------------------");
        System.out.println("Maze: ");
        for (int i = 0; i < N; i++) {
            System.out.print("|");
            for (int j = 0; j < N; j++) {
                if (maze[i][j] == null) {
                    System.out.print("  |");
                } else {
                    System.out.print(maze[i][j] + " |");
                }
            }
            System.out.println();
        }
        System.out.println("-----------------------------------");
    }
}