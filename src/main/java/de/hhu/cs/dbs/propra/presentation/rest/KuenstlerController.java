package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Kuenstler;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Path("/kuenstler")
@Produces(MediaType.APPLICATION_JSON)
public class KuenstlerController {

    @Inject
    private DataSource dataSource;

    @GET // Zum Testen Sehen der hinzugefügten Künstler
    public Response getKuenstler(@Context UriInfo ui) {
        ArrayList<Kuenstler> kuenstler = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        String sql = "SELECT * FROM Kuenstler";

        if (parameterCounter != 0) {
            sql += " WHERE ";
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            if (ui.getQueryParameters().containsKey("UserEmail")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("UserEmail"));
            }

            if (ui.getQueryParameters().containsKey("kuenstlername")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("kuenstlername"));
            }

            ResultSet resultSet = preparedStatement.executeQuery();


            while (resultSet.next()){
                kuenstler.add(new Kuenstler(resultSet.getString("UserEmail"),
                        resultSet.getString("kuenstlername")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(kuenstler).build();
    }

    // Neuen Kuenstler hinzufügen
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addKuenstler(@FormDataParam("email") String email, @FormDataParam("passwort") String passwort, @FormDataParam("vorname") String vorname,
                                    @FormDataParam("nachname") String nachname, @FormDataParam("kuenstlername") String kuenstlername, @Context UriInfo uriInfo) {

        int rowid;
        try (Connection connection = this.dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String userSql = "INSERT INTO User (Email, Passwort, Vorname, Nachname )VALUES (?, ?, ?, ?)";
            PreparedStatement userPreparedStatement = connection.prepareStatement(userSql);
            userPreparedStatement.closeOnCompletion();
            userPreparedStatement.setString(1, email);
            userPreparedStatement.setString(2, passwort);
            userPreparedStatement.setString(3, vorname);
            userPreparedStatement.setString(4, nachname);
            userPreparedStatement.executeUpdate();
            String kuenstlerSQL = "INSERT INTO Kuenstler (UserEmail, Kuenstlername) VALUES(?, ?)";
            PreparedStatement kuenstlerPreparedStatement = connection.prepareStatement(kuenstlerSQL);
            kuenstlerPreparedStatement.closeOnCompletion();
            kuenstlerPreparedStatement.setString(1, email);
            kuenstlerPreparedStatement.setString(2, kuenstlername);
            kuenstlerPreparedStatement.executeUpdate();

            ResultSet generatedKeys = userPreparedStatement.getGeneratedKeys();
            rowid = generatedKeys.getInt(1);
            connection.commit();

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(rowid)).build();
        return Response.created(uri).build();
    }
}
