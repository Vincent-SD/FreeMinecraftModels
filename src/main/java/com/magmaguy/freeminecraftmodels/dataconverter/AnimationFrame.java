package com.magmaguy.freeminecraftmodels.dataconverter;

public class AnimationFrame {
    public float xRotation;
    public float yRotation;
    public float zRotation;
    public float xPosition;
    public float yPosition;
    public float zPosition;
    public float xScale;
    public float yScale;
    public float zScale;

    public AnimationFrame(float xRotation, float yRotation, float zRotation, float xPosition, float yPosition, float zPosition) {
        this.xRotation = xRotation;
        this.yRotation = yRotation;
        this.zRotation = zRotation;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.zPosition = zPosition;
    }

    public AnimationFrame(){
    }

    @Override
    public String toString() {
        return "AnimationFrame{" +
                "xRotation=" + xRotation +
                ", yRotation=" + yRotation +
                ", zRotation=" + zRotation +
                ", xPosition=" + xPosition +
                ", yPosition=" + yPosition +
                ", zPosition=" + zPosition +
                ", xScale=" + xScale +
                ", yScale=" + yScale +
                ", zScale=" + zScale +
                '}';
    }
}
