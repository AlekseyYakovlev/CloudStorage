import lombok.Getter;

/**
 * @author Aleksey Yakovlev on 18.02.2018
 * @project CloudStorage
 */
@Getter
public class CommandMessage extends AbstractMessage{
    public static final int CMD_MSG_AUTH_OK = 951235789;
    public static final int CMD_MSG_REQUEST_FILE_DOWNLOAD = 321654951;
    public static final int CMD_REQUEST_FILE_LIST = 987258456;
    public static final int CMD_MSG_T = 654369147;

    private int type;
    private Object[] attachment;

    public CommandMessage( int type, Object... attachment ) {
        this.type = type;
        this.attachment = attachment;
    }
}
