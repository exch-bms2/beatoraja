package bms.player.beatoraja.stream.command;

public abstract class StreamCommand {
    public String COMMAND_STRING;
    abstract public void run(String data);
    abstract public void dispose();
}
