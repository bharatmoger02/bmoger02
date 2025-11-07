import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class AdvSudoku extends JFrame {
    private static final int SIZE = 9;
    private static final int SUBGRID = 3;
    private JTextField[][] cells = new JTextField[SIZE][SIZE];
    private int[][] puzzle = new int[SIZE][SIZE];
    private int[][] solution = new int[SIZE][SIZE];
    private boolean[][] fixed = new boolean[SIZE][SIZE];
    private JLabel statusLabel;
    private JButton solveBtn, hintBtn, newGameBtn, checkBtn;
    private JComboBox<String> difficultyBox;
    private javax.swing.Timer gameTimer;
    private int seconds = 0;
    private JLabel timerLabel;

    public AdvSudoku() {
        setTitle("Advanced Sudoku Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        initComponents();
        generateNewPuzzle("Medium");
        
        setSize(600, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Top panel with controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        topPanel.setBackground(new Color(240, 240, 240));
        
        difficultyBox = new JComboBox<>(new String[]{"Easy", "Medium", "Hard", "Expert"});
        difficultyBox.setSelectedItem("Medium");
        
        newGameBtn = new JButton("New Game");
        newGameBtn.setBackground(new Color(76, 175, 80));
        newGameBtn.setForeground(Color.WHITE);
        newGameBtn.setFocusPainted(false);
        newGameBtn.addActionListener(e -> generateNewPuzzle((String) difficultyBox.getSelectedItem()));
        
        hintBtn = new JButton("Hint");
        hintBtn.setBackground(new Color(33, 150, 243));
        hintBtn.setForeground(Color.WHITE);
        hintBtn.setFocusPainted(false);
        hintBtn.addActionListener(e -> provideHint());
        
        checkBtn = new JButton("Check");
        checkBtn.setBackground(new Color(255, 152, 0));
        checkBtn.setForeground(Color.WHITE);
        checkBtn.setFocusPainted(false);
        checkBtn.addActionListener(e -> checkSolution());
        
        solveBtn = new JButton("Solve");
        solveBtn.setBackground(new Color(244, 67, 54));
        solveBtn.setForeground(Color.WHITE);
        solveBtn.setFocusPainted(false);
        solveBtn.addActionListener(e -> solvePuzzle());
        
        timerLabel = new JLabel("Time: 00:00");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        topPanel.add(new JLabel("Difficulty:"));
        topPanel.add(difficultyBox);
        topPanel.add(newGameBtn);
        topPanel.add(hintBtn);
        topPanel.add(checkBtn);
        topPanel.add(solveBtn);
        topPanel.add(timerLabel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Sudoku grid panel
        JPanel gridPanel = new JPanel(new GridLayout(SIZE, SIZE, 1, 1));
        gridPanel.setBackground(Color.BLACK);
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                cells[row][col] = new JTextField();
                cells[row][col].setHorizontalAlignment(JTextField.CENTER);
                cells[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                
                // Add thick borders for 3x3 subgrids
                int top = (row % SUBGRID == 0) ? 3 : 1;
                int left = (col % SUBGRID == 0) ? 3 : 1;
                int bottom = (row == SIZE - 1) ? 3 : 1;
                int right = (col == SIZE - 1) ? 3 : 1;
                cells[row][col].setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));
                
                final int r = row;
                final int c = col;
                
                // Input validation
                cells[row][col].addKeyListener(new KeyAdapter() {
                    public void keyTyped(KeyEvent e) {
                        char ch = e.getKeyChar();
                        if (!Character.isDigit(ch) || ch == '0' || cells[r][c].getText().length() >= 1) {
                            e.consume();
                        }
                    }
                });
                
                gridPanel.add(cells[row][col]);
            }
        }
        
        add(gridPanel, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel();
        statusPanel.setBackground(new Color(240, 240, 240));
        statusLabel = new JLabel("Welcome to Sudoku! Select difficulty and click New Game.");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusPanel.add(statusLabel);
        
        add(statusPanel, BorderLayout.SOUTH);
        
        // Timer
        gameTimer = new javax.swing.Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;
                timerLabel.setText(String.format("Time: %02d:%02d", mins, secs));
            }
        });
    }

    private void generateNewPuzzle(String difficulty) {
        // Reset timer
        gameTimer.stop();
        seconds = 0;
        timerLabel.setText("Time: 00:00");
        
        // Clear arrays
        for (int i = 0; i < SIZE; i++) {
            Arrays.fill(puzzle[i], 0);
            Arrays.fill(solution[i], 0);
            Arrays.fill(fixed[i], false);
        }
        
        // Generate a complete solved sudoku
        generateSolution(0, 0);
        
        // Copy solution
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(solution[i], 0, puzzle[i], 0, SIZE);
        }
        
        // Remove numbers based on difficulty
        int cellsToRemove = switch (difficulty) {
            case "Easy" -> 35;
            case "Medium" -> 45;
            case "Hard" -> 52;
            case "Expert" -> 58;
            default -> 45;
        };
        
        removeNumbers(cellsToRemove);
        updateBoard();
        statusLabel.setText(difficulty + " puzzle generated. Good luck!");
        gameTimer.start();
    }

    private boolean generateSolution(int row, int col) {
        if (row == SIZE) return true;
        if (col == SIZE) return generateSolution(row + 1, 0);
        
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= SIZE; i++) numbers.add(i);
        Collections.shuffle(numbers);
        
        for (int num : numbers) {
            if (isValid(solution, row, col, num)) {
                solution[row][col] = num;
                if (generateSolution(row, col + 1)) return true;
                solution[row][col] = 0;
            }
        }
        return false;
    }

    private void removeNumbers(int count) {
        Random rand = new Random();
        while (count > 0) {
            int row = rand.nextInt(SIZE);
            int col = rand.nextInt(SIZE);
            if (puzzle[row][col] != 0) {
                puzzle[row][col] = 0;
                count--;
            }
        }
    }

    private void updateBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (puzzle[row][col] != 0) {
                    cells[row][col].setText(String.valueOf(puzzle[row][col]));
                    cells[row][col].setEditable(false);
                    cells[row][col].setBackground(new Color(220, 220, 220));
                    cells[row][col].setForeground(Color.BLACK);
                    fixed[row][col] = true;
                } else {
                    cells[row][col].setText("");
                    cells[row][col].setEditable(true);
                    cells[row][col].setBackground(Color.WHITE);
                    cells[row][col].setForeground(new Color(0, 100, 200));
                    fixed[row][col] = false;
                }
            }
        }
    }

    private void provideHint() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (!fixed[row][col] && cells[row][col].getText().isEmpty()) {
                    emptyCells.add(new int[]{row, col});
                }
            }
        }
        
        if (emptyCells.isEmpty()) {
            statusLabel.setText("No empty cells to provide hint!");
            return;
        }
        
        Random rand = new Random();
        int[] cell = emptyCells.get(rand.nextInt(emptyCells.size()));
        int row = cell[0];
        int col = cell[1];
        
        cells[row][col].setText(String.valueOf(solution[row][col]));
        cells[row][col].setBackground(new Color(255, 255, 200));
        statusLabel.setText("Hint provided at row " + (row + 1) + ", column " + (col + 1));
    }

    private void checkSolution() {
        boolean correct = true;
        int errors = 0;
        
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                String text = cells[row][col].getText();
                if (!text.isEmpty()) {
                    int value = Integer.parseInt(text);
                    if (value != solution[row][col]) {
                        cells[row][col].setBackground(new Color(255, 200, 200));
                        correct = false;
                        errors++;
                    } else if (!fixed[row][col]) {
                        cells[row][col].setBackground(new Color(200, 255, 200));
                    }
                }
            }
        }
        
        if (correct && isComplete()) {
            gameTimer.stop();
            statusLabel.setText("Congratulations! You solved it in " + formatTime(seconds) + "!");
            JOptionPane.showMessageDialog(this, "Puzzle solved correctly!\nTime: " + formatTime(seconds));
        } else if (errors > 0) {
            statusLabel.setText("Found " + errors + " error(s). Incorrect cells highlighted in red.");
        } else {
            statusLabel.setText("So far so good! Keep going!");
        }
    }

    private boolean isComplete() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (cells[row][col].getText().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void solvePuzzle() {
        gameTimer.stop();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                cells[row][col].setText(String.valueOf(solution[row][col]));
                if (!fixed[row][col]) {
                    cells[row][col].setBackground(new Color(200, 230, 255));
                }
            }
        }
        statusLabel.setText("Puzzle solved automatically!");
    }

    private boolean isValid(int[][] board, int row, int col, int num) {
        // Check row
        for (int c = 0; c < SIZE; c++) {
            if (board[row][c] == num) return false;
        }
        
        // Check column
        for (int r = 0; r < SIZE; r++) {
            if (board[r][col] == num) return false;
        }
        
        // Check 3x3 box
        int boxRow = row - row % SUBGRID;
        int boxCol = col - col % SUBGRID;
        for (int r = boxRow; r < boxRow + SUBGRID; r++) {
            for (int c = boxCol; c < boxCol + SUBGRID; c++) {
                if (board[r][c] == num) return false;
            }
        }
        
        return true;
    }

    private String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new AdvSudoku();
            }
        });
    }
}