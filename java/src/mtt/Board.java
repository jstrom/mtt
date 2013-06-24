package mtt;

public class Board implements Winnable
{
    static final int N = 3;

    SubBoard board[][] = new SubBoard[3][3];

    int whoWon = Common.TYPE_NONE;

    public Board()
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

    private void checkWon()
    {
        if (whoWon > 0)
            return;

        whoWon = Common.whoWon(board);

    }
}