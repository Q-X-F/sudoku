public class Sudoku {

    // Thrown when input string is not in the correct format
    class SudokuFormatError extends RuntimeException {}

    // Thrown when Sudoku has no solution
    class SudokuUnsolvable extends Exception {}

    // Object in each cell
    class SudokuNode {
        // The fixed value of a cell, 0 if not fixed.
        int value;

        // pStates[0] = true means the value is fixed and that info has been utilised,
        // pStates[i] where 1<=i<=9 indicates whether i remains a possibility to be the value
        boolean[] pStates;

        SudokuNode(int value) {
            this.value = value;
            this.pStates = new boolean[10];
            this.pStates[0] = false;
            if (value == 0) {
                for (int n = 1; n <= 9; n++) {
                    this.pStates[n] = true;
                }
            } else {
                this.pStates[value] = true;
            }
        }
    }

    // The Sudoku board
    SudokuNode[][] board;

    // Solve data
    // 114 on empty board
    int n_constrainFixScout;
    // 114 on empty board
    int n_scanFix;
    // 47 on empty board
    int n_branch;

    // Parses input String to Sudoku
    // String requirement: row-major ordering, left to right, top to bottom, '0' if blank
    public Sudoku(String input) {
        if (input.length() != 81) throw new SudokuFormatError();
        this.board = new SudokuNode[9][9];
        int row, col, value;
        for (int i = 0; i < 81; i++) {
            row = i / 9;
            col = i % 9;
            value = input.charAt(i) - '0';
            if (value < 0 || value > 9) throw new SudokuFormatError();
            this.board[row][col] = new SudokuNode(value);
            this.n_constrainFixScout = 0;
            this.n_scanFix = 0;
            this.n_branch = 0;
        }
    }



    // Checks if a Sudoku is complete and correct
    public boolean verifyComplete() {
        boolean[] present = new boolean[10];
        present[0] = true;
        // Check rows
        for (int i = 0; i < 9; i++) {
            for (int n = 1; n <= 9; n++) {
                present[n] = false;
            }
            for (int j = 0; j < 9; j++) {
                int value = this.board[i][j].value;
                if (present[value]) return false;
                present[value] = true;
            }
        }
        // Check columns
        for (int i = 0; i < 9; i++) {
            for (int n = 1; n <= 9; n++) {
                present[n] = false;
            }
            for (int j = 0; j < 9; j++) {
                int value = this.board[j][i].value;
                if (present[value]) return false;
                present[value] = true;
            }
        }
        // Check blocks
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int n = 1; n <= 9; n++) {
                    present[n] = false;
                }
                for (int k = 3 * i; k < 3 * i + 3; k++) {
                    for (int l = 3 * j; l < 3 * j + 3; l++) {
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
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (col % 3 == 0) {
                    System.out.print(" ");
                }
                if (this.board[row][col].value == 0) {
                    System.out.print("  ");
                } else {
                    System.out.print(this.board[row][col].value + " ");
                }
            }
            System.out.print('\n');
        }
        System.out.print('\n');
    }

    // Fills the board with a valid solution
    public void solve() throws SudokuUnsolvable {
        while (true) {
            int usefulness = 0;
            // Finds cell with least number of possibilities that can't be fixed, in case branching is needed
            int minNumPossibilities = 9;
            int minRow = 0;
            int minCol = 0;
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
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
                    else throw new SudokuUnsolvable();
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
        for (int n = 1; n <= 9; n++) {
            if (this.board[row][col].pStates[n]) {
                Sudoku clone = this.fork();
                clone.board[row][col].value = n;
                for (int m = 1; m <= 9; m++) {
                    if (m != n) clone.board[row][col].pStates[m] = false;
                }
                try {
                    clone.solve();
                    // Success if program reaches here
                    this.copyBack(clone);
                    success = true;
                } catch (SudokuUnsolvable u) {
                    // Failure
                }
            }
            if (success) break;
        }
        return success;
    }

    // Makes a new Sudoku object with same info, except incremented n_branch
    Sudoku fork() {
        Sudoku clone = new Sudoku("000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        clone.board = new SudokuNode[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                clone.board[row][col] = new SudokuNode(this.board[row][col].value);
                for (int i = 0; i < 10; i++) {
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
    void copyBack(Sudoku clone) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
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
    int constrainFixScout(int row, int col) throws SudokuUnsolvable{
        if (this.board[row][col].value != 0 && this.board[row][col].pStates[0]) {
            // Fixed and utilised
            return 0;
        } else if (this.board[row][col].value != 0) {
            // Fixed and not utilised, utilise
            int value = this.board[row][col].value;
            for (int i = 0; i < 9; i++) {
                // Eliminate value from cells in the same row
                if (i != col) this.board[row][i].pStates[value] = false;
                // Eliminate value from cells in the same column
                if (i != row) this.board[i][col].pStates[value] = false;
                // Eliminate value from cells in the same block
                if (i / 3 == row / 3) {
                    for (int j = (col / 3) * 3; j / 3 == col / 3; j++) {
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
            for (int n = 1; n <= 9; n++) {
                if (this.board[row][col].pStates[n]) {
                    value = n;
                    numSeenPossibility++;
                }
            }
            if (numSeenPossibility == 1) {
                this.board[row][col].value = value;
            } else if (numSeenPossibility == 0) throw new SudokuUnsolvable();
            return numSeenPossibility;
        }
    }

    // Scans rows, columns and blocks to see if values can be fixed, returns number of values fixed.
    int scanFix() throws SudokuUnsolvable {
        int fixed = 0;
        for (int n = 1; n <= 9; n++) {
            // Scanning rows and columns
            for (int i = 0; i < 9; i++) {
                if (scanFixRow(i, n)) fixed++;
                if (scanFixCol(i, n)) fixed++;
            }
            // Scanning blocks
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (scanFixBlock(i, j, n)) fixed++;
                }
            }
        }
        return fixed;
    }

    // Scans a row to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixRow(int row, int n) throws SudokuUnsolvable {
        boolean seenPosition;
        int position;
        seenPosition = false;
        position = -1;
        for (int i = 0; i < 9; i++) {
            if (this.board[row][i].value == n) return false;
            else if (this.board[row][i].pStates[n]) {
                if (seenPosition) return false;
                seenPosition = true;
                position = i;
            }
        }
        if (seenPosition) {
            this.board[row][position].value = n;
            for (int m = 1; m <= 9; m++) {
                if (m != n) this.board[row][position].pStates[m] = false;
            }
        } else if (!seenPosition) throw new SudokuUnsolvable();
        return true;
    }

    // Scans a column to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixCol(int col, int n) throws SudokuUnsolvable {
        boolean seenPosition;
        int position;
        seenPosition = false;
        position = -1;
        for (int i = 0; i < 9; i++) {
            if (this.board[i][col].value == n) return false;
            else if (this.board[i][col].pStates[n]) {
                if (seenPosition) return false;
                seenPosition = true;
                position = i;
            }
        }
        if (seenPosition) {
            this.board[position][col].value = n;
            for (int m = 1; m <= 9; m++) {
                if (m != n) this.board[position][col].pStates[m] = false;
            }
        } else if (!seenPosition) throw new SudokuUnsolvable();
        return true;
    }

    // Scans a block to see if values can be fixed, returns whether it has fixed one value.
    boolean scanFixBlock(int i, int j, int n) throws SudokuUnsolvable {
        boolean seenPosition = false;
        int uniqueRow = -1;
        int uniqueCol = -1;
        for (int row = 3 * i; row < 3 * i + 3; row++) {
            for (int col = 3 * j; col < 3 * j + 3; col++) {
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
            for (int m = 1; m <= 9; m++) {
                if (m != n) this.board[uniqueRow][uniqueCol].pStates[m] = false;
            }
        } else throw new SudokuUnsolvable();
        return true;
    }



}


