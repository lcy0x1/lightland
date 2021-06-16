package com.hikarishima.lightland.mobspawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lcy0x1.core.util.ExceptionHandler;
import com.lcy0x1.core.util.SerialClass;
import com.lcy0x1.core.util.Serializer;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@SerialClass
public class MobSpawn {

    public static final List<MobSpawn> LIST = new ArrayList<>();

    public static void init() {
        LIST.clear();
        String path = FMLPaths.CONFIGDIR.get().toString();
        File file = new File(path + File.separator + "lightland" + File.separator + "spawn_rules.json");
        if (file.exists()) {
            JsonElement elem = ExceptionHandler.get(() -> new JsonParser().parse(new FileReader(file)));
            if (elem != null && elem.isJsonArray()) {
                for (JsonElement e : elem.getAsJsonArray()) {
                    LIST.add(Serializer.from(e.getAsJsonObject(), MobSpawn.class, new MobSpawn()));
                }
            }
        } else {
            LogManager.getLogger().warn(file.toString() + " does not exist");
        }

    }

    public static void spawn(List<MobSpawnInfo.Spawners> list, BlockPos pos) {
        MobSpawn winner = null;
        double max_density = 0;
        for (MobSpawn spawn : MobSpawn.LIST) {
            double density = spawn.getDensity(pos.getX(), pos.getY());
            if (density > max_density) {
                winner = spawn;
                max_density = density;
            }
        }
        if (winner != null) {
            double difficulty = winner.getDifficulty(pos.getX(), pos.getY());
            if (difficulty > 0)
                for (MobSpawn.Entry entry : winner.getMobs())
                    if (entry.type != null) {
                        list.add(new MobSpawnInfo.Spawners(entry.type, entry.weight, entry.min, entry.max));
                        //TODO add difficulty
                    }
        }
    }

    @SerialClass
    public static class Entry {

        @SerialClass.SerialField
        public String id;

        @SerialClass.SerialField
        public int weight, min, max;

        public EntityType<?> type;

        @SerialClass.OnInject
        public void onInject() {
            type = EntityType.byString(id).get();
            if (type == null)
                LogManager.getLogger().warn("entity type [" + id + "] not present");
        }

    }

    @SerialClass.SerialField
    public String id, name;

    @SerialClass.SerialField
    public int origin_x, origin_y;

    @SerialClass.SerialField
    public double center_density, density_scale, safe_threshold, difficulty_cap, base_difficulty, difficulty_scale;

    @SerialClass.SerialField
    public Entry[] mobs;

    public MobSpawn() {
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getDensity(int x, int y) {
        int dx = origin_x - x;
        int dy = origin_y - y;
        return center_density - density_scale * Math.sqrt(dx * dx + dy * dy);
    }

    public double getDifficulty(int x, int y) {
        int dx = origin_x - x;
        int dy = origin_y - y;
        double dis = Math.sqrt(dx * dx + dy * dy);
        if (dis < safe_threshold)
            return 0;
        double diff = base_difficulty + dis * difficulty_scale;
        if (diff > difficulty_cap)
            return difficulty_cap;
        return diff;
    }

    public Entry[] getMobs() {
        return mobs;
    }
}
