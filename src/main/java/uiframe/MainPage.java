package uiframe;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.DefaultListModel;
import client.MyOwnClient;
import functions.LocalFunctions;
import javax.swing.JTextArea;

public class MainPage extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTextField textField;
    private JTextField textField_2;
    private JTextField textField_1;

    private MyOwnClient client;

    private JLabel callStatus;

    public MainPage(JFrame parentFrame) {

        setMaximumSize(new Dimension(600, 400));
        setMinimumSize(new Dimension(600, 400));
        setLayout(null);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);

        textField = new JTextField();
        textField.setBounds(10, 31, 86, 20);
        add(textField);

        JLabel lblListe = new JLabel("Liste");
        lblListe.setBounds(39, 95, 46, 14);
        add(lblListe);

        JLabel lblNewLabel = new JLabel("Adınız");
        lblNewLabel.setBounds(39, 11, 46, 14);
        add(lblNewLabel);

        textField_2 = new JTextField();
        textField_2.setBounds(125, 330, 320, 20);
        add(textField_2);

        JButton btnNewButton_1 = new JButton("=>");
        btnNewButton_1.setFocusable(false);
        btnNewButton_1.setBounds(455, 330, 66, 23);
        add(btnNewButton_1);

        btnNewButton_1.addActionListener(e -> {
            String message = textField_2.getText().trim();
            String selectedFriend = list.getSelectedValue();

            if (selectedFriend != null && !message.isEmpty()) {
                client.sendMessage(selectedFriend, message);
                textField_2.setText("");
            } else {
                JOptionPane.showMessageDialog(this,
                        "Lütfen bir arkadaş seçin ve mesajınızı girin",
                        "Uyarı",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton callButton = new JButton("📞");
        callButton.setFocusable(false);
        callButton.setBounds(525, 330, 50, 23);
        add(callButton);

        callButton.addActionListener(e -> {
            if (client != null) {
                String selectedFriend = list.getSelectedValue();

                if (selectedFriend != null) {
                    callStatus.setText("📞 Aranıyor...");
                    client.startCall(selectedFriend);
                } else {
                    JOptionPane.showMessageDialog(this, "Bir kullanıcı seç!");
                }
            }
        });

        textField_1 = new JTextField();
        textField_1.setEditable(false);
        textField_1.setEnabled(false);
        textField_1.setBounds(504, 31, 86, 20);
        add(textField_1);

        Timer timer = new Timer(1000, e -> {
            textField_1.setText(LocalFunctions.getCurrentTime().toString());
        });
        timer.start();

        JTextArea textArea = new JTextArea();
        textArea.setBounds(125, 110, 320, 206);
        textArea.setEditable(false);
        add(textArea);

        callStatus = new JLabel("Boşta");
        callStatus.setBounds(250, 10, 150, 20);
        add(callStatus);

        JButton btnNewButton = new JButton("Giriş");
        btnNewButton.setFocusable(false);
        btnNewButton.setBounds(106, 30, 89, 23);
        add(btnNewButton);

        btnNewButton.addActionListener(e -> {

            client = LocalFunctions.isValidUsername(textField.getText(), this);

            if (client != null) {
            	
            	client.setOnCallRejected(user -> {
            	    SwingUtilities.invokeLater(() -> {
            	    	JOptionPane.showMessageDialog(this, user + " çağrınızı reddetti.", "Çağrı Reddedildi", JOptionPane.INFORMATION_MESSAGE);
            	    	callStatus.setText("❌ reddedildi");
            	    });
            	});
            	
                client.setOnUserListUpdated(users -> {
                    SwingUtilities.invokeLater(() -> {
                        model.clear();
                        for (String user : users) {
                            model.addElement(user);
                        }
                    });
                });

                client.setOnMessageReceived(incomingMessage -> {
                    SwingUtilities.invokeLater(() -> {
                        String selectedFriend = list.getSelectedValue();
                        if (selectedFriend != null &&
                                incomingMessage.startsWith(selectedFriend + ":")) {
                            textArea.append(incomingMessage + "\n");
                        }
                    });
                });

                client.setOnIncomingCall(data -> {

                    SwingUtilities.invokeLater(() -> {

                        String[] parts = data.split("\\|");
                        String caller = parts[0];
                        String ipPort = parts[1];

                        callStatus.setText("📞 Gelen çağrı");

                        int choice = JOptionPane.showConfirmDialog(
                                this,
                                caller + " seni arıyor 📞\nKabul ediyor musun?",
                                "Gelen Çağrı",
                                JOptionPane.YES_NO_OPTION
                        );

                        if (choice == JOptionPane.YES_OPTION) {
                            callStatus.setText(caller + "ile 🟢 görüşme başladı");
                            client.acceptCall(caller, ipPort);
                        } else {
                            callStatus.setText("Tarafımızca ❌ reddedildi");
                	    	JOptionPane.showMessageDialog(this, "Çağrı reddedildi.", "Çağrı Reddedildi", JOptionPane.INFORMATION_MESSAGE);
                            client.rejectCall(caller);
                        }
                    });
                });
            }
        });

        JButton btnNewButton_2 = new JButton("Çıkış");
        btnNewButton_2.setFocusable(false);
        btnNewButton_2.setBounds(196, 30, 89, 23);
        add(btnNewButton_2);

        btnNewButton_2.addActionListener(e -> {
            LocalFunctions.logoutUser(client, parentFrame, this);
            model.clear();
        });

        JTextField addFriendField = new JTextField();
        addFriendField.setBounds(10, 60, 86, 20);
        add(addFriendField);

        JButton addFriendButton = new JButton("+");
        addFriendButton.setFocusable(false);
        addFriendButton.setBounds(106, 60, 50, 20);
        add(addFriendButton);

        addFriendButton.addActionListener(e -> {

            if (client != null) {

                String friendUsername = addFriendField.getText().trim();

                if (!friendUsername.isEmpty() && !friendUsername.equals(client.getUsername())) {

                    ArrayList<String> updatedFriends =
                            LocalFunctions.addFriend(client, friendUsername);

                    SwingUtilities.invokeLater(() -> {
                        model.clear();
                        for (String friend : updatedFriends) {
                            model.addElement(friend);
                        }
                    });

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Lütfen bir kullanıcı adı girin",
                            "Uyarı",
                            JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Önce giriş yapmalısınız",
                        "Hata",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        list.setBounds(10, 110, 86, 267);
        add(list);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && client != null) {

                String selectedFriend = list.getSelectedValue();

                if (selectedFriend != null) {
                    textArea.setText("");

                    var messages = client.getFriendsMessages(selectedFriend);

                    if (messages != null) {
                        messages.forEach(msg -> textArea.append(msg + "\n"));
                    }
                }
            }
        });
    }
}