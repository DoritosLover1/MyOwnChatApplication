package client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

import audio.AudioReceiver;
import audio.AudioSender;
import packet.CustomPacket;
import packet.CustomPacketType;

import javax.crypto.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class MyOwnClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private ServerSocket audioServer;
    private Socket audioSocket;
    private AudioSender audioSender;
    private AudioReceiver audioReceiver;
    
    private volatile boolean isInCall = false;
    private boolean shutdownHookAdded = false;

    private KeyPair myKeyPair;
    private final HashMap<String, PublicKey> friendsPublicKeys = new HashMap<>();
    
    private final HashMap<String, SecretKey> friendsAESKeys = new HashMap<>();

    private final BlockingQueue<CustomPacket> userListQueue = new LinkedBlockingQueue<>();
    private final ArrayList<String> friendsList = new ArrayList<>();
    private final HashMap<String, Queue<String>> friendsMessages = new HashMap<>();

    private Consumer<List<String>> onUserListUpdated;
    private Consumer<String> onMessageReceived;
    private Consumer<String> onIncomingCall;
    private Consumer<String> onCallRejected;

    public void setOnCallRejected(Consumer<String> callback) {
        this.onCallRejected = callback;
    }
    
    public Queue<String> getFriendsMessages(String friendUsername) {
        return friendsMessages.get(friendUsername);
    }
    
    public ArrayList<String> getLocalFriendsListPointer() {
        return friendsList;
    }

    public void setOnUserListUpdated(Consumer<List<String>> callback) {
        this.onUserListUpdated = callback;
    }

    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }

    public void setOnIncomingCall(Consumer<String> callback) {
        this.onIncomingCall = callback;
    }


    public void connect(String host, int port, String username) throws Exception {
        this.username = username;

        generateRSAKeyPair();

        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        String publicKeyString = encodePublicKey(myKeyPair.getPublic());
        
        out.println(new CustomPacket(username, "SERVER",
                CustomPacketType.LOGIN, publicKeyString).serializePacket());

        String raw = in.readLine();
        if (raw != null) {
            CustomPacket response = CustomPacket.parser(raw);
            if (response.getType() == CustomPacketType.LOGIN_USERNAME_ERROR) {
                socket.close();
                throw new Exception(response.getMessage());
            }
        }

        if (!shutdownHookAdded) {
            shutdownHookAdded = true;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (socket != null && !socket.isClosed()) {
                        disconnect();
                    }
                } catch (Exception ignored) {}
            }));
        }

        startListening();
    }

    private void generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        myKeyPair = keyGen.generateKeyPair();
    }

    private String encodePublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        return Base64.getEncoder().encodeToString(encoded);
    }

    private PublicKey decodePublicKey(String encodedKey) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    private String encryptWithRSA(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptWithRSA(String ciphertext, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    private String encryptWithAES(String plaintext, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptWithAES(String ciphertext, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String raw;
                while ((raw = in.readLine()) != null) {
                    CustomPacket packet = CustomPacket.parser(raw);

                    switch (packet.getType()) {

                        case RECEIVE_MESSAGE:
                            handleMessage(packet);
                            break;

                        case CALL_OFFER:
                            if (onIncomingCall != null) {
                                SwingUtilities.invokeLater(() ->
                                        onIncomingCall.accept(packet.getFrom() + "|" + packet.getMessage()));
                            }
                            break;

                        case CALL_ANSWER:
                            break;

                        case CALL_REJECT:
                            stopCall();

                            if (onCallRejected != null) {
                                SwingUtilities.invokeLater(() ->
                                        onCallRejected.accept(packet.getFrom()));
                            }

                            break;

                        case PUBLIC_KEY:

                            try {
                                String payload = packet.getMessage();
                                String sender = packet.getFrom();
                                
                                try {
                                    String decryptedAESKey = decryptWithRSA(payload, myKeyPair.getPrivate());
                                    byte[] decodedKey = Base64.getDecoder().decode(decryptedAESKey);
                                    SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                                    friendsAESKeys.put(sender, aesKey);
                                } catch (Exception decryptFailed) {
   
                                    try {
                                        PublicKey friendPublicKey = decodePublicKey(payload);
                                        friendsPublicKeys.put(sender, friendPublicKey);
                                        friendsPublicKeys.keySet();
                                    } catch (Exception decodeError) {
                                        decodeError.printStackTrace();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case USER_LIST:
                        case REFRESH_FRIENDS_LIST:
                            List<String> list = Arrays.asList(packet.getMessage().split(","));
                            friendsList.clear();
                            friendsList.addAll(list);

                            String myPublicKeyString = encodePublicKey(myKeyPair.getPublic());
                            for (String friend : list) {
                                if (friend != null && !friend.isEmpty() && !friend.equals(username)) {
                                    if (!friendsPublicKeys.containsKey(friend)) {
                                        out.println(new CustomPacket(
                                                username, friend,
                                                CustomPacketType.PUBLIC_KEY,
                                                myPublicKeyString
                                        ).serializePacket());
                                    }
                                }
                            }

                            if (onUserListUpdated != null)
                                onUserListUpdated.accept(list);

                            userListQueue.put(packet);
                            break;

                        default:
                            System.out.println(packet.getMessage());
                    }
                }
            } catch (Exception e) {
                stopCall();
            }
        }).start();
    }

    private void handleMessage(CustomPacket packet) {
        try {
            String sender = packet.getFrom();
            String encryptedMessage = packet.getMessage();

            String decryptedMessage = "[Şifre çözülemedi - dekripsiyon başarısız]";

            try {
                SecretKey aesKey = friendsAESKeys.get(sender);
                if (aesKey != null) {
                    decryptedMessage = decryptWithAES(encryptedMessage, aesKey);
                } else {
                    decryptedMessage = "[Hata: " + sender + " ile henüz session anahtarı kurulmadı]";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalMessage = decryptedMessage;

            friendsMessages
                    .computeIfAbsent(sender, k -> new LinkedBlockingQueue<>())
                    .add(finalMessage);

            if (onMessageReceived != null) {
                SwingUtilities.invokeLater(() ->
                        onMessageReceived.accept(sender + ": " + finalMessage));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCall(String target) {
        new Thread(() -> {
            try {
                int port = 6000 + new Random().nextInt(1000);

                audioServer = new ServerSocket(port);
                String ip = socket.getLocalAddress().getHostAddress();

                out.println(new CustomPacket(
                        username, target,
                        CustomPacketType.CALL_OFFER,
                        ip + ":" + port
                ).serializePacket());


                try {
                	audioSocket = audioServer.accept();

                	if (isInCall) {
                	    startAudio(audioSocket);
                	}
                } catch (SocketException e) {
                    
                }
                
                startAudio(audioSocket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void acceptCall(String fromUser, String ipPort) {
        new Thread(() -> {
            try {
                String[] parts = ipPort.split(":");

                out.println(new CustomPacket(
                        username, fromUser,
                        CustomPacketType.CALL_ANSWER,
                        "OK"
                ).serializePacket());

                audioSocket = new Socket(parts[0], Integer.parseInt(parts[1]));
                startAudio(audioSocket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void rejectCall(String fromUser) {
        out.println(new CustomPacket(
                username, fromUser,
                CustomPacketType.CALL_REJECT,
                "REJECT"
        ).serializePacket());
    }

    private void startAudio(Socket socket) throws Exception {
        isInCall = true;

        if (audioSender != null || audioReceiver != null) {
            stopCall();
        }

        audioSender = new AudioSender(socket);
        audioReceiver = new AudioReceiver(socket);

        audioSender.start();
        audioReceiver.start();
    }

    public void stopCall() {
        try {
            isInCall = false;

            if (audioSender != null) {
                audioSender.interrupt();
                audioSender = null;
            }

            if (audioReceiver != null) {
                audioReceiver.interrupt();
                audioReceiver = null;
            }

            try {
                if (audioSocket != null) {
                    audioSocket.close();
                    audioSocket = null;
                }
            } catch (Exception ignored) {}

            try {
                if (audioServer != null) {
                    audioServer.close();
                    audioServer = null;
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> addFriend(String friendUsername) throws Exception {
        out.println(new CustomPacket(
                this.username,
                "SERVER",
                CustomPacketType.ADD_FRIEND,
                friendUsername
        ).serializePacket());

        CustomPacket response = userListQueue.poll(5, TimeUnit.SECONDS);

        if (response != null) {
            Thread.sleep(200);
            String myPublicKeyString = encodePublicKey(myKeyPair.getPublic());
            out.println(new CustomPacket(
                    username, friendUsername,
                    CustomPacketType.PUBLIC_KEY,
                    myPublicKeyString
            ).serializePacket());
            
            return new ArrayList<>(Arrays.asList(response.getMessage().split(",")));
        }

        return new ArrayList<>();
    }

    public void sendMessage(String target, String message) {
        new Thread(() -> {
            try {
                PublicKey targetPublicKey = friendsPublicKeys.get(target);

                if (targetPublicKey == null) {
                    return;
                }

                SecretKey sharedAESKey = friendsAESKeys.get(target);

                if (sharedAESKey == null) {
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    keyGen.init(128);
                    sharedAESKey = keyGen.generateKey();
                    friendsAESKeys.put(target, sharedAESKey);

                    String encodedAESKey = Base64.getEncoder().encodeToString(sharedAESKey.getEncoded());
                    String encryptedAESKey = encryptWithRSA(encodedAESKey, targetPublicKey);

                    out.println(new CustomPacket(
                            username, target,
                            CustomPacketType.PUBLIC_KEY,
                            encryptedAESKey
                    ).serializePacket());
                    
                }

                String encryptedMessage = encryptWithAES(message, sharedAESKey);

                out.println(new CustomPacket(
                        username, target,
                        CustomPacketType.SEND_MESSAGE,
                        encryptedMessage
                ).serializePacket());


            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void disconnect() {
        try {
            stopCall();

            if (out != null) {
                out.println(new CustomPacket(
                        username, "SERVER",
                        CustomPacketType.LOGOUT,
                        username + " is logging out"
                ).serializePacket());
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (Exception e) {
        }
    }

	public String getUsername() {
		return username;
	}
}