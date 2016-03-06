package com.todobom.opennotescanner.helpers;

/**
 * Created by allgood on 05/03/16.
 */
public class OpenNoteMessage  {

    private String command;
    private Object obj;

    public OpenNoteMessage( String command , Object obj ) {
        setObj(obj);
        setCommand(command);
    }


    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
