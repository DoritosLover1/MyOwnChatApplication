package audio;

import javax.sound.sampled.*;
import java.io.OutputStream;
import java.net.Socket;

public class AudioSender extends Thread {

    private final Socket socket;
    private TargetDataLine mic;

    public AudioSender(Socket socket) {
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

            mic = AudioSystem.getTargetDataLine(format);
            mic.open(format);
            mic.start();

            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];

            while (!isInterrupted() && !socket.isClosed()) {

                int count = mic.read(buffer, 0, buffer.length);

                if (count > 0) {
                    os.write(buffer, 0, count);
                    os.flush();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (mic != null) {
                    mic.stop();
                    mic.close();
                }
            } catch (Exception ignored) {}
        }
    }
}