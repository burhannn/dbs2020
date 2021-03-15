package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Ort;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class VeranstalterController {

    @Inject
    private DataSource dataSource;



    // Neuen Veranstalter hinzufügen
    @Path("/veranstalter")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addVeranstalter(@FormDataParam("email") String email, @FormDataParam("passwort") String passwort, @FormDataParam("vorname") String vorname,
                                  @FormDataParam("nachname") String nachname, @FormDataParam("veranstaltername") String veranstaltername, @Context UriInfo uriInfo) {

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
            String veranstalterSQL = "INSERT INTO Veranstalter (UserEmail, Name) VALUES(?, ?)";
            PreparedStatement veranstalterPreparedStatement = connection.prepareStatement(veranstalterSQL);
            veranstalterPreparedStatement.closeOnCompletion();
            veranstalterPreparedStatement.setString(1, email);
            veranstalterPreparedStatement.setString(2, veranstaltername);
            veranstalterPreparedStatement.executeUpdate();

            ResultSet generatedKeys = userPreparedStatement.getGeneratedKeys();
            rowid = generatedKeys.getInt(1);
            connection.commit();

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = uriInfo.getAbsolutePathBuilder().path(String.valueOf(rowid)).build();
        return Response.created(uri).build();
    }
    @RolesAllowed("VERANSTALTER")
    @POST
    @Path("/orte")
    @Consumes("multipart/form-data")
    public Response addeOrt(@FormDataParam("bezeichnung") String bezeichnung, @FormDataParam("land") String land, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        //  Ticket_id,Datum,Preis,VIP_Vermerk,Besucher_Email,Festival_ID
        String sql = "INSERT INTO Ort(Name,Land) VALUES(?, ?)";
        int rowid;
        if (securityContext.isUserInRole("VERANSTALTER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Veranstalter WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            if (securityContext.isUserInRole("BESUCHER")) {
                PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL);
                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
            }

            preparedStatement.setString(1, bezeichnung);
            preparedStatement.setString(2, land);

            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            rowid = generatedKeys.getInt(1);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        URI uri = ui.getAbsolutePathBuilder().path(String.valueOf(rowid)).build();
        return Response.created(uri).build();

    }
    @RolesAllowed("VERANSTALTER")
    @GET // Zum Testen Sehen der hinzugefügten Orte
    @Path("/orte")
    public Response getOrte(@Context UriInfo ui) {
        ArrayList<Ort> orte = new ArrayList<>();

        String sql = "SELECT * FROM Ort";

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            while (resultSet.next()){
                orte.add(new Ort(resultSet.getString("Name"),
                        resultSet.getString("Land")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(orte).build();
    }

}
