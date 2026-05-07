package audio;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.net.Socket;

public class AudioReceiver extends Thread {

    private final Socket socket;

    public AudioReceiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(
                    44100,
                    16,
                    1,
                    true,
                    false
            );

            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[4096];

            while (!isInterrupted() && !socket.isClosed()) {

                int count = is.read(buffer);

                if (count == -1) break;

                if (count > 0) {
                    speakers.write(buffer, 0, count);
                }
            }

            speakers.drain();
            speakers.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}