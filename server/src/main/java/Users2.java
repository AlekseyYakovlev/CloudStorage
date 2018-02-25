import lombok.Data;

/**
 * @author Aleksey Yakovlev on 25.02.2018
 * @project CloudStorage
 */
@Data
public class Users2 {
    private int id;
    private String login;
    private String password;
    private boolean enabled;
}
