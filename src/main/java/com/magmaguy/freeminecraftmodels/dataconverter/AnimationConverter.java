package com.magmaguy.freeminecraftmodels.dataconverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magmaguy.freeminecraftmodels.MetadataHandler;
import com.magmaguy.magmacore.util.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class AnimationConverter {

    private static final File INPUT_DIR = new File(MetadataHandler.PLUGIN.getDataFolder(), "animations_input");
    private static final File OUTPUT_DIR = new File(MetadataHandler.PLUGIN.getDataFolder(), "animations");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void convertAll() {
        if (!INPUT_DIR.exists() || !INPUT_DIR.isDirectory()) {
            Logger.warn("animations_input directory not found.");
            return;
        }

        File[] files = INPUT_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            Logger.warn("No animation input files found.");
            return;
        }

        OUTPUT_DIR.mkdirs();

        int count = 0;
        for (File inputFile : files) {
            try (FileReader reader = new FileReader(inputFile)) {
                Map<String, Object> inputData = gson.fromJson(reader, Map.class);
                if (!inputData.containsKey("animations")) {
                    Logger.warn("Skipping file without 'animations': " + inputFile.getName());
                    continue;
                }

                Map<String, Object> animationsMap = (Map<String, Object>) inputData.get("animations");

                for (Map.Entry<String, Object> entry : animationsMap.entrySet()) {
                    String fullName = entry.getKey();
                    Map<String, Object> ajAnim = (Map<String, Object>) entry.getValue();

                    Map<String, Object> bbAnim = convertAnimation(fullName, ajAnim);

                    String filename = fullName.contains(".") ? fullName.substring(fullName.lastIndexOf('.') + 1) : fullName;
                    File outFile = new File(OUTPUT_DIR, filename + ".json");

                    try (FileWriter writer = new FileWriter(outFile)) {
                        gson.toJson(bbAnim, writer);
                        count++;
                    }
                }

            } catch (Exception e) {
                Logger.warn("Failed to convert animation file: " + inputFile.getName() + " - " + e.getMessage());
            }
        }

        Logger.info("Converted " + count + " animation(s) to Blockbench format in 'animations/'");
    }

    private static Map<String, Object> convertAnimation(String animName, Map<String, Object> ajAnim) {
        Map<String, Object> bbAnim = new LinkedHashMap<>();
        bbAnim.put("uuid", UUID.randomUUID().toString());
        bbAnim.put("name", animName.contains(".") ? animName.substring(animName.lastIndexOf('.') + 1) : animName);
        bbAnim.put("loop", Boolean.TRUE.equals(ajAnim.get("loop")) ? "loop" : "once");
        bbAnim.put("override", false);
        bbAnim.put("length", ajAnim.getOrDefault("animation_length", 1));
        bbAnim.put("snapping", 100);
        bbAnim.put("selected", false);
        bbAnim.put("anim_time_update", "");
        bbAnim.put("blend_weight", "");
        bbAnim.put("start_delay", "");
        bbAnim.put("loop_delay", "");

        Map<String, Object> animators = new LinkedHashMap<>();
        Map<String, Object> bones = (Map<String, Object>) ajAnim.get("bones");

        for (Map.Entry<String, Object> boneEntry : bones.entrySet()) {
            String boneName = boneEntry.getKey();
            Map<String, Object> boneData = (Map<String, Object>) boneEntry.getValue();

            Map<String, Object> animator = new LinkedHashMap<>();
            animator.put("name", boneName);
            animator.put("type", "bone");

            List<Map<String, Object>> keyframes = new ArrayList<>();

            for (Map.Entry<String, Object> channelEntry : boneData.entrySet()) {
                String channel = channelEntry.getKey();
                Map<String, Map<String, Object>> keyframesByTime = (Map<String, Map<String, Object>>) channelEntry.getValue();

                for (Map.Entry<String, Map<String, Object>> kfEntry : keyframesByTime.entrySet()) {
                    try {
                        float time = Float.parseFloat(kfEntry.getKey());
                        Map<String, Object> kfData = kfEntry.getValue();

                        List<Map<String, Object>> dataPoints = Collections.singletonList(toVectorDict((List<?>) kfData.get("post")));

                        Map<String, Object> keyframe = new LinkedHashMap<>();
                        keyframe.put("channel", channel);
                        keyframe.put("data_points", dataPoints);
                        keyframe.put("uuid", UUID.randomUUID().toString());
                        keyframe.put("time", time);
                        keyframe.put("color", -1);
                        keyframe.put("interpolation", kfData.getOrDefault("lerp_mode", "linear"));
                        keyframe.put("easing", "linear");
                        keyframe.put("easingArgs", new ArrayList<>());

                        keyframes.add(keyframe);
                    } catch (Exception e) {
                        Logger.warn("Error parsing keyframe: " + boneName + ", " + channel + ", " + kfEntry.getKey() + " → " + e.getMessage());
                    }
                }
            }

            animator.put("keyframes", keyframes);
            animators.put(UUID.randomUUID().toString(), animator);
        }

        bbAnim.put("animators", animators);
        return bbAnim;
    }

    private static Map<String, Object> toVectorDict(List<?> arr) {
        Map<String, Object> vector = new LinkedHashMap<>();
        vector.put("x", arr.get(0));
        vector.put("y", arr.get(1));
        vector.put("z", arr.get(2));
        return vector;
    }
}