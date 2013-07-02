package com.mtt;

public class TopBoard implements Board
{
    static final int N = 3;

    SubBoard board[][] = new SubBoard[3][3];

    int whoWon = Common.TYPE_NONE;

    public TopBoard()
    {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                board[i][j] = new SubBoard();
    }


    public int whoWon()
    {
        // check rows
        if (whoWon > 0)
            return whoWon;

        return 0;
    }

    public int whoPlayed(int row, int col)
    {
        return board[row][col].whoWon();
    }

    public void checkWon()
    {
        if (whoWon > 0)
            return;

        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                board[i][j].checkWon();

        whoWon = Common.whoWon(this);

    }


    public int playCell(int row, int col, int type)
    {
        return Common.PLAY_FAIL; // can't explicitly play on top board
    }
}