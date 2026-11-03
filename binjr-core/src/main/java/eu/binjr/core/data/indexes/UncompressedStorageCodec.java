/*
 * Copyright 2001-2025 The Apache Software Foundation
 * Copyright 2026 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.core.data.indexes;

import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.compressing.CompressionMode;
import org.apache.lucene.codecs.compressing.Compressor;
import org.apache.lucene.codecs.compressing.Decompressor;
import org.apache.lucene.codecs.lucene104.Lucene104Codec;
import org.apache.lucene.codecs.lucene90.compressing.Lucene90CompressingStoredFieldsFormat;
import org.apache.lucene.store.ByteBuffersDataInput;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;


public class UncompressedStorageCodec extends FilterCodec {

    private final StoredFieldsFormat fieldsFormat = new Lucene90CompressingStoredFieldsFormat(
            "UncompressedStoredFields", new CompressionMode() {
        @Override
        public Compressor newCompressor() {
            return new Compressor() {
                @Override
                public void close() {
                }

                @Override
                public void compress(ByteBuffersDataInput buffersInput, org.apache.lucene.store.DataOutput out)
                        throws IOException {
                    out.copyBytes(buffersInput, buffersInput.length());
                }
            };
        }

        @Override
        public Decompressor newDecompressor() {
            return new Decompressor() {
                @Override
                public void decompress(
                        DataInput in, int originalLength, int offset, int length, BytesRef bytes)
                        throws IOException {
                    bytes.bytes = ArrayUtil.growNoCopy(bytes.bytes, length);
                    in.skipBytes(offset);
                    in.readBytes(bytes.bytes, 0, length);
                    bytes.offset = 0;
                    bytes.length = length;
                }

                @Override
                public Decompressor clone() {
                    return this;
                }
            };
        }
    }, 128 * 1024, 1, 10);

    public UncompressedStorageCodec() {
        super("UncompressedStorageCodec", new Lucene104Codec());
    }

    @Override
    public StoredFieldsFormat storedFieldsFormat() {
        return fieldsFormat;
    }
}