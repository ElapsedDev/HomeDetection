package me.elapsed.homedetection.objects;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class HouseHoldMember {

    private int disconnectCount;
    private boolean connected;
    private final String name;

    public HouseHoldMember(String name) {
        this.name = name;
        this.disconnectCount = 0;
        this.connected = false;

    }

    public void addDisconnectCount() {
        this.disconnectCount++;
    }

    public void resetDisconnectCount() {
        this.disconnectCount = 0;
    }

}
