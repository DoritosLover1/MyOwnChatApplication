package functions;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import client.MyOwnClient;

public interface LocalFunctions {
    
    static String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalTime.now().format(formatter);
    }
    
    static MyOwnClient isValidUsername(String username, JPanel parentPanel) {
        try {
            MyOwnClient client = new MyOwnClient();
            client.connect("localhost", 5000, username);
            JOptionPane.showMessageDialog(
                    parentPanel,
                    "Başarıyla giriş yapıldı.",
                    "Giriş", 
                    JOptionPane.INFORMATION_MESSAGE);
            
            return client;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parentPanel, 
                    "Sunucuya bağlanırken bir hata oluştu=> " + ex.getMessage(), 
                    "Hata", 
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

	static void logoutUser(MyOwnClient client, JFrame parentFrame, JPanel parentPanel) {
		if (client != null) {
			try {
				client.disconnect();
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(parentPanel, "Çıkış yaparken bir hata oluştu: " + e1.getMessage(), "Hata",
						JOptionPane.ERROR_MESSAGE);
			}
			JOptionPane.showMessageDialog(parentPanel, "Başarıyla çıkış yapıldı.", "Çıkış",
					JOptionPane.INFORMATION_MESSAGE);
			client = null;
			parentFrame.dispose();
		} else {
			JOptionPane.showMessageDialog(parentPanel, "Henüz giriş yapılmadı.", "Hata", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	static ArrayList<String> addFriend(MyOwnClient client, String friendUsername) {
	    if (client != null && !friendUsername.trim().isEmpty() && !friendUsername.equals(client.getUsername())) {
	        try {
	            ArrayList<String> friends = client.addFriend(friendUsername);
	            ArrayList<String> localFriends = client.getLocalFriendsListPointer();
				if (localFriends.contains(friendUsername)) {
					JOptionPane.showMessageDialog(null, friendUsername + " zaten arkadaş listenizde mevcut.", "Bilgi",
							JOptionPane.INFORMATION_MESSAGE);
					return friends;
				}
				else if (friends.contains(friendUsername)) {
	                JOptionPane.showMessageDialog(null,
	                        friendUsername + " başarıyla arkadaş olarak eklendi.",
	                        "Başarılı",
	                        JOptionPane.INFORMATION_MESSAGE);
	                localFriends.add(friendUsername);
	            } else {
	                JOptionPane.showMessageDialog(null,
	                        friendUsername + " eklenemedi veya kullanıcı bulunamadı.",
	                        "Hata",
	                        JOptionPane.ERROR_MESSAGE);
	            }
	            
	            return friends;
	        } catch (Exception e) {
	            JOptionPane.showMessageDialog(null,
	                    "Arkadaş eklenirken bir hata oluştu: " + e.getMessage(),
	                    "Hata",
	                    JOptionPane.ERROR_MESSAGE);
	            return new ArrayList<>();
	        }
	    } else {
	        JOptionPane.showMessageDialog(null,
	                "Henüz giriş yapılmadı.",
	                "Hata",
	                JOptionPane.ERROR_MESSAGE);
	        return new ArrayList<>();
	    }
	}
}