public class SudokuPlus {

    // Thrown when input string is not in the correct format
    class SudokuPlusFormatError extends RuntimeException {}

    // Thrown when Sudoku has no solution
    class SudokuPlusUnsolvable extends Exception {}

    // Object in each cell
    class SudokuPlusNode {
        // The fixed value of a cell, 0 if not fixed.
        int value;

        // pStates[0] = true means the value is fixed and that info has been utilised,
        // pStates[i] where 1<=i<=16 indicates whether i remains a possibility to be the value
        boolean[] pStates;

        SudokuPlusNode(int value) {
            this.value = value;
            this.pStates = new boolean[17];
            this.pStates[0] = false;
            if (value == 0) {
                for (int n = 1; n <= 16; n++) {
                    this.pStates[n] = true;
                }
            } else {
                this.pStates[value] = true;
            }
        }
    }

    // The Sudoku board
    SudokuPlusNode[][] board;

    // Solve data
    // 403 on empty board
    int n_constrainFixScout;
    // 403 on empty board
    int n_scanFix;
    // 176 on empty board
    int n_branch;

    // Parses input String to Sudoku
    // String requirement: row-major ordering, left to right, top to bottom, '0' if blank
    public SudokuPlus(String input) {
        if (input.length() != 256) throw new SudokuPlusFormatError();
        this.board = new SudokuPlusNode[16][16];
        int row, col, c, value;
        for (int i = 0; i < 256; i++) {
            row = i / 16;
            col = i % 16;
            c = input.charAt(i);
            if (c >= 'a') value = 10 + c - 'a';
            else value = c - '0';
            if (value < 0 || value > 16) throw new SudokuPlusFormatError();
            this.board[row][col] = new SudokuPlusNode(value);
            this.n_constrainFixScout = 0;
            this.n_scanFix = 0;
            this.n_branch = 0;
        }
    }



    // Checks if a Sudoku is complete and correct
    public boolean verifyComplete() {
        boolean[] present = new boolean[17];
        present[0] = true;
        // Check rows
        for (int i = 0; i < 16; i++) {
            for (int n = 1; n <= 16; n++) {
                present[n] = false;
            }
            for (int j = 0; j < 16; j++) {
                int value = this.board[i][j].value;
                if (present[value]) return false;
                present[value] = true;
            }
        }
        // Check columns
        for (int i = 0; i < 16; i++) {
            for (int n = 1; n <= 16; n++) {
                present[n] = false;
            }
            for (int j = 0; j < 16; j++) {
                int value = this.board[j][i].value;
                if (present[value]) return false;
                present[value] = true;
            }
        }
        // Check blocks
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int n = 1; n <= 16; n++) {
                    present[n] = false;
                }
                for (int k = 4 * i; k < 4 * i + 4; k++) {
                    for (int l = 4 * j; l < 4 * j + 4; l++) {
                        int value = this.board[k][l].value;
                        if (present[value]) return false;
                        present[value] = true;
                    }
                }
            }
        }
        return true;
    }

    // Pretty prints the Sudoku board
    public void prettyPrint() {
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                if (col % 4 == 0) {
                    System.out.print(" ");
                }
                if (this.board[row][col].value == 0) {
                    System.out.print("  ");
                } else if (this.board[row][col].value >= 10) {
                    System.out.print((char)(this.board[row][col].value - 10 + 'a') + " ");
                } else {
                    System.out.print(this.board[row][col].value + " ");
                }
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    // Fills the board with a valid solution
    public void solve() throws SudokuPlusUnsolvable {
        while (true) {
            int usefulness = 0;
            // Finds cell with least number of possibilities that can't be fixed, in case branching is needed
            int minNumPossibilities = 16;
            int minRow = 0;
            int minCol = 0;
            for (int row = 0; row < 16; row++) {
                for (int col = 0; col < 16; col++) {
                    int res = constrainFixScout(row, col);
                    if (res == 1) usefulness++;
                    else if (res > 1 && res < minNumPossibilities) {
                        minRow = row;
                        minCol = col;
                        minNumPossibilities = res;
                    }
                }
            }
            this.n_constrainFixScout++;
            this.n_scanFix++;
            if (scanFix() == 0 && usefulness == 0) {
                if (this.verifyComplete()) return;
                else {
                    // Deterministic methods no longer make progress, branching required
                    if (branch(minRow, minCol)) return;
                    else throw new SudokuPlusUnsolvable();
                }
            }

        }
    }

    // Prints information on the cost of solve
    public void printSolveData() {
        System.out.println("Parallel space usage (extra boards): " + this.n_branch);
        System.out.println("Times constrainFixScout ran on board: " + this.n_constrainFixScout);
        System.out.println("Times scanFix ran on board: " + this.n_scanFix);
        System.out.print("\n");
    }

    // Branching out when deterministic solving is insufficient, returns false if unsolvable
    boolean branch(int row, int col) {
        boolean success = false;
        // Tries solve on each possible branch if the previous branch fails
        for (int n = 1; n <= 16; n++) {
            if (this.board[row][col].pStates[n]) {
                SudokuPlus clone = this.fork();
                clone.board[row][col].value = n;
                for (int m = 1; m <= 16; m++) {
                    if (m != n) clone.board[row][col].pStates[m] = false;
                }
                try {
                    clone.solve();
                    // Success if program reaches here
                    this.copyBack(clone);
                    success = true;
                } catch (SudokuPlusUnsolvable u) {
                    // Failure
                }
            }
            if (success) break;
        }
        return success;
    }

    // Makes a new Sudoku object with same info, except incremented n_branch
    SudokuPlus fork() {
        SudokuPlus clone = new SudokuPlus("0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        clone.board = new SudokuPlusNode[16][16];
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                clone.board[row][col] = new SudokuPlusNode(this.board[row][col].value);
                for (int i = 0; i < 17; i++) {
                    clone.board[row][col].pStates[i] = this.board[row][col].pStates[i];
                }
            }
        }
        clone.n_constrainFixScout = this.n_constrainFixScout;
        clone.n_scanFix = this.n_scanFix;
        clone.n_branch = this.n_branch + 1;
        return clone;
    }

    // Copies given Sudoku object into this
    void copyBack(SudokuPlus clone) {
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                this.board[row][col] = clone.board[row][col];
                for (int i = 0; i < 10; i++) {
                    this.board[row][col].pStates[i] = clone.board[row][col].pStates[i];
                }
            }
        }
        this.n_constrainFixScout = clone.n_constrainFixScout;
        this.n_scanFix = clone.n_scanFix;
        this.n_branch = clone.n_branch;
    }

    // If value is fixed, take away possibilities from related cells (utilising the info), return 0 if already utilised and 1 if not
    // If value is not fixed, check and return number of possibilities and if only one, fix
    int constrainFixScout(int row, int col) throws SudokuPlusUnsolvable {
        if (this.board[row][col].value != 0 && this.board[row][col].pStates[0]) {
            // Fixed and utilised
            return 0;
        } else if (this.board[row][col].value != 0) {
            // Fixed and not utilised, utilise
            int value = this.board[row][col].value;
            for (int i = 0; i < 16; i++) {
                // Eliminate value from cells in the same row
                if (i != col) this.board[row][i].pStates[value] = false;
                // Eliminate value from cells in the same column
                if (i != row) this.board[i][col].pStates[value] = false;
                // Eliminate value from cells in the same block
                if (i / 4 == row / 4) {
                    for (int j = (col / 4) * 4; j / 4 == col / 4; j++) {
                        if (i != row || j != col) this.board[i][j].pStates[value] = false;
                    }
                }
            }
            this.board[row][col].pStates[0] = true;
            return 1;
        } else {
            // Not fixed
            int numSeenPossibility = 0;
            int value = 0;
            for (int n = 1; n <= 16; n++) {
                if (this.board[row][col].pStates[n]) {
                    value = n;
                    numSeenPossibility++;
                }
            }
            if (numSeenPossibility == 1) {
                this.board[row][col].value = value;
            } else if (numSeenPossibility == 0) throw new SudokuPlusUnsolvable();
            return numSeenPossibility;
        }
    }

    // Scans rows, columns and blocks to see if values can be fixed, returns number of values fixed.
    int scanFix() throws SudokuPlusUnsolvable {
        int fixed = 0;
        for (int n = 1; n <= 16; n++) {
            // Scanning rows and columns
            for (int i = 0; i < 16; i++) {
                if (scanFixRow(i, n)) fixed++;
                if (scanFixCol(i, n)) fixed++;
            }
            // Scanning blocks
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (scanFixBlock(i, j, n)) fixed++;
                }
            }
        }
        return fixed;
    }

    // Scans a row to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixRow(int row, int n) throws SudokuPlusUnsolvable {
        boolean seenPosition;
        int position;
        seenPosition = false;
        position = -1;
        for (int i = 0; i < 16; i++) {
            if (this.board[row][i].value == n) return false;
            else if (this.board[row][i].pStates[n]) {
                if (seenPosition) return false;
                seenPosition = true;
                position = i;
            }
        }
        if (seenPosition) {
            this.board[row][position].value = n;
            for (int m = 1; m <= 16; m++) {
                if (m != n) this.board[row][position].pStates[m] = false;
            }
        } else if (!seenPosition) throw new SudokuPlusUnsolvable();
        return true;
    }

    // Scans a column to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixCol(int col, int n) throws SudokuPlusUnsolvable {
        boolean seenPosition;
        int position;
        seenPosition = false;
        position = -1;
        for (int i = 0; i < 16; i++) {
            if (this.board[i][col].value == n) return false;
            else if (this.board[i][col].pStates[n]) {
                if (seenPosition) return false;
                seenPosition = true;
                position = i;
            }
        }
        if (seenPosition) {
            this.board[position][col].value = n;
            for (int m = 1; m <= 16; m++) {
                if (m != n) this.board[position][col].pStates[m] = false;
            }
        } else if (!seenPosition) throw new SudokuPlusUnsolvable();
        return true;
    }

    // Scans a block to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixBlock(int i, int j, int n) throws SudokuPlusUnsolvable {
        boolean seenPosition = false;
        int uniqueRow = -1;
        int uniqueCol = -1;
        for (int row = 4 * i; row < 4 * i + 4; row++) {
            for (int col = 4 * j; col < 4 * j + 4; col++) {
                if (this.board[row][col].value == n) return false;
                else if (this.board[row][col].pStates[n]) {
                    if (seenPosition) return false;
                    uniqueRow = row;
                    uniqueCol = col;
                    seenPosition = true;
                }
            }
        }
        if (seenPosition) {
            this.board[uniqueRow][uniqueCol].value = n;
            for (int m = 1; m <= 16; m++) {
                if (m != n) this.board[uniqueRow][uniqueCol].pStates[m] = false;
            }
        } else throw new SudokuPlusUnsolvable();
        return true;
    }

}
