import java.util.*;

public class Tabuleiro {
    // Board Representation
    private int[][] board; // 8x8 board: 0=Empty, 1=White, 2=Black, 3=White King, 4=Black King
    private static final int EMPTY = 0;
    private static final int WHITE = 1;
    private static final int BLACK = 2;
    private static final int WHITE_KING = 3;
    private static final int BLACK_KING = 4;

    // Coordinate Mapping
    private static final Map<Character, Integer> colMap = new HashMap<>();
    private static final Map<Character, Integer> rowMap = new HashMap<>();
    private static final Map<Integer, Character> revColMap = new HashMap<>();
    private static final Map<Integer, Character> revRowMap = new HashMap<>();

    // Piece Counts
    private int whiteCount = 0;
    private int blackCount = 0;

    // Optimization: Track active pieces
    private Set<String> whitePositions = new HashSet<>(); // e.g., "A1", "C3"
    private Set<String> blackPositions = new HashSet<>();

    // Game State
    private int currentPlayer = WHITE; // 1 starts
    private boolean inMultiCapture = false;
    private String activePieceCoord = null; // The piece that must continue capturing

    static {
        // Initialize Maps
        char[] cols = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'};
        for (int i = 0; i < 8; i++) {
            colMap.put(cols[i], i);
            revColMap.put(i, cols[i]);
            // Rows are 1-8. In array, 0 is row 1, 7 is row 8.
            rowMap.put((char)('1' + i), i);
            revRowMap.put(i, (char)('1' + i));
        }
    }

    public Tabuleiro() {
        board = new int[8][8];
        initializeBoard();
    }

    private void initializeBoard() {
        // Clear board
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = EMPTY;
            }
        }
        whitePositions.clear();
        blackPositions.clear();
        whiteCount = 0;
        blackCount = 0;
        currentPlayer = WHITE;
        inMultiCapture = false;
        activePieceCoord = null;

        // Place White pieces (rows 0, 1, 2) on dark squares
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 0) {
                    addPiece(r, c, WHITE);
                }
            }
        }

        // Place Black pieces (rows 5, 6, 7) on dark squares
        for (int r = 5; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 0) {
                    addPiece(r, c, BLACK);
                }
            }
        }
    }

    private void addPiece(int r, int c, int type) {
        board[r][c] = type;
        String pos = toCoord(r, c);
        if (type == WHITE || type == WHITE_KING) {
            whitePositions.add(pos);
            whiteCount++;
        } else if (type == BLACK || type == BLACK_KING) {
            blackPositions.add(pos);
            blackCount++;
        }
    }

    private void removePiece(int r, int c) {
        int type = board[r][c];
        String pos = toCoord(r, c);
        board[r][c] = EMPTY;
        if (type == WHITE || type == WHITE_KING) {
            whitePositions.remove(pos);
            whiteCount--;
        } else if (type == BLACK || type == BLACK_KING) {
            blackPositions.remove(pos);
            blackCount--;
        }
    }

    private void movePiece(int r1, int c1, int r2, int c2) {
        int type = board[r1][c1];
        String pos1 = toCoord(r1, c1);
        String pos2 = toCoord(r2, c2);

        board[r1][c1] = EMPTY;
        board[r2][c2] = type;

        if (type == WHITE || type == WHITE_KING) {
            whitePositions.remove(pos1);
            whitePositions.add(pos2);
        } else {
            blackPositions.remove(pos1);
            blackPositions.add(pos2);
        }
    }

    // Convert (row, col) to "A1"
    private String toCoord(int r, int c) {
        if (r < 0 || r > 7 || c < 0 || c > 7) return null;
        return "" + revColMap.get(c) + revRowMap.get(r);
    }

    // Convert "A1" to int[]{r, c}
    private int[] fromCoord(String coord) {
        if (coord == null || coord.length() < 2) return null;
        char colChar = coord.charAt(0);
        char rowChar = coord.charAt(1);
        if (!colMap.containsKey(colChar) || !rowMap.containsKey(rowChar)) return null;
        return new int[]{rowMap.get(rowChar), colMap.get(colChar)};
    }

    // Core Game Logic

    public String makeMove(String from, String to) {
        // Validate input format
        int[] start = fromCoord(from);
        int[] end = fromCoord(to);
        if (start == null || end == null) return "Invalid coordinates";

        // Generate all valid moves for the current state
        List<Move> validMoves = getValidMoves(currentPlayer);

        // Check if the requested move is in the list of valid moves
        Move requestedMove = null;
        for (Move m : validMoves) {
            if (m.r1 == start[0] && m.c1 == start[1] && m.r2 == end[0] && m.c2 == end[1]) {
                requestedMove = m;
                break;
            }
        }

        if (requestedMove == null) {
            return "Invalid move";
        }

        // Execute the move
        executeMove(requestedMove);

        // Check win condition
        int winner = checkWinner();
        if (winner != 0) {
            return "Game Over. Winner: " + (winner == WHITE ? "White" : "Black");
        }

        return "Move executed";
    }

    private void executeMove(Move move) {
        // Move the piece
        movePiece(move.r1, move.c1, move.r2, move.c2);

        // Handle capture
        boolean captured = false;
        if (move.capturedR != -1) {
            removePiece(move.capturedR, move.capturedC);
            captured = true;
        }

        // Handle Promotion (Man -> King)
        // Check if piece is Man and reached end row
        int type = board[move.r2][move.c2];
        if (type == WHITE && move.r2 == 7) {
            board[move.r2][move.c2] = WHITE_KING;
        } else if (type == BLACK && move.r2 == 0) {
            board[move.r2][move.c2] = BLACK_KING;
        }

        // Multi-capture logic
        if (captured) {
            // Check if this piece can capture again
            // We need to generate capture moves specifically for this piece at new position
            // But strict rule: "First capture... forward... subsequent... any".
            // Since we just captured, we are now in "subsequent" phase if we continue.
            // So we pass 'false' for isFirstCapture to allow backward captures for Men.

            // However, we need to know if there ARE any captures available for THIS piece.
            List<Move> nextCaptures = getCapturesForPiece(move.r2, move.c2, currentPlayer, false);

            if (!nextCaptures.isEmpty()) {
                inMultiCapture = true;
                activePieceCoord = toCoord(move.r2, move.c2);
                return; // Turn continues
            }
        }

        // End turn
        inMultiCapture = false;
        activePieceCoord = null;
        currentPlayer = (currentPlayer == WHITE) ? BLACK : WHITE;
    }

    // Check winner: 0=None, 1=White, 2=Black
    public int checkWinner() {
        if (blackCount == 0) return WHITE;
        if (whiteCount == 0) return BLACK;

        // Check if current player has moves
        List<Move> moves = getValidMoves(currentPlayer);
        if (moves.isEmpty()) {
            return (currentPlayer == WHITE) ? BLACK : WHITE;
        }
        return 0;
    }

    // Move class to store move details
    private static class Move {
        int r1, c1, r2, c2;
        int capturedR = -1, capturedC = -1; // -1 if no capture

        Move(int r1, int c1, int r2, int c2) {
            this.r1 = r1; this.c1 = c1;
            this.r2 = r2; this.c2 = c2;
        }

        Move(int r1, int c1, int r2, int c2, int cr, int cc) {
            this(r1, c1, r2, c2);
            this.capturedR = cr;
            this.capturedC = cc;
        }
    }

    // Get all valid moves for a player
    private List<Move> getValidMoves(int player) {
        List<Move> moves = new ArrayList<>();
        List<Move> captures = new ArrayList<>();

        // If in multi-capture, only the active piece can move
        if (inMultiCapture) {
            int[] pos = fromCoord(activePieceCoord);
            if (pos != null) {
                // For subsequent captures, Man can capture backwards. So isFirstCapture = false
                captures.addAll(getCapturesForPiece(pos[0], pos[1], player, false));
            }
            return captures; // Must capture if possible (logic ensures captures check first)
        }

        // Otherwise, check all pieces
        Set<String> pieces = (player == WHITE) ? whitePositions : blackPositions;

        // 1. Check for captures (Mandatory)
        for (String posStr : pieces) {
            int[] pos = fromCoord(posStr);
            // First capture of sequence -> isFirstCapture = true
            captures.addAll(getCapturesForPiece(pos[0], pos[1], player, true));
        }

        if (!captures.isEmpty()) {
            return captures; // Mandatory capture
        }

        // 2. If no captures, check normal moves
        for (String posStr : pieces) {
            int[] pos = fromCoord(posStr);
            moves.addAll(getNormalMovesForPiece(pos[0], pos[1], player));
        }

        return moves;
    }

    private List<Move> getCapturesForPiece(int r, int c, int player, boolean isFirstCapture) {
        List<Move> captures = new ArrayList<>();
        int type = board[r][c];
        boolean isKing = (type == WHITE_KING || type == BLACK_KING);

        int[] dr, dc;
        if (isKing) {
            dr = new int[]{1, 1, -1, -1};
            dc = new int[]{1, -1, 1, -1};
        } else {
            // Man
            if (isFirstCapture) {
                 // Must capture forward
                 // White (1): Forward is +1. Black (2): Forward is -1.
                 int forward = (player == WHITE) ? 1 : -1;
                 dr = new int[]{forward, forward};
                 dc = new int[]{1, -1};
            } else {
                 // Subsequent captures: Any direction
                 dr = new int[]{1, 1, -1, -1};
                 dc = new int[]{1, -1, 1, -1};
            }
        }

        for (int i = 0; i < dr.length; i++) {
            if (isKing) {
                // Flying King Capture Logic
                // Can move any number of squares diagonally before capturing?
                // "Flying Kings: Can move any number of squares diagonally."
                // "Can capture backwards."
                // "Landing Logic: Upon capturing, the King must land exactly on the square immediately following the last captured piece in that direction."

                // Logic: Search along direction (dr[i], dc[i])
                // Find the first piece.
                // If it's enemy, check if square BEHIND it is empty.
                // If yes, that's a valid capture.
                // Can king fly BEFORE capturing? Yes.

                int currR = r + dr[i];
                int currC = c + dc[i];
                while (isValidPos(currR, currC) && board[currR][currC] == EMPTY) {
                    currR += dr[i];
                    currC += dc[i];
                }

                // Now we hit a piece or edge
                if (isValidPos(currR, currC)) {
                    int piece = board[currR][currC];
                    if (isEnemy(player, piece)) {
                        // Check landing spot (must be immediately after)
                        int landR = currR + dr[i];
                        int landC = currC + dc[i];
                        if (isValidPos(landR, landC) && board[landR][landC] == EMPTY) {
                            captures.add(new Move(r, c, landR, landC, currR, currC));
                        }
                    }
                }
            } else {
                // Man Capture Logic (Standard step)
                // Jump over enemy
                int midR = r + dr[i];
                int midC = c + dc[i];
                int destR = r + 2 * dr[i];
                int destC = c + 2 * dc[i];

                if (isValidPos(destR, destC) && board[destR][destC] == EMPTY) {
                    if (isValidPos(midR, midC)) {
                        int midPiece = board[midR][midC];
                        if (isEnemy(player, midPiece)) {
                            captures.add(new Move(r, c, destR, destC, midR, midC));
                        }
                    }
                }
            }
        }

        return captures;
    }

    private List<Move> getNormalMovesForPiece(int r, int c, int player) {
        List<Move> moves = new ArrayList<>();
        int type = board[r][c];
        boolean isKing = (type == WHITE_KING || type == BLACK_KING);

        int[] dr, dc;
        if (isKing) {
             dr = new int[]{1, 1, -1, -1};
             dc = new int[]{1, -1, 1, -1};

             // Flying King Move
             for (int i = 0; i < 4; i++) {
                 int currR = r + dr[i];
                 int currC = c + dc[i];
                 while (isValidPos(currR, currC) && board[currR][currC] == EMPTY) {
                     moves.add(new Move(r, c, currR, currC));
                     currR += dr[i];
                     currC += dc[i];
                 }
             }
        } else {
             // Man Move (Forward only)
             int forward = (player == WHITE) ? 1 : -1;
             dr = new int[]{forward, forward};
             dc = new int[]{1, -1};

             for (int i = 0; i < 2; i++) {
                 int destR = r + dr[i];
                 int destC = c + dc[i];
                 if (isValidPos(destR, destC) && board[destR][destC] == EMPTY) {
                     moves.add(new Move(r, c, destR, destC));
                 }
             }
        }
        return moves;
    }

    private boolean isValidPos(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private boolean isEnemy(int player, int piece) {
        if (piece == EMPTY) return false;
        if (player == WHITE) {
            return piece == BLACK || piece == BLACK_KING;
        } else {
            return piece == WHITE || piece == WHITE_KING;
        }
    }

    // Helper for debugging/testing
    public void printBoard() {
        for (int r = 7; r >= 0; r--) {
            System.out.print((r + 1) + " ");
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                String sym = ".";
                if (p == WHITE) sym = "w";
                if (p == BLACK) sym = "b";
                if (p == WHITE_KING) sym = "W";
                if (p == BLACK_KING) sym = "B";
                System.out.print(sym + " ");
            }
            System.out.println();
        }
        System.out.println("  A B C D E F G H");
    }

    // For testing purposes
    public void clearBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                board[r][c] = EMPTY;
            }
        }
        whitePositions.clear();
        blackPositions.clear();
        whiteCount = 0;
        blackCount = 0;
        currentPlayer = WHITE;
        inMultiCapture = false;
        activePieceCoord = null;
    }

    public void setPiece(String coord, int type) {
        int[] pos = fromCoord(coord);
        if (pos != null) {
            board[pos[0]][pos[1]] = type;
            // Determine if it's white or black to add to sets
            if (type == WHITE || type == WHITE_KING) {
                whitePositions.add(coord);
                whiteCount++;
            } else if (type == BLACK || type == BLACK_KING) {
                blackPositions.add(coord);
                blackCount++;
            }
        }
    }

    public void setTurn(int player) {
        this.currentPlayer = player;
    }

    public static void main(String[] args) {
        Tabuleiro t = new Tabuleiro();
        t.printBoard();

        Scanner scanner = new Scanner(System.in);
        while (t.checkWinner() == 0) {
            System.out.println("Current Player: " + (t.currentPlayer == WHITE ? "White" : "Black"));
            System.out.print("Enter move (e.g. C3 D4): ");
            String line = scanner.nextLine();
            if (line.equalsIgnoreCase("exit")) break;
            String[] parts = line.split("\\s+");
            if (parts.length == 2) {
                String res = t.makeMove(parts[0].toUpperCase(), parts[1].toUpperCase());
                System.out.println(res);
                t.printBoard();
            } else {
                System.out.println("Invalid input format.");
            }
        }
    }
}
