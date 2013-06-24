package mtt;

public interface Winnable
{

    abstract int whoWon();
    /*
    static final int WON_EMPTY = 0, WON_X = 1, WON_O = 2, WON_OTHER = 3;

    abstract boolean whoWon(int i, int j);

    static boolean isWon(Winnable foo)
    {
        // check rows

        for (int i = 0; i < 3; i++) {
            int kind = WON_OTHER;
            for (int j = 0; j < za3; j++) {
                foo.whoWon(i,j);
            }
        }

        // check cols

        // check diags


    }
    */
}