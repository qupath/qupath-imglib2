package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.IntArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestImmutableIntArray {

    @Test
    void Check_Cannot_Set_Value() {
        int[] data = new int[] {0, 1, 2};
        IntArray array = new ImmutableIntArray(data);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> array.setValue(0, 0));
    }

    @Test
    void Check_Storage_Array_Not_Modified() {
        int[] expectedData = new int[] {0, 1, 2};
        int[] data = expectedData.clone();
        IntArray array = new ImmutableIntArray(data);

        int[] storageArray = array.getCurrentStorageArray();
        storageArray[1] = 0;

        Assertions.assertArrayEquals(expectedData, data);
    }
}
