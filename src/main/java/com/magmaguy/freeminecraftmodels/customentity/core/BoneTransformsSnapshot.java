package com.magmaguy.freeminecraftmodels.customentity.core;

public class BoneTransformsSnapshot {
    public final double xRotation, yRotation, zRotation;
    public final double xTranslation, yTranslation, zTranslation;

    public BoneTransformsSnapshot(double xRot, double yRot, double zRot,
                                  double xTrans, double yTrans, double zTrans) {
        this.xRotation = xRot;
        this.yRotation = yRot;
        this.zRotation = zRot;
        this.xTranslation = xTrans;
        this.yTranslation = yTrans;
        this.zTranslation = zTrans;
    }

    public BoneTransformsSnapshot() {
        this.xRotation = 0;
        this.yRotation = 0;
        this.zRotation = 0;
        this.xTranslation = 0;
        this.yTranslation = 0;
        this.zTranslation = 0;
    }

    public BoneTransformsSnapshot interpolateTo(BoneTransformsSnapshot target, double progress) {
        return new BoneTransformsSnapshot(
                linearInterpolate(xRotation, target.xRotation, progress),
                linearInterpolate(yRotation, target.yRotation, progress),
                linearInterpolate(zRotation, target.zRotation, progress),
                linearInterpolate(xTranslation, target.xTranslation, progress),
                linearInterpolate(yTranslation, target.yTranslation, progress),
                linearInterpolate(zTranslation, target.zTranslation, progress)
        );
    }

    private double linearInterpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    @Override
    public String toString() {
        return "BoneTransformsSnapshot{" +
                "xRotation=" + xRotation +
                ", yRotation=" + yRotation +
                ", zRotation=" + zRotation +
                ", xTranslation=" + xTranslation +
                ", yTranslation=" + yTranslation +
                ", zTranslation=" + zTranslation +
                '}';
    }
}

