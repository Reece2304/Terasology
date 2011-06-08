/*
 * Copyright 2011 Benjamin Glatzel <benjamin.glatzel@me.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.begla.blockmania.world;

import com.github.begla.blockmania.Helper;
import java.util.Collections;
import java.util.TreeMap;
import java.util.logging.Level;
import javolution.util.FastList;

/**
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 */
public final class ChunkCache {

    private final TreeMap<Integer, Chunk> _chunkCache = new TreeMap<Integer, Chunk>();
    private World _parent;

    /**
     * 
     * @param _parent
     */
    public ChunkCache(World _parent) {
        this._parent = _parent;
    }

    /**
     * Loads a specified chunk from cache or queues a new chunk for generation.
     *
     * NOTE: This method ALWAYS returns a valid chunk (if positive x and z values are provided) 
     * since a new chunk is generated if none of the present chunks fit the request.
     *
     * @param x X-coordinate of the chunk
     * @param z Z-coordinate of the chunk
     * @return The chunk
     */
    public Chunk loadOrCreateChunk(int x, int z) {
        // Catch negative values
        if (x < 0 || z < 0) {
            return null;
        }

        // Try to load the chunk from the cache
        Chunk c = _chunkCache.get(Helper.getInstance().cantorize(x, z));

        // We got a chunk! Already! Great!
        if (c != null) {
            return c;
        }

        // Delete some elements if the cache size is exceeded
        if (_chunkCache.size() > 1024) {
            // Fetch all chunks within the cache
            FastList<Chunk> sortedChunks = null;
            sortedChunks = new FastList<Chunk>(_chunkCache.values());
            // Sort them according to their distance to the player
            Collections.sort(sortedChunks);

            Helper.LOGGER.log(Level.FINE, "Cache full. Removing some chunks from the chunk cache...");

            // Free some space
            for (int i = 0; i < 32; i++) {
                int indexToDelete = sortedChunks.size() - i;

                if (indexToDelete >= 0 && indexToDelete < sortedChunks.size()) {
                    Chunk cc = sortedChunks.get(indexToDelete);
                    // Save the chunk before removing it from the cache
                    _chunkCache.remove(Helper.getInstance().cantorize((int) cc.getPosition().x, (int) cc.getPosition().z));
                    cc.dispose();
                }
            }

            Helper.LOGGER.log(Level.FINE, "Finished removing chunks from the chunk cache.");
        }

        // Init a new chunk
        c = _parent.prepareNewChunk(x, z);
        _chunkCache.put(Helper.getInstance().cantorize(x, z), c);

        return c;
    }

    /**
     * Returns true if the given chunk is present in the cache.
     * 
     * @param c The chunk
     * @return True if the chunk is present in the chunk cache
     */
    public boolean isChunkCached(Chunk c) {
        return loadChunk((int) c.getPosition().x, (int) c.getPosition().z) != null;
    }

    /**
     * Tries to load a chunk from the cache. Returns null if no
     * chunk is found.
     * 
     * @param x X-coordinate
     * @param z Z-coordinate
     * @return The loaded chunk
     */
    public Chunk loadChunk(int x, int z) {
        Chunk c = _chunkCache.get(Helper.getInstance().cantorize(x, z));
        return c;
    }

    /**
     * 
     * @param key
     * @return
     */
    public Chunk getChunkByKey(int key) {
        return _chunkCache.get(key);
    }

    /**
     * Writes all chunks to disk.
     */
    public void writeAllChunksToDisk() {
        _parent.suspendUpdateThread();
        for (Chunk c : _chunkCache.values()) {
            c.writeChunkToDisk();
        }
        _parent.resumeUpdateThread();
    }

    /**
     * 
     * @return
     */
    public int size() {
        return _chunkCache.size();
    }
}
