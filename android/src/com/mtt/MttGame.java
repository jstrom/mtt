package com.mtt;
import java.util.*;

// Stores all the current state about the game, including board state
public class MttGame
{

    TopBoard current;

    // part of the game state
    int lastSubCol = -1;
    int lastSubRow = -1;
    int lastPlayer = -1;


    public int takeAction(Action a)
    {
        return takeAction(a.row, a.col, a.subRow, a.subCol, a.player);
    }

    public int takeAction(int r, int c, int sr, int sc, int player)
    {
        // XXX enforce rules here?

        lastSubRow = sr;
        lastSubCol = sc;
        lastPlayer = player;

        // XXX Copy on write?
        return current.board[r][c].playCell(sr, sc, player);
    }


    public static class Action
    {
        int row, col, subRow, subCol, player;
        public Action(int r, int c, int sr, int sc, int player)
        {
            this.row = r;
            this.col = c;
            this.subRow = sr;
            this.subCol = sc;
            this.player = player;
        }
    }


    // this could be useful for an AI...
    public ArrayList<Action> enumerateActions()
    {
        return new ArrayList();
    }
}