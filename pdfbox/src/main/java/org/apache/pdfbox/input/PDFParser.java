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
package org.apache.pdfbox.input;

import java.io.IOException;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;

/**
 * @author Andrea Vacondio
 *
 */
public final class PDFParser
{
    private PDFParser()
    {
        // utility
    }

    /**
     * Parses the given {@link SeekableSource} returning the corresponding {@link PDDocument}.
     * 
     * @param source
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source) throws IOException
    {
        return parse(source, null, null);
    }

    /**
     * Parses the given {@link SeekableSource} using the provided password returning the corresponding decrypted
     * {@link PDDocument}.
     * 
     * @param source
     * @param password the password to decrypt the document
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source, String password) throws IOException
    {
        return parse(source, null, password);
    }

    /**
     * Parses the given {@link SeekableSource} using the given password, returning the corresponding decrypted
     * {@link PDDocument}. A custom {@link IndirectObjectsProvider} can be provided to use a different strategy to load
     * objects from the document; if null the default provider is used and objects are lazy loaded when the object is
     * accessed.
     * 
     * @param source {@link SeekableSource} to parse
     * @param provider {@link IndirectObjectsProvider} to use. Optional.
     * @param password to be used for decryption. Optional.
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source, IndirectObjectsProvider provider,
            String password) throws IOException
    {
        return DefaultPDFParser.parse(source, provider,
                Optional.ofNullable(password).map(StandardDecryptionMaterial::new).orElse(null));
    }
}
