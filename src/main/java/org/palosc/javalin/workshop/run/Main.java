/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.palosc.javalin.workshop.run;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;
import static io.javalin.core.security.SecurityUtil.roles;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdbi.v3.core.Jdbi;
import org.mariadb.jdbc.MariaDbDataSource;
import org.palosc.javalin.workshop.model.UserRole;
import org.palosc.javalin.workshop.repository.JdbiManager;
import org.palosc.javalin.workshop.rest.AuthenticationRestApi;
import org.palosc.javalin.workshop.rest.TodoItemRestApi;
import org.palosc.javalin.workshop.rest.middleware.TokenValidator;

/**
 *
 * @author tareq
 */
public class Main {

    public static void main(String[] args) {
        prepareDatasource();
        TodoItemRestApi todoItems = new TodoItemRestApi();
        TokenValidator validator = new TokenValidator();
        AuthenticationRestApi authentication = new AuthenticationRestApi();
        Javalin app = Javalin.create((it) -> {
            it.accessManager(validator::validate);
        }).start(7000);

        app.routes(() -> {
            path("/todo", () -> {
                get(todoItems::get, roles(UserRole.USER));
                post(todoItems::post, roles(UserRole.USER));
                path(":id", () -> {
                    get(todoItems::getById, roles(UserRole.USER, UserRole.ADMIN));
                    put(todoItems::put, roles(UserRole.USER, UserRole.ADMIN));
                    delete(todoItems::delete, roles(UserRole.USER, UserRole.ADMIN));
                });
            });
            path("/auth", () -> {
                path("/signup", () -> {
                    post(authentication::signup, roles(UserRole.NONE));
                });

                path("/login", () -> {
                    post(authentication::login, roles(UserRole.NONE));
                });
            });
            path("/users", () -> {
                get(authentication::listUsers, roles(UserRole.ADMIN));
            });
        });
        app.config.addStaticFiles("/webapp");
    }

     private static void prepareDatasource() {
        try {
            MariaDbDataSource source = new MariaDbDataSource();
            source.setDatabaseName("todoapi");
            source.setUserName("todoapi");
            source.setPassword("todoapi");
            source.setServerName("localhost");
            JdbiManager.set(Jdbi.create(source));
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
