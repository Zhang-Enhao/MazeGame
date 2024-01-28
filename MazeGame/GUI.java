import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

public class GUI extends JFrame {

    private int N;
    private JPanel gridPanel;
    private JPanel leftPanel;
    private JPanel scorePanel;
    private JPanel primaryPanel;
    private JPanel backupPanel;
    private JPanel timePanel;

    public GUI(PlayerInterface player, String currentPlayerID) {

        try {
            GameState gameState = player.getGameState();
            this.N = gameState.getN();
            setTitle(currentPlayerID);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            gridPanel = new JPanel();
            leftPanel = new JPanel();
            scorePanel = new JPanel();
            primaryPanel = new JPanel();
            backupPanel = new JPanel();
            timePanel = new JPanel();
            leftPanel.setLayout(new GridLayout(4, 1));


            leftPanel.add(scorePanel);
            leftPanel.add(primaryPanel);
            leftPanel.add(backupPanel);
            leftPanel.add(timePanel);

            SimpleDateFormat sdf= new SimpleDateFormat("HH:mm:ss");
            timePanel.add(new JLabel(sdf.format(gameState.getStartTime())));

            add(leftPanel,BorderLayout.WEST);
            add(gridPanel, BorderLayout.CENTER);

            setSize(1000, 500);
            setVisible(true);

            gridPanel.removeAll();
            gridPanel.setLayout(new GridLayout(N, N));

            for (int i = 0; i < N * N; i++) {
                JButton button = new JButton();
                button.setEnabled(false);
                gridPanel.add(button);
            }

            update(gameState);
            validate();
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(GameState gameState){
        try {
            updateScores(gameState.getAllScores());
            updateMaze(gameState.getMaze());
            updatePrimary(gameState.getPrimaryID());
            updateBackup(gameState.getBackupID());
            this.repaint();
        } catch (Exception e) {
        }
        
    }

    private void updateScores(ConcurrentHashMap<String, Integer> scores) {
        scorePanel.removeAll();
        int size = scores.size();
        scorePanel.setLayout(new GridLayout(size, 1));
        for (String s : scores.keySet()) {
            int score = scores.get(s);
            scorePanel.add(new JLabel(s + ": " + score));
        }
    }

    private void updatePrimary(String primaryID) {
        primaryPanel.removeAll();
        primaryPanel.add(new JLabel("Primary Server : " + primaryID));
    }

    private void updateBackup(String backupID) {
        backupPanel.removeAll();
        backupPanel.add(new JLabel("BackUp Server : " + backupID));
    }


    private void updateMaze(String[][] maze) {
        Component[] components = gridPanel.getComponents();

        for (Component component : components) {
            JButton button = (JButton) component;
            Point position = buttonPosition(button);
            button.setText(maze[(int)position.x][(int)position.y]);
        }
    }

    private Point buttonPosition(JButton button) {
        int index = gridPanel.getComponentZOrder(button);
        int row = index / N;
        int col = index % N;
        return new Point(col, row);
    }

    // public static void main(String[] args) {
    //     String[][] maze = new String[5][5];
    //     maze[0][1] = "aa";
    //     maze[1][3] = "ab";
    //     maze[2][4] = "ac";
    //     maze[3][3] = "ad";
    //     maze[2][1] = "*";
    //     maze[3][4] = "*";
    //     maze[2][3] = "*";
    //     maze[0][0] = "*";
    //     ConcurrentHashMap<String, Integer> scores = new ConcurrentHashMap<String, Integer>();
    //     scores.put("aa", 1);
    //     scores.put("ab", 2);
    //     scores.put("ac", 3);
    //     scores.put("ad", 4);
    //     GUI gui = new GUI("aa", 5, scores, "aa", "ab",  maze);
    //     maze[0][0] = "cc";
    //     gui.updateMaze(maze);
    //     scores.put("ad", 56);
    //     gui.updateScores(scores);

    // }
}
