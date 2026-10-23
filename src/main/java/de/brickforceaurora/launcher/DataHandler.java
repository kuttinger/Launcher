package de.brickforceaurora.launcher;

import de.brickforceaurora.launcher.data.StorageHandler;
import me.lauriichan.snowframe.util.Version;
import me.lauriichan.snowframe.util.nbt.CompoundTag;
import me.lauriichan.snowframe.util.nbt.StringTag;
import me.lauriichan.snowframe.util.nbt.TagType;

public final class DataHandler {

    private DataHandler() {
        throw new UnsupportedOperationException();
    }

    public static final StorageHandler<Version> VERSION = new StorageHandler<>(TagType.INT_ARRAY) {
        @Override
        public Version read(final CompoundTag root, final String key) {
            final int[] components = root.getIntArray(key);
            return new Version(components[0], components[1], components[2], components[3]);
        }

        @Override
        public void write(final CompoundTag root, final String key, final Version version) {
            final int[] components = new int[4];
            components[0] = version.major;
            components[1] = version.minor;
            components[2] = version.patch;
            components[3] = version.revision;
            root.put(key, components);
        }
    };

    public static final StorageHandler<String> STRING = new StorageHandler<>(TagType.STRING) {
        @Override
        public String read(final CompoundTag root, final String key) {
            return root.getString(key);
        }

        @Override
        public void write(final CompoundTag root, final String key, final String value) {
            root.put(key, new StringTag(value == null ? "" : value));
        }
    };

    public static final StorageHandler<Integer> INT = new StorageHandler<>(TagType.INT) {
        @Override
        public Integer read(final CompoundTag root, final String key) {
            return root.getInt(key);
        }

        @Override
        public void write(final CompoundTag root, final String key, final Integer value) {
            root.put(key, value == null ? 0 : value.intValue());
        }
    };

}
