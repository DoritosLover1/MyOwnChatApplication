package packet;

import functions.UniqueIdentifier;

public class CustomPacket {

    private int version = 1;
    private String id;
    private String from;
    private String to;
    private CustomPacketType type;
    private int size;
    private String message;

    public CustomPacket(String from, String to, CustomPacketType type, String message) {
        this.id = UniqueIdentifier.getUniqueIdentifier();
        this.from = from;
        this.to = to;
        this.type = type;
        this.message = message;
        this.size = message.length();
    }

    // version|id|from|to|type|size|message
    public static CustomPacket parser(String raw) {

        String[] parts = raw.split("\\|", 7);

        if (parts.length != 7) {
            throw new IllegalArgumentException("Invalid packet format");
        }

        int version = Integer.parseInt(parts[0]);
        String id = parts[1];
        String from = parts[2];
        String to = parts[3];
        CustomPacketType type = CustomPacketType.valueOf(parts[4]);
        int size = Integer.parseInt(parts[5]);
        String message = parts[6];

        if (message.length() != size) {
            throw new IllegalArgumentException("Packet size mismatch");
        }

        CustomPacket packet = new CustomPacket(from, to, type, message);

        packet.version = version;
        packet.id = id;
        packet.size = size;

        return packet;
    }

    public final String serializePacket() {

        return version + "|" +
               id + "|" +
               from + "|" +
               to + "|" +
               type.name() + "|" +
               size + "|" +
               message;
    }

    public final int getVersion() {
        return version;
    }

    public final String getId() {
        return id;
    }

    public final String getFrom() {
        return from;
    }

    public final String getTo() {
        return to;
    }
    
    public final String getFromAndMessage() {
		return from + ": " + message;
	}

    public final CustomPacketType getType() {
        return type;
    }

    public final int getSize() {
        return size;
    }

    public final String getMessage() {
        return message;
    }
}