package com.github.kaindljulian.piano.lwjgl3;

import com.github.kaindljulian.piano.audio.AudioCapture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Plays a WAV file and returns each block as it's handed to the speakers, like a capture of that playback. */
class WavFileCapture implements AudioCapture {
    private static final int LINE_BUFFER_FRAMES = 2048;

    private final AudioInputStream stream;
    private final SourceDataLine line;
    private final int channelCount;
    private byte[] bytes = new byte[0];

    WavFileCapture(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        AudioInputStream source = AudioSystem.getAudioInputStream(file);
        channelCount = source.getFormat().getChannels();
        AudioFormat format = new AudioFormat(source.getFormat().getSampleRate(), 16, channelCount, true, false);
        stream = AudioSystem.getAudioInputStream(format, source);
        line = AudioSystem.getSourceDataLine(format);
        line.open(format, LINE_BUFFER_FRAMES * format.getFrameSize());
    }

    @Override
    public int getSampleRate() {
        return Math.round(stream.getFormat().getSampleRate());
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public void start() {
        line.start();
    }

    @Override
    public int read(float[] buffer, int offset, int length) {
        length -= length % channelCount;
        if (bytes.length < length * 2) {
            bytes = new byte[length * 2];
        }
        int read;
        try {
            read = stream.read(bytes, 0, length * 2);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (read <= 0) {
            return -1;
        }
        line.write(bytes, 0, read);
        for (int i = 0; i < read / 2; i++) {
            buffer[offset + i] = (short) ((bytes[2 * i + 1] << 8) | (bytes[2 * i] & 0xff)) / 32768f;
        }
        return read / 2;
    }

    @Override
    public void stop() {
        line.stop();
        line.flush();
    }

    @Override
    public void close() {
        line.close();
        try {
            stream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
