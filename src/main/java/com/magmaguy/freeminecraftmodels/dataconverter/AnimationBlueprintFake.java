package com.magmaguy.freeminecraftmodels.dataconverter;

import com.magmaguy.freeminecraftmodels.utils.LoopType;

import java.util.HashMap;
import java.util.Map;

//TODO refacto en creant une classe parente/interface pour AnimationBlueprintFake et AnimationBlueprint
public class AnimationBlueprintFake extends AnimationBlueprint {

    public AnimationBlueprintFake(String name, int duration, LoopType loopType, Map<BoneBlueprint, AnimationFrame[]> frames) {
        super(null, name, null);
        this.duration = duration;
        this.loopType = loopType;
        this.animationName = name;
        this.animationFrames.putAll(frames);
    }

    @Override
    public HashMap<BoneBlueprint, AnimationFrame[]> getAnimationFrames() {
        return animationFrames;
    }
}