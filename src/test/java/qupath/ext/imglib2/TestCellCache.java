package qupath.ext.imglib2;

import net.imglib2.img.basictypeaccess.DataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.cell.Cell;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.regions.ImageRegion;

public class TestCellCache {

    @Test
    void Check_Negative_Max_Size() {
        int cacheMaxSizeMiB = -10;

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CellCache(cacheMaxSizeMiB));
    }

    @Test
    void Check_Cell_Created() {
        byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        Cell<ByteArray> expectedSampleCell = new SampleCell(data);
        TileRequest tileRequest = TileRequest.createInstance(
                "path",
                0,
                1,
                ImageRegion.createInstance(0, 0, 5, 5, 2, 3)
        );
        CellCache cellCache = new CellCache(1024);

        Cell<? extends DataAccess> cell = cellCache.getCell(tileRequest, t -> expectedSampleCell);

        Assertions.assertEquals(expectedSampleCell, cell);
    }

    @Test
    void Check_Cell_Cached() {
        byte[] data = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        Cell<ByteArray> expectedSampleCell = new SampleCell(data);
        Cell<ByteArray> notExpectedSampleCell = new SampleCell(new byte[0]);
        TileRequest tileRequest = TileRequest.createInstance(
                "path",
                0,
                1,
                ImageRegion.createInstance(0, 0, 5, 5, 2, 3)
        );
        CellCache cellCache = new CellCache(1024);
        cellCache.getCell(tileRequest, t -> expectedSampleCell);

        Cell<? extends DataAccess> cell = cellCache.getCell(tileRequest, t -> notExpectedSampleCell);

        Assertions.assertEquals(expectedSampleCell, cell);
    }

    private static class SampleCell extends Cell<ByteArray> {

        public SampleCell(byte[] data) {
            super(new int[] {5, 5, 5}, new long[] {0, 1, 2}, new ByteArray(data));
        }
    }
}
