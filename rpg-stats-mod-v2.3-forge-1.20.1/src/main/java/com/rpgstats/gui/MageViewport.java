package com.rpgstats.gui;

/** One coordinate system for rendering and native widget hit tests at every GUI scale. */
public record MageViewport(double scale, double x, double y) {
    public static MageViewport fit(int width,int height) {
        double scale=Math.min(width/820.0,height/470.0);
        return new MageViewport(scale,(width-820*scale)/2,(height-470*scale)/2);
    }
    public double localX(double screenX) { return (screenX-x)/scale; }
    public double localY(double screenY) { return (screenY-y)/scale; }
}
