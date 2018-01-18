package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import org.kynosarges.tektosyne.subdivision.*;

/**
 * Provides unit tests for class {@link Voronoi} and related classes.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class VoronoiTest {

    @Test
    public void testVoronoi() {
        final int count = 10 + (int) (Math.random() * 91);
        final PointD[] points = new PointD[count];
        for (int i = 0; i < points.length; i++)
            points[i] = GeoUtils.randomPoint(-1000, -1000, 2000, 2000);

        testVoronoiResults(Voronoi.findAll(points, new RectD(-1000, -1000, 2000, 2000)));
    }

    @Test
    public void testVoronoiWithRegionTouchingThreeCorners() {
        RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points1 = new PointD[]{
                new PointD(-1, -1), // key region
                new PointD(-9, -7),
                new PointD(-8, -9),
        };

        final PointD[] points2 = new PointD[]{
                new PointD(1, -1), // key region
                new PointD(9, -7),
                new PointD(8, -9),
        };

        final PointD[] points3 = new PointD[]{
                new PointD(-1, 1), // key region
                new PointD(-9, 7),
                new PointD(-8, 9),
        };

        final PointD[] points4 = new PointD[]{
                new PointD(1, 1), // key region
                new PointD(9, 7),
                new PointD(8, 9),
        };

        testVoronoiResults(Voronoi.findAll(points1, clip));
        testVoronoiResults(Voronoi.findAll(points2, clip));
        testVoronoiResults(Voronoi.findAll(points3, clip));
        testVoronoiResults(Voronoi.findAll(points4, clip));
    }

    @Test
    public void testVoronoiWithRegionTouchingTwoOppositeSides() {
        RectD clip = new RectD(-10, -10, 10, 10);

        final PointD[] points = new PointD[]{
                new PointD(-5, 0),
                new PointD(0, 0), // key region
                new PointD(5, 0),
        };

        testVoronoiResults(Voronoi.findAll(points, clip));
    }

    private void testVoronoiResults(VoronoiResults results) {
        final Subdivision delaunay = results.toDelaunaySubdivision(true);
        delaunay.validate();

        // compare original and subdivision’s Delaunay edges
        final LineD[] delaunayEdges = delaunay.toLines();
        assertEquals(results.delaunayEdges().length, delaunayEdges.length);

        for (LineD edge: results.delaunayEdges())
            if (PointDComparatorY.compareExact(edge.start, edge.end) > 0)
                assertTrue(Arrays.asList(delaunayEdges).contains(edge.reverse()));
            else
                assertTrue(Arrays.asList(delaunayEdges).contains(edge));

        final VoronoiMap voronoi = new VoronoiMap(results);
        voronoi.source().validate();

        // compare original and subdivision’s Voronoi regions
        final NavigableMap<Integer, SubdivisionFace> voronoiFaces = voronoi.source().faces();
        assertEquals(results.voronoiRegions().length, voronoiFaces.size() - 1);

        for (SubdivisionFace face: voronoiFaces.values()) {
            if (face.outerEdge() == null) continue;
            final int index = voronoi.fromFace(face);

            final PointD[] polygon = results.voronoiRegions()[index];
            assertArrayEquivalent(polygon, face.outerEdge().cyclePolygon());

            final PointD site = results.generatorSites[index];
            assertNotEquals(PolygonLocation.OUTSIDE, face.outerEdge().locate(site));
        }
    }

    private static <T> void assertArrayEquivalent(T[] a, T[] b) {
        assertCollectionEquivalent(Arrays.asList(a), Arrays.asList(b));
    }

    private static <T> void assertCollectionEquivalent(Collection<T> a, Collection<T> b) {
        assertEquals(a.size(), b.size());
        assertTrue(a.containsAll(b));
        assertTrue(b.containsAll(a));
    }
}
