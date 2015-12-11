/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sejda.sambox.pdmodel.font;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.InputStream;

import org.apache.fontbox.FontBoxFont;
import org.apache.fontbox.util.BoundingBox;
import org.sejda.sambox.cos.COSArray;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.font.encoding.DictionaryEncoding;
import org.sejda.sambox.pdmodel.font.encoding.Encoding;
import org.sejda.sambox.pdmodel.font.encoding.GlyphList;
import org.sejda.sambox.util.Matrix;
import org.sejda.sambox.util.Vector;

/**
 * A PostScript Type 3 Font.
 *
 * @author Ben Litchfield
 */
public class PDType3Font extends PDSimpleFont
{
    private PDResources resources;
    private COSDictionary charProcs;
    private Matrix fontMatrix;
    private BoundingBox fontBBox;

    /**
     * Constructor.
     *
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDType3Font(COSDictionary fontDictionary) throws IOException
    {
        super(fontDictionary);
        readEncoding();
    }

    @Override
    public String getName()
    {
        return dict.getNameAsString(COSName.NAME);
    }

    @Override
    protected final void readEncoding() throws IOException
    {
        COSDictionary encodingDict = (COSDictionary) dict.getDictionaryObject(COSName.ENCODING);
        encoding = new DictionaryEncoding(encodingDict);
        glyphList = GlyphList.getAdobeGlyphList();
    }

    @Override
    protected Encoding readEncodingFromFont() throws IOException
    {
        // Type 3 fonts do not have a built-in encoding
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    protected Boolean isFontSymbolic()
    {
        return false;
    }

    @Override
    public GeneralPath getPath(String name) throws IOException
    {
        // Type 3 fonts do not use vector paths
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    public boolean hasGlyph(String name) throws IOException
    {
        COSStream stream = (COSStream) getCharProcs().getDictionaryObject(COSName.getPDFName(name));
        return stream != null;
    }

    @Override
    public FontBoxFont getFontBoxFont()
    {
        // Type 3 fonts do not use FontBox fonts
        throw new UnsupportedOperationException("not supported for Type 3 fonts");
    }

    @Override
    public Vector getDisplacement(int code) throws IOException
    {
        return getFontMatrix().transform(new Vector(getWidth(code), 0));
    }

    @Override
    public float getWidth(int code) throws IOException
    {
        int firstChar = dict.getInt(COSName.FIRST_CHAR, -1);
        int lastChar = dict.getInt(COSName.LAST_CHAR, -1);
        if (getWidths().size() > 0 && code >= firstChar && code <= lastChar)
        {
            return getWidths().get(code - firstChar);
        }
        else
        {
            PDFontDescriptor fd = getFontDescriptor();
            if (fd != null)
            {
                return fd.getMissingWidth();
            }
            else
            {
                return getWidthFromFont(code);
            }
        }
    }

    @Override
    public float getWidthFromFont(int code) throws IOException
    {
        PDType3CharProc charProc = getCharProc(code);
        if (charProc == null)
        {
            return 0;
        }
        return charProc.getWidth();
    }

    @Override
    public boolean isEmbedded()
    {
        return true;
    }

    @Override
    public float getHeight(int code) throws IOException
    {
        PDFontDescriptor desc = getFontDescriptor();
        if (desc != null)
        {
            // the following values are all more or less accurate at least all are average
            // values. Maybe we'll find another way to get those value for every single glyph
            // in the future if needed
            PDRectangle fontBBox = desc.getFontBoundingBox();
            float retval = 0;
            if (fontBBox != null)
            {
                retval = fontBBox.getHeight() / 2;
            }
            if (retval == 0)
            {
                retval = desc.getCapHeight();
            }
            if (retval == 0)
            {
                retval = desc.getAscent();
            }
            if (retval == 0)
            {
                retval = desc.getXHeight();
                if (retval > 0)
                {
                    retval -= desc.getDescent();
                }
            }
            return retval;
        }
        return 0;
    }

    @Override
    protected byte[] encode(int unicode) throws IOException
    {
        throw new UnsupportedOperationException("Not implemented: Type3");
    }

    @Override
    public int readCode(InputStream in) throws IOException
    {
        return in.read();
    }

    @Override
    public Matrix getFontMatrix()
    {
        if (fontMatrix == null)
        {
            COSArray array = (COSArray) dict.getDictionaryObject(COSName.FONT_MATRIX);
            if (array != null)
            {
                fontMatrix = new Matrix(array);
            }
            else
            {
                return super.getFontMatrix();
            }
        }
        return fontMatrix;
    }

    @Override
    public boolean isDamaged()
    {
        // there's no font file to load
        return false;
    }

    /**
     * Returns the optional resources of the type3 stream.
     *
     * @return the resources bound to be used when parsing the type3 stream
     */
    public PDResources getResources()
    {
        if (resources == null)
        {
            COSDictionary resources = (COSDictionary) dict.getDictionaryObject(COSName.RESOURCES);
            if (resources != null)
            {
                this.resources = new PDResources(resources);
            }
        }
        return resources;
    }

    /**
     * This will get the fonts bounding box from its dictionary.
     *
     * @return The fonts bounding box.
     */
    public PDRectangle getFontBBox()
    {
        COSArray rect = (COSArray) dict.getDictionaryObject(COSName.FONT_BBOX);
        PDRectangle retval = null;
        if (rect != null)
        {
            retval = new PDRectangle(rect);
        }
        return retval;
    }

    @Override
    public BoundingBox getBoundingBox()
    {
        if (fontBBox != null)
        {
            return fontBBox;
        }
        PDRectangle rect = getFontBBox();
        if (rect.getLowerLeftX() == 0 && rect.getLowerLeftY() == 0 && rect.getUpperRightX() == 0
                && rect.getUpperRightY() == 0)
        {
            // Plan B: get the max bounding box of the glyphs
            COSDictionary cp = getCharProcs();
            for (COSName name : cp.keySet())
            {
                COSBase base = cp.getDictionaryObject(name);
                if (base instanceof COSStream)
                {
                    PDType3CharProc charProc = new PDType3CharProc(this, (COSStream) base);
                    try
                    {
                        PDRectangle glyphBBox = charProc.getGlyphBBox();
                        rect.setLowerLeftX(
                                Math.min(rect.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                        rect.setLowerLeftY(
                                Math.min(rect.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                        rect.setUpperRightX(
                                Math.max(rect.getUpperRightX(), glyphBBox.getUpperRightX()));
                        rect.setUpperRightY(
                                Math.max(rect.getUpperRightY(), glyphBBox.getUpperRightY()));
                    }
                    catch (IOException ex)
                    {
                        // ignore
                    }
                }
            }
        }
        fontBBox = new BoundingBox(rect.getLowerLeftX(), rect.getLowerLeftY(),
                rect.getUpperRightX(), rect.getUpperRightY());
        return fontBBox;
    }

    /**
     * Returns the dictionary containing all streams to be used to render the glyphs.
     * 
     * @return the dictionary containing all glyph streams.
     */
    public COSDictionary getCharProcs()
    {
        if (charProcs == null)
        {
            charProcs = (COSDictionary) dict.getDictionaryObject(COSName.CHAR_PROCS);
        }
        return charProcs;
    }

    /**
     * Returns the stream of the glyph for the given character code
     * 
     * @param code character code
     * @return the stream to be used to render the glyph
     */
    public PDType3CharProc getCharProc(int code)
    {
        String name = getEncoding().getName(code);
        if (!name.equals(".notdef"))
        {
            COSStream stream;
            stream = (COSStream) getCharProcs().getDictionaryObject(COSName.getPDFName(name));
            if (stream == null)
            {
                return null;
            }
            return new PDType3CharProc(this, stream);
        }
        return null;
    }
}
