import db.HibernateSessionFactory;
import org.hibernate.Session;

import java.util.List;

/**
 * @author Aleksey Yakovlev on 25.02.2018
 * @project CloudStorage
 */
public class AuthorizationController {
    Session session;


    private static AuthorizationController ourInstance = new AuthorizationController();

    public static AuthorizationController getInstance() {
        return ourInstance;
    }

    private AuthorizationController() {

    }

    public boolean checkLoginPass( String login, String password ) {
        session = HibernateSessionFactory.getSessionFactory().openSession();
        session.beginTransaction();


        List usersList = session
                .createQuery("from UsersEntity where login = :login and password = :pass")
                .setParameter("login", login)
                .setParameter("pass", password)
                .list();

        session.close();
        return usersList.size() == 1;
    }
}
