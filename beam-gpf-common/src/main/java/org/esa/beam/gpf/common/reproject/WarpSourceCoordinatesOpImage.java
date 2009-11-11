/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.gpf.common.reproject;

import com.bc.ceres.core.Assert;

import org.esa.beam.jai.ResolutionLevel;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.Warp;

/**
 * todo - add API doc
 * 
 * @author Marco Zuehlke
 * @since BEAM 4.7
 */
class WarpSourceCoordinatesOpImage extends SourcelessOpImage {

    private final Warp warp;
    private final RasterFormatTag rasterFormatTag;
    
    private static ImageLayout createTwoBandedImageLayout(int width, int height, Dimension tileSize) {
        if (width < 0) {
            throw new IllegalArgumentException("width");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height");
        }
        if (tileSize == null) {
            throw new IllegalArgumentException("tileSize");
        }
        SampleModel sampleModel = RasterFactory.createPixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 2);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        if (colorModel == null) {
            final int dataType = sampleModel.getDataType();
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {DataBuffer.getDataTypeSize(dataType)};
            colorModel = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE, dataType);
        }
        return new ImageLayout(0, 0, width, height, 0, 0, tileSize.width, tileSize.height, sampleModel, colorModel);
    }
   

    /**
     * @param warp
     * @param width
     * @param height
     * @param tileSize
     * @param configuration
     */
    WarpSourceCoordinatesOpImage(Warp warp, int width, int height, Dimension tileSize,
                                        Map configuration) {
        this(warp, createTwoBandedImageLayout(width, height, tileSize), configuration);
    }

    private WarpSourceCoordinatesOpImage(Warp warp, ImageLayout layout, Map configuration) {
        super(layout, configuration, layout.getSampleModel(null), layout.getMinX(null), layout.getMinY(null),
              layout.getWidth(null), layout.getHeight(null));
        this.warp = warp;
        int compatibleTag = RasterAccessor.findCompatibleTag(null, layout.getSampleModel(null));
        rasterFormatTag = new RasterFormatTag(layout.getSampleModel(null), compatibleTag);
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    @Override
    protected void computeRect(PlanarImage sources[], WritableRaster dest, Rectangle destRect) {
//        System.out.println("WarpSourceCoordinatesOpImage "+destRect);
//        long t1 = System.currentTimeMillis();
        RasterAccessor dst = new RasterAccessor(dest, destRect, rasterFormatTag, getColorModel());
        computeRectFloat(dst);
        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }
//        long t2 = System.currentTimeMillis();
//        System.out.println("WarpSourceCoordinatesOpImage "+(t2-t1));
    }

    private void computeRectFloat(RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int bandOffsets[] = dst.getBandOffsets();
        float data[][] = dst.getFloatDataArrays();
        float warpData[] = new float[2 * dstWidth * dstHeight];
        int lineOffset = 0;

        warp.warpRect(dst.getX(), dst.getY(), dstWidth, dstHeight, warpData);
        int count = 0;
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;
            for (int w = 0; w < dstWidth; w++) {
                data[0][pixelOffset + bandOffsets[0]] = warpData[count++];
                data[1][pixelOffset + bandOffsets[1]] = warpData[count++];
                pixelOffset += pixelStride;
            }
        }
    }
}
