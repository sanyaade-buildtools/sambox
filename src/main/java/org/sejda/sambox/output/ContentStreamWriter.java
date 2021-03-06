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
package org.sejda.sambox.output;

import static org.sejda.sambox.contentstream.operator.Operator.BI_OPERATOR;
import static org.sejda.sambox.contentstream.operator.Operator.EI_OPERATOR;
import static org.sejda.sambox.contentstream.operator.Operator.ID_OPERATOR;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.sejda.io.BufferedCountingChannelWriter;
import org.sejda.io.CountingWritableByteChannel;
import org.sejda.sambox.contentstream.operator.Operator;
import org.sejda.sambox.cos.COSBase;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.cos.COSStream;
import org.sejda.sambox.cos.IndirectCOSObjectReference;
import org.sejda.sambox.util.Charsets;
/**
 * Component capable of writing a content stream tokens, {@link Operator}s and {@link COSBase} operands.
 * 
 * @author Andrea Vacondio
 */
public class ContentStreamWriter extends DefaultCOSWriter
{

    public ContentStreamWriter(CountingWritableByteChannel channel)
    {
        super(channel);
    }

    public ContentStreamWriter(BufferedCountingChannelWriter writer)
    {
        super(writer);
    }

    public void writeTokens(List<Object> tokens) throws IOException
    {
        for (Object token : tokens)
        {
            if (token instanceof COSBase)
            {
                ((COSBase) token).accept(this);
                writeSpace();
            }
            else if (token instanceof Operator)
            {
                this.writeOperator((Operator) token);
            }
            else
            {
                throw new IOException("Unsupported type in content stream:" + token);
            }
        }
    }

    public void writeTokens(Operator... tokens) throws IOException
    {
        for (Operator token : tokens)
        {
            writeOperator(token);
        }
    }

    /**
     * Writes the byte array as is as content of the stream.
     * 
     * @param byteArray
     * @throws IOException
     */
    public void writeContent(byte[] byteArray) throws IOException
    {
        writer().write(byteArray);
    }

    public void writeEOL() throws IOException
    {
        writer().writeEOL();
    }

    public void writeSpace() throws IOException
    {
        writer().write(SPACE);
    }

    private void writeOperator(Operator token) throws IOException
    {
        writer().write(token.getName().getBytes(Charsets.ISO_8859_1));
        if (token.getName().equals(BI_OPERATOR))
        {
            writeEOL();
            COSDictionary imageParams = Optional.ofNullable(token.getImageParameters()).orElseGet(
                    COSDictionary::new);
            for (COSName key : imageParams.keySet())
            {
                key.accept(this);
                writeSpace();
                imageParams.getDictionaryObject(key).accept(this);
                writeEOL();
            }
            writer().write(ID_OPERATOR.getBytes(Charsets.US_ASCII));
            writeEOL();
            writer().write(token.getImageData());
            writeEOL();
            writer().write(EI_OPERATOR.getBytes(Charsets.US_ASCII));
        }
        writeEOL();
    }

    @Override
    public void visit(COSStream value)
    {
        throw new UnsupportedOperationException("Cannot write a stream inside a stream");
    }

    @Override
    public void visit(IndirectCOSObjectReference value)
    {
        throw new UnsupportedOperationException(
                "Cannot write an indirect object reference inside a stream");
    }

    @Override
    public void writeComplexObjectSeparator()
    {
        // write nothing
    }
}
