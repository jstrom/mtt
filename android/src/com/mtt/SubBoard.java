package com.mtt;

public class SubBoard implements Board
{

    int whoWon = Common.TYPE_NONE;

    int board[][] = new int[3][3];

    public int whoWon()
    {
        return whoWon;
    }


    public int whoPlayed(int row, int col)
    {
        return board[row][col];
    }

    public int playCell(int row, int col, int type)
    {
        if (board[row][col] == Common.TYPE_NONE) {
            board[row][col] = type;
            return Common.PLAY_SUCCESS;
        }

        return Common.PLAY_FAIL; // XXXX
    }

    void checkWon()
    {
        if (whoWon > 0)
            return;

        whoWon = Common.whoWon(this);
    }
}