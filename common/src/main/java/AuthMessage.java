import lombok.Getter;

/**
 * @author Aleksey Yakovlev on 18.02.2018
 * @project CloudStorage
 */
@Getter
public class AuthMessage extends AbstractMessage {
    private String login;
    private String pass;

    public AuthMessage( String login, String pass ) {
        this.login = login;
        this.pass = pass;
    }

}
