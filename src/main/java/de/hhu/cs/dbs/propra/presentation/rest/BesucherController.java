package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Besucher;
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

@Path("/besucher")
@Produces(MediaType.APPLICATION_JSON)
public class BesucherController {

    @Inject
    private DataSource dataSource;

    @GET // Zum Testen Sehen der hinzugefügten Besucher
    public Response getBesucher(@Context UriInfo ui) {
        ArrayList<Besucher> besucher = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        String sql = "SELECT * FROM Besucher";

        if (parameterCounter != 0) {
            sql += " WHERE ";
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            if (ui.getQueryParameters().containsKey("UserEmail")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("UserEmail"));
            }

            if (ui.getQueryParameters().containsKey("geburtsdatum")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("geburtsdatum"));
            }
            if (ui.getQueryParameters().containsKey("telefonnummer")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("telefonnummer"));
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            while (resultSet.next()){
                besucher.add(new Besucher(resultSet.getString("UserEmail"),
                        resultSet.getString("geburtsdatum"),
                        resultSet.getString("telefonnummer")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(besucher).build();
    }

    // Neuen Besucher hinzufügen
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addBesucher(@FormDataParam("email") String email, @FormDataParam("passwort") String passwort, @FormDataParam("vorname") String vorname,
                                 @FormDataParam("nachname") String nachname, @FormDataParam("geburtsdatum") String geburtsdatum, @FormDataParam("telefonnummer") String telefonnummer, @Context UriInfo uriInfo) {

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
            String besucherSQL = "INSERT INTO Besucher (UserEmail, Geburtsdatum, Telefonnummer) VALUES(?, ?, ?)";
            PreparedStatement besucherPreparedStatement = connection.prepareStatement(besucherSQL);
            besucherPreparedStatement.closeOnCompletion();
            besucherPreparedStatement.setString(1, email);
            besucherPreparedStatement.setString(2, geburtsdatum);
            besucherPreparedStatement.setString(3, telefonnummer);
            besucherPreparedStatement.executeUpdate();

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
