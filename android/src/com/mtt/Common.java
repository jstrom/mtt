package com.mtt;

public class Common
{
    static final int TYPE_NONE = 0, TYPE_X = 1, TYPE_O = 2;
    static final int PLAY_FAIL = 0, PLAY_SUCCESS = 1;

    static int whoWon(Board board)
    {

      outer:
        for (int i = 0; i < 3; i++) {
            int kind = board.whoPlayed(i,0);//board[i][0].whoWon();

            for (int j = 1; j < 3; j++) {
                if (kind != board.whoPlayed(i,j))//[i][j].whoWon())
                    continue outer;
            }
            if (kind != Common.TYPE_NONE) {
                return kind;
            }
        }

        // check cols
      outer:
        for (int j = 0; j < 3; j++) {
            int kind = board.whoPlayed(0,j);//[0][j].whoWon();

            for (int i = 1; i < 3; i++) {
                if (kind != board.whoPlayed(i,j))//[i][j].whoWon())
                    continue outer;
            }
            if (kind != Common.TYPE_NONE) {
                return kind;
            }
        }

        // check diags
        {
            int diags[][][] = {{{0,0},{1,1},{2,2}},
                               {{0,2},{1,1},{2,0}}};

          outer:
            for (int d[][] : diags) {
                int kind = board.whoPlayed(d[0][0], d[0][1]);//[d[0][0]][d[0][1]].whoWon();
                for (int i = 1; i < 3; i++) {
                    if (kind != board.whoPlayed(d[i][0], d[i][1]))//[d[i][0]][d[i][1]].whoWon())
                        continue outer;
                }

                if (kind != Common.TYPE_NONE) {
                    return kind;
                }
            }
        }

        return TYPE_NONE;
    }
}