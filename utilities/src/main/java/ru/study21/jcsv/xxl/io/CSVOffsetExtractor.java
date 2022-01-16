package ru.study21.jcsv.xxl.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

// writes an array of long-s; i-th long is i-th row END (!)
// rows are detected with '\n'
// for CRLF we will copy '\r', then write '\n', so all good
// (validity of CSV file is not checked)
public class CSVOffsetExtractor {

    // NOTE: input file MUST have line break at the end!
    public static void extractOffsets(
            FileChannel inputChannel,
            FileChannel outputChannel,
            int readBufferSize,
            int writeBufferSize
    ) throws IOException {
        ByteBuffer readBuf = ByteBuffer.allocate(readBufferSize);
        ByteBuffer writeBuf = ByteBuffer.allocate(writeBufferSize);
        long pos = 0;

        while (inputChannel.read(readBuf) > 0) {
            readBuf.flip();
            while (readBuf.hasRemaining()) {
                byte c = readBuf.get();
                if (c == '\n') {
                    writeBuf.putLong(pos);
                    if (writeBuf.remaining() < 4) {
                        writeBuf.flip();
                        outputChannel.write(writeBuf);
                        writeBuf.clear();
                    }
                }
                pos++;
            }
        }
        if (writeBuf.position() > 0) {
            writeBuf.flip();
            outputChannel.write(writeBuf);
            writeBuf.clear();
        }
    }

}
