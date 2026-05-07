package ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import uiframe.MainPage;
import javax.swing.JFrame;

public class ApplicationBuilderMain {

    private JFrame frame;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ApplicationBuilderMain window = new ApplicationBuilderMain();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public ApplicationBuilderMain() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setResizable(false);
        frame.setTitle("Chat Application by DoritosLover1");
        frame.setBounds(100, 100, 620, 450);
        frame.setMaximumSize(new Dimension(620, 450));
        frame.setMinimumSize(new Dimension(620, 450));
        frame.getContentPane().add(new MainPage(frame));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}