package com.magmaguy.freeminecraftmodels.customentity;

import com.magmaguy.easyminecraftgoals.NMSManager;
import com.magmaguy.freeminecraftmodels.MetadataHandler;
import com.magmaguy.freeminecraftmodels.customentity.core.LegacyHitDetection;
import com.magmaguy.freeminecraftmodels.customentity.core.ModeledEntityInterface;
import com.magmaguy.freeminecraftmodels.customentity.core.RegisterModelEntity;
import com.magmaguy.freeminecraftmodels.dataconverter.FileModelConverter;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.Display;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DynamicEntity extends ModeledEntity implements ModeledEntityInterface {
    @Getter
    private static final List<DynamicEntity> dynamicEntities = new ArrayList<>();
    @Getter @Setter
    private Display.ItemDisplay minecraftHeadEntity;
    @Getter
    private final String name = "default";
    private BukkitTask skeletonSync = null;

    private static NamespacedKey namespacedKey = new NamespacedKey(MetadataHandler.PLUGIN, "DynamicEntity");

    public static boolean isDynamicEntity(LivingEntity livingEntity) {
        if (livingEntity == null) return false;
        return livingEntity.getPersistentDataContainer().has(namespacedKey, PersistentDataType.BYTE);
    }

    public static DynamicEntity getDynamicEntity(LivingEntity livingEntity) {
        for (DynamicEntity dynamicEntity : dynamicEntities)
            if (dynamicEntity.getLivingEntity().equals(livingEntity))
                return dynamicEntity;
        return null;
    }

    //Coming soon
    public DynamicEntity(String entityID, Location targetLocation) {
        super(entityID, targetLocation);
        dynamicEntities.add(this);
    }

    public static void shutdown() {
        dynamicEntities.forEach(DynamicEntity::remove);
        dynamicEntities.clear();
    }

    //safer since it can return null
    @Nullable
    public static DynamicEntity create(String entityID, LivingEntity livingEntity) {
        FileModelConverter fileModelConverter = FileModelConverter.getConvertedFileModels().get(entityID);
        if (fileModelConverter == null) return null;
        DynamicEntity dynamicEntity = new DynamicEntity(entityID, livingEntity.getLocation());
        dynamicEntity.spawn(livingEntity);
        livingEntity.setVisibleByDefault(false);
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getLocation().getWorld().equals(dynamicEntity.getLocation().getWorld())) {
                player.hideEntity(MetadataHandler.PLUGIN, livingEntity);
            }
        });
        livingEntity.getPersistentDataContainer().set(namespacedKey, PersistentDataType.BYTE, (byte) 0);

        //find and store minecraft head entity
        dynamicEntity.getSkeleton().getBones().forEach(bone -> {
            if (bone.getBoneBlueprint().isHead()){

                com.magmaguy.easyminecraftgoals.v1_20_R3.packets.PacketDisplayEntity test =
                        (com.magmaguy.easyminecraftgoals.v1_20_R3.packets.PacketDisplayEntity) bone.getBoneTransforms().getPacketDisplayEntity();
                try {
                    Field field = test.getClass().getDeclaredField("itemDisplay");
                    field.setAccessible(true);
                    Object value = field.get(test);
                    Display.ItemDisplay mcEntity = ( Display.ItemDisplay) value;
                    dynamicEntity.setMinecraftHeadEntity(mcEntity);

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return dynamicEntity;
    }

    public void spawn(LivingEntity entity) {
        super.livingEntity = entity;
        RegisterModelEntity.registerModelEntity(entity, getSkeletonBlueprint().getModelName());
        super.spawn();
        syncSkeletonWithEntity();
        setHitbox();
    }

    private void syncSkeletonWithEntity() {
        skeletonSync = new BukkitRunnable() {
            @Override
            public void run() {
                if (livingEntity == null || !livingEntity.isValid()) {
                    remove();
                    cancel();
                    return;
                }
                Location entityLocation = livingEntity.getLocation();
                entityLocation.setYaw(NMSManager.getAdapter().getBodyRotation(livingEntity));
                getSkeleton().setCurrentLocation(entityLocation);
                getSkeleton().setCurrentHeadPitch(livingEntity.getEyeLocation().getPitch());
                getSkeleton().setCurrentHeadYaw(livingEntity.getEyeLocation().getYaw());
            }
        }.runTaskTimer(MetadataHandler.PLUGIN, 0, 1);
    }

    @Override
    public void remove() {
        super.remove();
        if (livingEntity != null)
            livingEntity.remove();
        if (skeletonSync != null) skeletonSync.cancel();
    }

    private void setHitbox() {
        if (getSkeletonBlueprint().getHitbox() == null) return;
        NMSManager.getAdapter().setCustomHitbox(super.livingEntity, (float) getSkeletonBlueprint().getHitbox().getWidth(), (float) getSkeletonBlueprint().getHitbox().getHeight(), true);
    }

    public void setHeadTexture(int customModelData){
        ItemStack itemStack = new ItemStack(Material.LEATHER_HORSE_ARMOR);
        LeatherArmorMeta itemMeta = (LeatherArmorMeta)itemStack.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        itemMeta.setColor(Color.WHITE);
        itemStack.setItemMeta(itemMeta);
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        minecraftHeadEntity.a(nmsItemStack);
    }

    public void test(){
        Bukkit.getScheduler().runTaskLater(MetadataHandler.PLUGIN,() -> {
            setHeadTexture(50019);
        },20*5);
    }

    @Override
    public BoundingBox getHitbox() {
        if (livingEntity == null) return null;
        return livingEntity.getBoundingBox();
    }

    @Override
    public void damage(Player player, double damage) {
        if (livingEntity == null) return;
        LegacyHitDetection.setEntityDamageBypass(true);
        livingEntity.damage(damage, player);
        getSkeleton().tint();
    }

    @Override
    public World getWorld() {
        if (livingEntity == null || !livingEntity.isValid()) return null;
        return livingEntity.getWorld();
    }
}
