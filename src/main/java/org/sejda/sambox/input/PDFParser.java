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
package org.sejda.sambox.input;

import static java.util.Objects.requireNonNull;
import static org.sejda.sambox.util.SpecVersionUtils.PDF_HEADER;
import static org.sejda.util.RequireUtils.requireIOCondition;

import java.io.IOException;
import java.util.Optional;

import org.sejda.io.SeekableSource;
import org.sejda.sambox.cos.COSDocument;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.encryption.DecryptionMaterial;
import org.sejda.sambox.pdmodel.encryption.PDEncryption;
import org.sejda.sambox.pdmodel.encryption.SecurityHandler;
import org.sejda.sambox.pdmodel.encryption.StandardDecryptionMaterial;
import org.sejda.sambox.util.SpecVersionUtils;
import org.sejda.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides public entry point to parse a {@link SeekableSource} and obtain a {@link PDDocument}.
 * 
 * @author Andrea Vacondio
 */
public class PDFParser
{
    private static final Logger LOG = LoggerFactory.getLogger(PDFParser.class);

    /**
     * Parses the given {@link SeekableSource} returning the corresponding {@link PDDocument}.
     * 
     * @param source
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source) throws IOException
    {
        return parse(source, (String) null);
    }

    /**
     * Parses the given {@link SeekableSource} using the given password, returning the corresponding decrypted
     * {@link PDDocument}.
     * 
     * @param source {@link SeekableSource} to parse
     * @param password to be used for decryption. Optional.
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source, String password) throws IOException
    {
        return parse(source, Optional.ofNullable(password).map(StandardDecryptionMaterial::new)
                .orElse(null));
    }

    /**
     * Parses the given {@link SeekableSource} using the given {@link DecryptionMaterial} and returning the
     * corresponding decrypted {@link PDDocument}.
     * 
     * @param source {@link SeekableSource} to parse
     * @param decryptionMaterial to be used for decryption. Optional.
     * @return the parsed document
     * @throws IOException
     */
    public static PDDocument parse(SeekableSource source, DecryptionMaterial decryptionMaterial)
            throws IOException
    {
        requireNonNull(source);
        COSParser parser = new COSParser(source);
        PDDocument document = doParse(decryptionMaterial, parser);
        document.setOnCloseAction(() -> {
            IOUtils.close(parser.provider());
            IOUtils.close(parser);
        });
        return document;
    }

    private static PDDocument doParse(DecryptionMaterial decryptionMaterial, COSParser parser)
            throws IOException
    {
        String headerVersion = readHeader(parser);
        LOG.trace("Parsed header version: " + headerVersion);
        XrefParser xrefParser = new XrefParser(parser);
        xrefParser.parse();

        COSDocument document = new COSDocument(xrefParser.trailer(), headerVersion);
        if (document.isEncrypted())
        {
            LOG.debug("Preparing for document decryption");
            PDEncryption encryption = new PDEncryption(document.getEncryptionDictionary());

            SecurityHandler securityHandler = encryption.getSecurityHandler();
            securityHandler.prepareForDecryption(encryption, document.getDocumentID(), Optional
                    .ofNullable(decryptionMaterial).orElse(new StandardDecryptionMaterial("")));
            parser.provider().initializeWith(securityHandler);
            return new PDDocument(document, securityHandler.getCurrentAccessPermission());
        }
        return new PDDocument(document);
    }

    private static String readHeader(COSParser parser) throws IOException
    {
        parser.position(0);
        int headerIndex = -1;
        String header = parser.readLine();
        while ((headerIndex = header.indexOf(PDF_HEADER)) < 0)
        {
            // if a line starts with a digit, it has to be the first one with data in it
            if ((header.length() > 0) && (Character.isDigit(header.charAt(0))))
            {
                throw new IOException("Unable to find expected file header");
            }
            header = parser.readLine();
        }

        final String trimmedLeftHeader = header.substring(headerIndex, header.length());
        requireIOCondition(trimmedLeftHeader.length() >= SpecVersionUtils.EXPECTED_HEADER_LENGTH,
                "Unable to find expected header '%PDF-n.n'");
        LOG.debug("Found header " + trimmedLeftHeader);
        return SpecVersionUtils.parseHeaderString(trimmedLeftHeader);
    }
}
