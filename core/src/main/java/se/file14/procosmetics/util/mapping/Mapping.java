package se.file14.procosmetics.util.mapping;

import se.file14.procosmetics.util.version.BukkitVersion;
import se.file14.procosmetics.util.version.VersionUtil;

public class Mapping {

    public static MappingType MAPPING_TYPE = MappingType.SPIGOT;

    static {
        boolean useMojangMappings = false;

        try {
            // Check if the server is running Paper. Starting from 1.20.6 Paper uses Mojang mappings.
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            if (VersionUtil.isHigherThanOrEqualTo(BukkitVersion.v1_20)) {
                useMojangMappings = true;
            }
        } catch (ClassNotFoundException ignored) {
            // Folia does not include this class, fall back to an additional check below.
        }

        if (!useMojangMappings) {
            try {
                // Folia and other Mojang-mapped forks expose unversioned CraftBukkit classes.
                Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
                if (VersionUtil.isHigherThanOrEqualTo(BukkitVersion.v1_20)) {
                    useMojangMappings = true;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (useMojangMappings) {
            MAPPING_TYPE = MappingType.MOJANG;
        }
    }
}
