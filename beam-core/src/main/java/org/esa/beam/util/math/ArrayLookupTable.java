/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util.math;

/**
 * The class {@code ArrayLookupTable} performs the function of
 * multilinear  interpolation for array lookup  tables with an
 * arbitrary number of dimensions.
 * <p/>
 * todo - thread safety
 * todo - method for contracting a table (see MAPP-AER processor)
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class ArrayLookupTable {

    /**
     * The lookup values.
     */
    private final double[] values;
    /**
     * The dimensions associated with the lookup table.
     */
    private final IntervalPartition[] dimensions;
    /**
     * The strides defining the layout of the lookup value array.
     */
    private final int[] strides;
    /**
     * The relative array offsets of the lookup values for the vertices of a coordinate grid cell.
     */
    private final int[] o;
    /**
     * The lookup value arrays for the vertices bracketing the lookup point.
     */
    private final double[][] v;
    /**
     * The normalized representation of the lookup point's coordinates.
     */
    private final FracIndex[] fracIndexes;

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the looked-up value array.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public ArrayLookupTable(int length, final double[] values, final IntervalPartition... dimensions) throws
            IllegalArgumentException,
            NullPointerException {
        if (length < 1) {
            throw new IllegalArgumentException("length < 1");
        }

        LookupTable.ensureLegalArray(dimensions);
        LookupTable.ensureLegalArray(values, length * LookupTable.getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;

        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = length; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        LookupTable.computeVertexOffsets(strides, o);

        v = new double[1 << n][length];
        fracIndexes = FracIndex.createArray(n);
    }

    /**
     * Constructs an array lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param length     the length of the looked-up value array.
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     * @throws IllegalArgumentException if {@code length} is less than {@code 1} or the length of
     *                                  {@code values} is not equal to {@code length} times the number
     *                                  of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public ArrayLookupTable(int length, final double[] values, final double[]... dimensions) throws
            IllegalArgumentException,
            NullPointerException {
        this(length, values, IntervalPartition.createArray(dimensions));
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    public final IntervalPartition[] getDimensions() {
        return dimensions;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(final int i) {
        return dimensions[i];
    }

    /**
     * Returns an interpolated value array for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @return the interpolated value array.
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double[] getValues(final double... coordinates) throws IllegalArgumentException {
        LookupTable.ensureLegalArray(coordinates, dimensions.length);

        for (int i = 0; i < dimensions.length; ++i) {
            LookupTable.computeFracIndex(dimensions[i], coordinates[i], fracIndexes[i]);
        }

        return getValues(fracIndexes);
    }

    double[] getValues(final FracIndex... fracIndexes) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        for (int i = 0; i < v.length; ++i) {
            System.arraycopy(values, origin + o[i], v[i], 0, v[i].length);
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                for (int k = 0; k < v[j].length; ++k) {
                    v[j][k] += f * (v[m + j][k] - v[j][k]);
                }
            }
        }

        return v[0];
    }
}
