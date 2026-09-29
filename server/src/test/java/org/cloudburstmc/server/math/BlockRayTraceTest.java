package org.cloudburstmc.server.math;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockRayTraceTest {

    @Test
    void visitsCellsInOrderAcrossNegativeCoordinates() {
        List<Vector3i> cells = new ArrayList<>();
        BlockRayTrace.of(Vector3f.from(-0.5f, 0.5f, 0.5f), Vector3f.from(-2.5f, 0.5f, 0.5f))
                .forEach(cells::add);

        assertEquals(List.of(Vector3i.from(-1, 0, 0), Vector3i.from(-2, 0, 0),
                Vector3i.from(-3, 0, 0)), cells);
    }

    @Test
    void visitsAdjacentCellAtStartingBoundary() {
        List<Vector3i> cells = new ArrayList<>();
        BlockRayTrace.of(Vector3f.from(1, 0.5f, 0.5f), Vector3f.from(0.5f, 0.5f, 0.5f))
                .forEach(cells::add);

        assertEquals(List.of(Vector3i.from(1, 0, 0), Vector3i.from(0, 0, 0)), cells);
    }

    @Test
    void zeroLengthRayVisitsOnlyItsStartingCell() {
        var iterator = BlockRayTrace.of(Vector3f.from(1.5f, 2.5f, 3.5f),
                Vector3f.from(1.5f, 2.5f, 3.5f)).iterator();

        assertEquals(Vector3i.from(1, 2, 3), iterator.next());
        assertThrows(NoSuchElementException.class, iterator::next);
    }
}
