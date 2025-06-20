package com.magmaguy.freeminecraftmodels.dataconverter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.magmaguy.freeminecraftmodels.MetadataHandler;
import com.magmaguy.freeminecraftmodels.utils.StringToResourcePackFilename;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FileModelConverter {

    @Getter
    private static final HashMap<String, FileModelConverter> convertedFileModels = new HashMap<>();
    @Getter
    private static final HashMap<String, Integer> imageSize = new HashMap<>();
    private final HashMap<String, Object> values = new HashMap<>();
    private final HashMap<String, Object> outliner = new HashMap<>();
    //Store the texture with the identifier and the name of the texture file
    private final HashMap<Integer, String> textures = new HashMap<>();

    private static final String CATEGORY_DEFAULT = "default";
    public static final File ANIMATION_DIRECTORY = new File(MetadataHandler.PLUGIN.getDataFolder(), "animations");
    private static final Gson gson = new Gson();



    private String modelName;
    @Getter
    private SkeletonBlueprint skeletonBlueprint;
    @Getter
    private AnimationsBlueprint animationsBlueprint = null;
    @Getter
    private String ID;



    /**
     * In this instance, the file is the raw bbmodel file which is actually in a JSON format
     *
     * @param file bbmodel file to parse
     */
    public FileModelConverter(File file) {

        if (file.getName().contains(".bbmodel")) modelName = file.getName().replace(".bbmodel", "");
        else if (file.getName().contains(".fmmodel")) modelName = file.getName().replace(".fmmodel", "");
        else {
            Bukkit.getLogger().warning("File " + file.getName() + " should not be in the models folder!");
            return;
        }

        modelName = StringToResourcePackFilename.convert(modelName);

        Gson gson = new Gson();

        Reader reader;
        // create a reader
        try {
            reader = Files.newBufferedReader(Paths.get(file.getPath()));
        } catch (Exception ex) {
            Logger.warn("Failed to read file " + file.getAbsolutePath());
            return;
        }

        // convert JSON file to map
        Map<?, ?> map = gson.fromJson(reader, Map.class);

        /* Just for debugging, this is very spammy
        // print map entries
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }
         */

        // close reader
        try {
            reader.close();
        } catch (Exception exception) {
            Logger.warn("Failed to close reader for file!");
            return;
        }

        double projectResolution = (double) ((Map<?, ?>) map.get("resolution")).get("height");

        //This parses the textures, extracts them to the correct directory and stores their values for the bone texture references
        List<Map<?, ?>> texturesValues = (ArrayList<Map<?, ?>>) map.get("textures");
        for (int i = 0; i < texturesValues.size(); i++) {
            Map<?, ?> element = texturesValues.get(i);
            String imageName = StringToResourcePackFilename.convert((String) element.get("name"));
            if (!imageName.contains(".png")) {
                if (!imageName.contains(".")) imageName += ".png";
                else imageName.split("\\.")[0] += ".png";
            }
            String base64Image = (String) element.get("source");
            //So while there is an ID in blockbench it is not what it uses internally, what it uses internally is the ordered list of textures. Don't ask why.
            Integer id = i;
            textures.put(id, imageName.replace(".png", ""));
            base64Image = base64Image.split(",")[base64Image.split(",").length - 1];
            //VSD COMMENTAIRE DE CE IF QUI SERT A RIEN
            //if (!imageSize.containsKey(imageName))
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64Image));
                File imageFile = new File(MetadataHandler.PLUGIN.getDataFolder().getAbsolutePath() + File.separatorChar + "output" + File.separatorChar + "FreeMinecraftModels" + File.separatorChar + "assets" + File.separatorChar + "freeminecraftmodels" + File.separatorChar + "textures" + File.separatorChar + "entity" + File.separatorChar + modelName + File.separatorChar + imageName);
                FileUtils.writeByteArrayToFile(imageFile, inputStream.readAllBytes());
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                //VSD TODO store heads texture for each model
                imageSize.put(imageName, bufferedImage.getWidth());
            } catch (Exception ex) {
                Logger.warn("Failed to convert image " + imageName + " to its corresponding image file!");
            }
        }

        //This parses the blocks
        List<Map> elementValues = (ArrayList<Map>) map.get("elements");
        for (Map element : elementValues) {
            values.put((String) element.get("uuid"), element);
        }

        //This creates the bones and skeleton
        List outlinerValues = (ArrayList) map.get("outliner");
        for (int i = 0; i < outlinerValues.size(); i++) {
            if (!(outlinerValues.get(i) instanceof Map)) {
                //Bukkit.getLogger().warning("WTF format for model name " + modelName + ": " + outlinerValues.get(i));
                //I don't really know why Blockbench does this
                continue;
            } else {
                Map<?, ?> element = (Map<?, ?>) outlinerValues.get(i);
                outliner.put((String) element.get("uuid"), element);
            }
        }

        ID = modelName;
        skeletonBlueprint = new SkeletonBlueprint(projectResolution, outlinerValues, values, generateFileTextures(), modelName, null);//todo: pass path

//        List animationList = (ArrayList) map.get("animations");
//        if (animationList != null)
//            animationsBlueprint = new AnimationsBlueprint(animationList, modelName, skeletonBlueprint);

        loadAnimationsFromCategory();

//        List<Map<String, Object>> animationDataList = AnimationLoader.getAnimationsForModel(this);
//        animationsBlueprint = new AnimationsBlueprint(animationDataList, modelName, skeletonBlueprint);
        convertedFileModels.put(modelName, this);//todo: id needs to be more unique, add folder directory into it
    }


    private void loadAnimationsFromCategory() {

        if (!ANIMATION_DIRECTORY.exists() || !ANIMATION_DIRECTORY.isDirectory()) {
            Logger.warn("Animation folder not found");
            return;
        }

        File[] animationFiles = ANIMATION_DIRECTORY.listFiles((dir, name) -> name.endsWith(".json"));
        if (animationFiles == null || animationFiles.length == 0) return;

        List<Object> animationDataList = new ArrayList<>();
        for (File animFile : animationFiles) {
            try (Reader reader = Files.newBufferedReader(animFile.toPath())) {
                Map<String, Object> animData = gson.fromJson(reader, Map.class);
                Set<String> currentAnimationRig = getRigFromAnimation(animData);
                if (animationRigMatchesModelRig(currentAnimationRig)) {
                    animationDataList.add(animData);
                }
            } catch (Exception ex) {
                Logger.warn("Failed to load animation file: " + animFile.getName());
            }
        }

        if (!animationDataList.isEmpty()) {
            animationsBlueprint = new AnimationsBlueprint(animationDataList, modelName, skeletonBlueprint);
        }
    }

    private boolean animationRigMatchesModelRig(Set<String> animationRig) {
        Set<String> modelRig = skeletonBlueprint.getBoneMap().keySet();
        return modelRig.containsAll(animationRig);
    }

    private static Set<String> getRigFromAnimation( Map<String, Object> animData) {
        Set<String> names = (Set<String>) ((Collection) ((LinkedTreeMap) animData.get("animators")).values())
                .stream()
                .map(animator -> ((LinkedTreeMap) animator).get("name").toString())
                .collect(Collectors.toSet());
        return names;
    }


    public static void shutdown() {
        convertedFileModels.clear();
        imageSize.clear();
    }

    private Map<String, Map<String, Object>> generateFileTextures() {
        Map<String, Map<String, Object>> texturesMap = new HashMap<>();
        Map<String, Object> textureContents = new HashMap<>();
        for (Integer key : textures.keySet())
            textureContents.put("" + key, "freeminecraftmodels:entity/" + modelName + "/" + textures.get(key));
        texturesMap.put("textures", textureContents);
        return texturesMap;
    }
}
