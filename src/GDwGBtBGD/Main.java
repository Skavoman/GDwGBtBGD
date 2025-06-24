package GDwGBtBGD;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    static Action startGameAction;
    private static double seconds = 0;
    private static JLabel timeLabel;
    private static Timer timer;
    static int worldSizeX = 9;
    static int worldSizeY = 9;
    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public static void createAndShowSplash(){
        JFrame splash = new JFrame();
        splash.setSize(379, 249);
        splash.setLocationRelativeTo(null);
        splash.setUndecorated(true);
        splash.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon("splash.gif");
        JLabel jLabel = new JLabel(imageIcon);
        jLabel.setIcon(new ImageIcon(Main.class.getResource("splash.gif")));

        splash.add(jLabel, BorderLayout.CENTER);

        splash.setVisible(true);

        new Timer(9900, e -> splash.dispose()).start();
    }

    public static void createAndShowGUI() {

        //read world data from text file
        try {
            File myObj = new File("level.nte");
            Scanner myReader = new Scanner(myObj);
            if(myReader.hasNextLine()){
                System.out.println(myReader.nextLine());
            }
            if(myReader.hasNextLine()){
                worldSizeX = Integer.parseInt(myReader.nextLine());
            }
            if(myReader.hasNextLine()){
                worldSizeY = Integer.parseInt(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println(" ");
        } catch (NumberFormatException e){
            worldSizeX = 7;
            worldSizeY = 7;
        }

        JFrame frame = new JFrame("Grid Dots with Grid Blocks that Block Grid Dots");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        GridPanel panel = new GridPanel(worldSizeX, worldSizeY, 37);

        JProgressBar healthBar = new JProgressBar();
        healthBar.setMaximum(5);

        JButton playAgain = new JButton("Play it Again");
        JButton exit = new JButton("Exit");
        playAgain.setVisible(false);
        exit.setVisible(false);
        playAgain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();

                createAndShowGUI();
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        frame.setIconImage(new ImageIcon(Main.class.getResource("javaIcon.png")).getImage());

        JLabel tips = new JLabel("");
        tips.setForeground(Color.RED);
        new Timer(2345, e -> tips.setText("Click on the grid to place blocks!")).start();
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tips.setVisible(false);
            }
        });

        timeLabel = new JLabel("0 seconds", SwingConstants.CENTER);
        timeLabel.setForeground(Color.WHITE);

        timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double scale = Math.pow(10, 2); // 2 decimal places
                double roundedValue = Math.round(seconds * scale) / scale;
                seconds = seconds + 0.1;
                timeLabel.setText("      "+roundedValue + " seconds      ");


                if (panel.dead){
                    timer.stop();
                    timeLabel.setText("You survived: "+roundedValue + " seconds");
                    playAgain.setVisible(true);
                    exit.setVisible(true);
                    roundedValue = 0;
                    seconds = 0;
                 }
                healthBar.setValue(panel.health);

            }
        });
        timer.start();

        JLabel newBlocks = new JLabel();
        newBlocks.setForeground(Color.yellow);

        panel.setLayout(new FlowLayout());
        panel.add(timeLabel);
        panel.add(healthBar);
        panel.add(playAgain);
        panel.add(exit);
        panel.add(tips);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        panel.requestFocusInWindow();
    }
}

class GridPanel extends JPanel {
    private int rows;
    private int cols;
    private int cellSize;
    int circleRow;
    int circleCol;
    private int circleDiameter;
    int greenRow;
    int greenCol;
    private int redCellsBroken;
    Color[][] gridColors;
    private Timer timer;
    private Map<String, Long> redCellTimers;
    private long cooldownEndTime;
    private Random random;
    private int yellowRow = -1;
    private int yellowCol = -1;
    private boolean yellowCellCreated = false;
    boolean dead = false;
    int health = 5;
    boolean canPlaceBlocks = true;
    Color msGray = new ColorUIResource(255, 255, 255);
    private Image redCellImage;
    private Image whiteCellImage;
    private Image yellowCellImage;
    private Image orangeCellImage;
    private Image blueCellImage;
    private Image purpleCellImage;
    int redCellAmount;



    public GridPanel(int rows, int cols, int cellSize) {
        this.rows = rows;
        this.cols = cols;
        this.cellSize = cellSize;
        this.circleRow = 0;
        this.circleCol = 0;
        this.greenRow = rows - 1;
        this.greenCol = cols - 1;
        this.circleDiameter = cellSize / 2;
        this.gridColors = new Color[rows][cols];
        this.redCellTimers = new HashMap<>();
        this.cooldownEndTime = System.currentTimeMillis();
        this.random = new Random();
        setPreferredSize(new Dimension(cols * cellSize, rows * cellSize));

        String gridBlockPath = "gridBlock.png";
        String gridGroundPath = "gridGround.png";
        String yellowBlockPath = "yellowBlock.png";
        String tallGrassPath = "tallgrass.png";
        String bearTrapPath = "beartrap.png";
        String bearTrapSmapPath = "beartrap.gif";

        redCellImage = new ImageIcon(Main.class.getResource(gridBlockPath)).getImage(); 
        redCellImage = redCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        blueCellImage = new ImageIcon(Main.class.getResource(bearTrapPath)).getImage(); 
        blueCellImage = blueCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        purpleCellImage = new ImageIcon(Main.class.getResource(bearTrapSmapPath)).getImage();
        purpleCellImage = purpleCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        orangeCellImage = new ImageIcon(Main.class.getResource(tallGrassPath)).getImage();
        orangeCellImage = orangeCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        whiteCellImage = new ImageIcon(Main.class.getResource(gridGroundPath)).getImage();
        whiteCellImage = whiteCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);
        yellowCellImage = new ImageIcon(Main.class.getResource(yellowBlockPath)).getImage();
        whiteCellImage = whiteCellImage.getScaledInstance(cellSize, cellSize, Image.SCALE_SMOOTH);

        Action startGameAction;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gridColors[i][j] = msGray;
            }
        }

        int i = 0;
        if (i == 0){
            randomBlocksGenerator();
            randomBlockReplenisher();
        }

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (redCellAmount == 2){
                    randomBlocksGenerator();
                }

                if (canPlaceBlocks){
                    int newCircleRow = circleRow;
                    int newCircleCol = circleCol;

                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                            if (circleRow > 0) newCircleRow--;
                            System.out.println("j");
                            break;
                        case KeyEvent.VK_DOWN:
                            if (circleRow < rows - 1) newCircleRow++;
                            break;
                        case KeyEvent.VK_LEFT:
                            if (circleCol > 0) newCircleCol--;
                            break;
                        case KeyEvent.VK_RIGHT:
                            if (circleCol < cols - 1) newCircleCol++;
                            break;
                        case KeyEvent.VK_W:
                            if (circleRow > 0) newCircleRow--;
                            break;
                        case KeyEvent.VK_S:
                            if (circleRow < rows - 1) newCircleRow++;
                            break;
                        case KeyEvent.VK_A:
                            if (circleCol > 0) newCircleCol--;
                            break;
                        case KeyEvent.VK_D:
                            if (circleCol < cols - 1) newCircleCol++;
                            break;
                    }

                    if (gridColors[newCircleRow][newCircleCol] == msGray) {
                        circleRow = newCircleRow;
                        circleCol = newCircleCol;
                        repaint();
                    }
                    if (gridColors[newCircleRow][newCircleCol] == Color.ORANGE) {
                        circleRow = newCircleRow;
                        circleCol = newCircleCol;
                        repaint();
                    }
                    if (gridColors[newCircleRow][newCircleCol] == Color.BLUE) {
                        circleRow = newCircleRow;
                        circleCol = newCircleCol;
                        updateHealth();
                    }
                }


            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                int row = y / cellSize;
                int col = x / cellSize;

                // Change the clicked cell to red
                if (gridColors[row][col] == msGray && canPlaceBlocks){
                    gridColors[row][col] = Color.RED;
                    redCellAmount++;
                }


                repaint();
            }
        });

        timer = new Timer(337, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveGreenDot();
                checkRedCells();
                repaint();
            }
        });
        timer.start();
    }

    void moveGreenDot() {
        int[] direction = findNextMove();
        if (direction != null) {
            greenRow += direction[0];
            greenCol += direction[1];
        }

        if (greenRow == yellowRow && greenCol == yellowCol) {
            teleportGreenDot();
        }
        if (greenRow == circleRow&& greenCol == circleCol) {
            updateHealth();
        }
        int newCircleRow1 = greenRow;
        int newCircleCol1 = greenCol;
        if (gridColors[newCircleRow1][newCircleCol1] == Color.YELLOW) {
            teleportGreenDot();
        }
    }

    public void updateHealth(){
        health--;
        if (health > 0){
            System.out.println(health);
        }
        if (health == 0){
            dead = true;
            canPlaceBlocks = false;

        }
    }

    public static void resetGame(Color[][] grid, Color newColor) {

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = newColor;
            }
        }

    }

    private int[] findNextMove() {
        int distanceToBlue = Math.abs(greenRow - circleRow) + Math.abs(greenCol - circleCol);
        int distanceToYellow = Math.abs(greenRow - yellowRow) + Math.abs(greenCol - yellowCol);

        if (yellowRow != -1 && yellowCol != -1 && distanceToYellow < distanceToBlue) {
            // Move towards yellow cell
            if (greenRow < yellowRow && isValidMove(greenRow + 1, greenCol)) {
                return new int[] {1, 0};
            } else if (greenRow > yellowRow && isValidMove(greenRow - 1, greenCol)) {
                return new int[] {-1, 0};
            } else if (greenCol < yellowCol && isValidMove(greenRow, greenCol + 1)) {
                return new int[] {0, 1};
            } else if (greenCol > yellowCol && isValidMove(greenRow, greenCol - 1)) {
                return new int[] {0, -1};
            }
        }

        if (greenRow == yellowRow && greenCol == yellowCol) {
            teleportGreenDot();
        }

        if (greenRow < circleRow && isValidMove(greenRow + 1, greenCol)) {
            return new int[] {1, 0};
        } else if (greenRow > circleRow && isValidMove(greenRow - 1, greenCol)) {
            return new int[] {-1, 0};
        } else if (greenCol < circleCol && isValidMove(greenRow, greenCol + 1)) {
            return new int[] {0, 1};
        } else if (greenCol > circleCol && isValidMove(greenRow, greenCol - 1)) {
            return new int[] {0, -1};
        }
        return null;

    }

    private boolean isValidMove(int newRow, int newCol) {
        return newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                && gridColors[newRow][newCol] != Color.RED;
    }

    private void checkRedCells() {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        long currentTime = System.currentTimeMillis();
        int targetRow = -1;
        int targetCol = -1;
        int closestDistance = Integer.MAX_VALUE;
        List<int[]> redCells = new ArrayList<>();

        for (int[] direction : directions) {
            int newRow = greenRow + direction[0];
            int newCol = greenCol + direction[1];
            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                    && gridColors[newRow][newCol] == Color.RED) {
                String key = newRow + "," + newCol;
                int distance = Math.abs(newRow - circleRow) + Math.abs(newCol - circleCol);
                if ((newRow == circleRow || newCol == circleCol) && distance < closestDistance) {
                    closestDistance = distance;
                    targetRow = newRow;
                    targetCol = newCol;
                }
                if (redCellTimers.containsKey(key)) {
                    long elapsed = currentTime - redCellTimers.get(key);
                    if (elapsed > 3670 && currentTime >= cooldownEndTime && !dead) {
                        if (targetRow != -1 && targetCol != -1) {
                            gridColors[targetRow][targetCol] = msGray;
                            redCellTimers.remove(key);
                            cooldownEndTime = currentTime + 2760;
                            redCellsBroken++;
                            checkYellowCell();
                        } else {
                            if (!dead){
                                redCells.add(new int[]{newRow, newCol});
                            }
                        }
                    }
                } else {
                    redCellTimers.put(key, Long.valueOf(currentTime));
                }
            } else {
                redCellTimers.remove(newRow + "," + newCol);
            }
        }

        if (targetRow == -1 && !redCells.isEmpty() && !dead) {
            int[] randomRedCell = redCells.get(random.nextInt(redCells.size()));
            gridColors[randomRedCell[0]][randomRedCell[1]] = msGray;
            redCellsBroken++;
            checkYellowCell();
        }
    }

    private void checkYellowCell() {
        if (redCellsBroken >= 4 && !yellowCellCreated) {
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] direction : directions) {
                int newRow = greenRow + direction[0];
                int newCol = greenCol + direction[1];
                if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols
                        && gridColors[newRow][newCol] == msGray) {
                    yellowRow = newRow;
                    yellowCol = newCol;
                    gridColors[yellowRow][yellowCol] = Color.YELLOW;
                    break;
                }
            }
        }
    }

    private void teleportGreenDot() {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        List<int[]> potentialCells = new ArrayList<>();
        for (int[] direction1 : directions) {
            int row1 = circleRow + direction1[0];
            int col1 = circleCol + direction1[1];
            if (isValidTeleportCell(row1, col1)) {
                for (int[] direction2 : directions) {
                    int row2 = row1 + direction2[0];
                    int col2 = col1 + direction2[1];
                    if (isValidTeleportCell(row2, col2)) {
                        potentialCells.add(new int[]{row2, col2});
                    }
                }
            }
        }
        if (!potentialCells.isEmpty() && !dead) {
            int[] cell = potentialCells.get(random.nextInt(potentialCells.size()));
            greenRow = cell[0];
            greenCol = cell[1];
        }
        yellowRow = -1;
        yellowCol = -1;
        redCellsBroken = 0;
    }

    private void randomBlocksGenerator(){
        for (int i = 0; i < 8; i++) {
            Random random1 = new Random();
            gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.RED;
            redCellAmount++;
        }
        Random random2 = new Random();
        gridColors[random2.nextInt(Main.worldSizeX)][random2.nextInt(Main.worldSizeY)] = Color.yellow;

        for (int i = 0; i < 10; i++) {
            gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.ORANGE;
        }
    }

    private void randomBlockReplenisher(){
        timer = new Timer(13495, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Random random = new Random();
                gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.RED;
                gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.RED;
                gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.RED;
                gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.RED;
                gridColors[random.nextInt(Main.worldSizeX)][random.nextInt(Main.worldSizeY)] = Color.BLUE;
            }

        });
        timer.start();

    }

    private boolean isValidTeleportCell(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols
                && gridColors[row][col] == msGray;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        super.paintComponent(g);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                if (gridColors[i][j] == Color.RED) {
                    g.drawImage(redCellImage, j * cellSize, i * cellSize, this);
                } else if (gridColors[i][j] == msGray) {
                    g.drawImage(whiteCellImage, j * cellSize, i * cellSize, this);
                } else if (gridColors[i][j] == Color.ORANGE) {
                    g.drawImage(orangeCellImage, j * cellSize, i * cellSize, this);
                } else if (gridColors[i][j] == Color.YELLOW) {
                    g.drawImage(yellowCellImage, j * cellSize, i * cellSize, this);
                } else if (gridColors[i][j] == Color.BLUE) {
                    g.drawImage(blueCellImage, j * cellSize, i * cellSize, this);
                } else if (gridColors[i][j] == Color.magenta) {
                    g.drawImage(purpleCellImage, j * cellSize, i * cellSize, this);
                } else {
                    g.setColor(gridColors[i][j]);
                    g.fillRect(j * cellSize, i * cellSize, cellSize, cellSize);
                }

            }
        }



        g.setColor(new ColorUIResource(0, 0, 255));
        int circleX = circleCol * cellSize + (cellSize - circleDiameter) / 2;
        int circleY = circleRow * cellSize + (cellSize - circleDiameter) / 2;
        g.fillOval(circleX, circleY, circleDiameter, circleDiameter);

        g.setColor(new ColorUIResource(0, 255, 0));
        int greenX = greenCol * cellSize + (cellSize - circleDiameter) / 2;
        int greenY = greenRow * cellSize + (cellSize - circleDiameter) / 2;
        g.fillOval(greenX, greenY, circleDiameter, circleDiameter);

        if (yellowRow != -1 && yellowCol != -1) {
            g.setColor(Color.YELLOW);
            int yellowX = yellowCol * cellSize + (cellSize - circleDiameter) / 2;
            int yellowY = yellowRow * cellSize + (cellSize - circleDiameter) / 2;
            g.fillRect(yellowX, yellowY, circleDiameter, circleDiameter);
        }
    }

}
