package com.rpgstats.gui;

/** Fixed canvas fitted to the window, shared by clipping and widget hit testing. */
public final class GuideLayout {
    public static final int BOOK_X=20,BOOK_Y=18,BOOK_W=780,BOOK_H=434;
    public static final int CONTENT_TOP=36,CONTENT_BOTTOM=410;
    public static int scrollLimit(int contentBottom){return Math.max(0,contentBottom-CONTENT_BOTTOM);}
    private GuideLayout(){}
}
