package com.muhammaddaffa.playerprofiles.manager.profile;

public class Profile {

    private final String uuid;
    private boolean locked = false;
    private final Runnable onChange;

    public Profile(String uuid){
        this(uuid, false);
    }

    public Profile(String uuid, boolean lockedStatus){
        this(uuid, lockedStatus, () -> {});
    }

    Profile(String uuid, boolean lockedStatus, Runnable onChange) {
        this.uuid = uuid;
        this.locked = lockedStatus;
        this.onChange = onChange;
    }

    public String getUUID(){
        return uuid;
    }

    public boolean isLocked(){
        return locked;
    }

    public void setLocked(boolean status){
        if (locked == status) return;
        boolean previous = locked;
        locked = status;
        try {
            onChange.run();
        } catch (RuntimeException ex) {
            locked = previous;
            throw ex;
        }
    }

}
