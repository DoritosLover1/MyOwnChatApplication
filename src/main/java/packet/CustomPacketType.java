package packet;

public enum CustomPacketType {

    LOGIN,
    LOGIN_USERNAME_ERROR,
    LOGIN_USERNAME_AVAILABLE,
    LOGOUT,

    SEND_MESSAGE,
    RECEIVE_MESSAGE,

    USER_LIST,
    GET_USERS,

    USER_JOINED,
    USER_LEFT,

    INFO,
    ERROR,
    
    ADD_FRIEND,
    REFRESH_FRIENDS_LIST,
    
    PUBLIC_KEY, 
    
    CALL_OFFER,
    CALL_ANSWER,
    CALL_REJECT
}