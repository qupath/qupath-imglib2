package qupath.ext.imglib2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import qupath.ext.imglib2.immutablearrays.ImmutableByteArray;
import qupath.ext.imglib2.immutablearrays.ImmutableDoubleArray;
import qupath.ext.imglib2.immutablearrays.ImmutableFloatArray;
import qupath.ext.imglib2.immutablearrays.ImmutableIntArray;
import qupath.ext.imglib2.immutablearrays.ImmutableShortArray;
import qupath.lib.images.servers.TileRequest;

import java.util.function.Function;

/**
 * A cache for {@link Cell} associated with {@link TileRequest}.
 * <p>
 * This class is thread-safe.
 */
public class CellCache {

    private final Cache<TileRequest, Cell<? extends ArrayDataAccess<?>>> cache;

    /**
     * Create a cache with the specified maximum size.
     *
     * @param cacheMaxSizeMiB the maximal size of the cache in mebibyte (MiB). Must be greater than or equal to zero
     * @throws IllegalArgumentException if the maximal size is negative
     */
    public CellCache(int cacheMaxSizeMiB) {
        this.cache = Caffeine.newBuilder()
                .weigher((TileRequest tile, Cell<? extends ArrayDataAccess<?>> cell) -> {
                    int valueSizeBytes;
                    if (cell.getData() instanceof ImmutableByteArray) {
                        valueSizeBytes = 1;
                    } else if (cell.getData() instanceof ImmutableShortArray) {
                        valueSizeBytes = 2;
                    } else if (cell.getData() instanceof ImmutableIntArray) {
                        valueSizeBytes = 4;
                    } else if (cell.getData() instanceof ImmutableFloatArray) {
                        valueSizeBytes = 4;
                    } else if (cell.getData() instanceof ImmutableDoubleArray) {
                        valueSizeBytes = 8;
                    } else {
                        valueSizeBytes = 1;
                    }

                    return (int) (cell.getData().getArrayLength() * valueSizeBytes / (1024f * 1024f));
                })
                .maximumWeight(cacheMaxSizeMiB)
                .softValues()
                .build();
    }

    /**
     * Get a cached cell corresponding to the provided tile request, or compute it if the cache doesn't contain such cell.
     *
     * @param tileRequest the tile request corresponding to the cell to retrieve
     * @param cellGetter a function to compute a cell from a tile request. It will be called if this cache doesn't contain
     *                   a cell for the provided tile request
     * @return a cell corresponding to the provided tile request
     */
    public Cell<? extends ArrayDataAccess<?>> getCell(TileRequest tileRequest, Function<TileRequest, Cell<? extends ArrayDataAccess<?>>> cellGetter) {
        return cache.get(tileRequest, cellGetter);
    }
}
