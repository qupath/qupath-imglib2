package qupath.ext.imglib2.immutablearrays;

import net.imglib2.img.basictypeaccess.array.DoubleArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestImmutableDoubleArray {

    @Test
    void Check_Cannot_Set_Value() {
        double[] data = new double[] {0, 1, 2};
        DoubleArray array = new ImmutableDoubleArray(data);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> array.setValue(0, 0));
    }

    @Test
    void Check_Storage_Array_Not_Modified() {
        double[] expectedData = new double[] {0, 1, 2};
        double[] data = expectedData.clone();
        DoubleArray array = new ImmutableDoubleArray(data);

        double[] storageArray = array.getCurrentStorageArray();
        storageArray[1] = 0;

        Assertions.assertArrayEquals(expectedData, data);
    }
}
