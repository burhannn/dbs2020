package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Band;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

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
import java.util.List;
import java.util.Map;


@Path("/bands")
@Produces(MediaType.APPLICATION_JSON)
public class BandController {
    @Inject
    private DataSource dataSource;

    @Context
    private SecurityContext securityContext;


    /*
     * Anzeigen aller Bands
     */
    @GET
    public Response getBands(@Context UriInfo ui) {
        ArrayList<Band> bands = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        String sql = "SELECT * FROM Band";

        if (parameterCounter != 0) {
            sql += " WHERE ";
        }

        if (ui.getQueryParameters().containsKey("name")) {
            sql += "Name like ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("gruendungsjahr")) {
            sql += "strftime('%Y', datum) = ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("genre")) {
            sql += "B_ID = (SELECT B_ID FROM Band_hat_Genre WHERE GenreName LIKE ?)";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            if (ui.getQueryParameters().containsKey("name")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("name") + "%");
            }

            if (ui.getQueryParameters().containsKey("gruendungsjahr")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("gruendungsjahr"));
            }

            if (ui.getQueryParameters().containsKey("genre")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("genre") + "%");
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();  //404
            }
            while (resultSet.next()){
                bands.add(new Band(resultSet.getInt("B_ID"),
                        resultSet.getString("name"),
                        resultSet.getInt("gruendungsjahr")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(bands).build();
    }
    // Hinzufügen einer Band und Künstler als Mitglied eintragen
    @RolesAllowed("KUENSTLER")
    @POST
    @Consumes("multipart/form-data")
    public Response addeBand(FormDataMultiPart formData, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        int bandID;
        //  Band
        String sqlBandID = "SELECT MAX(B_ID) FROM Band";
        String sqlBand = "INSERT INTO Band(B_ID, Name, Gruendungsjahr) VALUES(?, ?, ?)";
        // Bandmitglied und Genre der Band
        String sqlMitgliedBand = "INSERT INTO Band_gehoert_Kuenstler(B_ID, KuenstlerEmail) VALUES(?, ?)";
        String sqlBandGenre = "INSERT INTO Band_hat_Genre(B_ID, GenreName) VALUES(?, (SELECT Name FROM Genre WHERE ROWID = ?))";
        if (securityContext.isUserInRole("KUENSTLER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Kuenstler WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatementBandID = connection.prepareStatement(sqlBandID);
             PreparedStatement preparedStatementBand = connection.prepareStatement(sqlBand);
             PreparedStatement preparedStatementMitglied = connection.prepareStatement(sqlMitgliedBand);
             PreparedStatement preparedStatementGenre = connection.prepareStatement(sqlBandGenre);
             PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {

            ResultSet res = preparedStatementBandID.executeQuery();
            bandID = res.getInt(1) + 1;      //Leider hardgecodet
            if (securityContext.isUserInRole("KUENSTLER")) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
                connection.setAutoCommit(false);

                preparedStatementBand.setInt(1, bandID);
                preparedStatementBand.setString(2, felder.get("name").get(0).getValue());
                preparedStatementBand.setInt(3, Integer.valueOf(felder.get("gruendungsjahr").get(0).getValue()));

                preparedStatementBand.executeUpdate();


                preparedStatementMitglied.setInt(1, bandID);
                preparedStatementMitglied.setString(2, authMail);

                preparedStatementMitglied.executeUpdate();

                preparedStatementGenre.setInt(1, bandID);
                preparedStatementGenre.setInt(2, Integer.valueOf(felder.get("genreid").get(0).getValue()));

                preparedStatementGenre.executeUpdate();

                connection.commit();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = ui.getAbsolutePathBuilder().path("bands/").build();
        return Response.created(uri).build();

    }
    // Einer Band einen Künstler hinzufügen
    @RolesAllowed("KUENSTLER")
    @POST
    @Path("/{bandid}/kuenstler")
    @Consumes("multipart/form-data")
    public Response addeKünstler(FormDataMultiPart formData, @PathParam("bandid") int bandID, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        String sqlBand = "INSERT INTO Band_gehoert_Kuenstler(B_ID, KuenstlerEmail) VALUES((SELECT B_ID FROM Band WHERE ROWID = ?), (SELECT UserEmail FROM Kuenstler WHERE ROWID =?))";
        // Bandmitglied
        String sqlMitgliedBand = "SELECT KuenstlerEmail FROM Band_gehoert_Kuenstler WHERE B_ID = ? AND KuenstlerEmail = ?";
        if (securityContext.isUserInRole("KUENSTLER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Kuenstler WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatementBand = connection.prepareStatement(sqlBand);
             PreparedStatement preparedStatementMitglied = connection.prepareStatement(sqlMitgliedBand);
             PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {

            if (securityContext.isUserInRole("KUENSTLER")) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(401);
                }
                preparedStatementMitglied.setInt(1,bandID);
                preparedStatementMitglied.setString(2, authMail);

                ResultSet resultSetM = preparedStatementMitglied.executeQuery();

                if(resultSetM.isClosed()){
                    throw new WebApplicationException("Sie sind nicht Mitglied dieser Band! .........................................", Response.Status.FORBIDDEN);     //401
                }

                connection.setAutoCommit(false);

                preparedStatementBand.setInt(1, bandID);
                preparedStatementBand.setInt(2, Integer.valueOf(felder.get("kuenstlerid").get(0).getValue()));

                preparedStatementBand.executeUpdate();

                connection.commit();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = ui.getAbsolutePathBuilder().path("bands/" + bandID + "/kuenstler").build();
        return Response.created(uri).build();

    }
    // Ordnet einer Band ein Genre zu.

    @RolesAllowed("KUENSTLER")
    @POST
    @Path("/{bandid}/genres")
    @Consumes("multipart/form-data")
    public Response addeGenrezuBand(FormDataMultiPart formData, @PathParam("bandid") int bandID, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        String bereitsvorhanden = "SELECT count(*) FROM Band_hat_Genre WHERE B_ID = (SELECT B_ID FROM Band WHERE ROWID = ?) AND GenreName = (SELECT Name FROM Genre WHERE ROWID = ?)";
        String sqlGenre = "INSERT INTO Band_hat_Genre(B_ID, GenreName) VALUES((SELECT B_ID FROM Band WHERE ROWID = ?), (SELECT Name FROM Genre WHERE ROWID = ?))";
        // Bandmitglied
        String sqlMitgliedBand = "SELECT KuenstlerEmail FROM Band_gehoert_Kuenstler WHERE B_ID = ? AND KuenstlerEmail = ?";
        if (securityContext.isUserInRole("KUENSTLER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Kuenstler WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatementGenre = connection.prepareStatement(sqlGenre);
             PreparedStatement existiert = connection.prepareStatement(bereitsvorhanden);
             PreparedStatement preparedStatementMitglied = connection.prepareStatement(sqlMitgliedBand);
             PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {

            if (securityContext.isUserInRole("KUENSTLER")) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(401);
                }
                preparedStatementMitglied.setInt(1,bandID);
                preparedStatementMitglied.setString(2, authMail);

                ResultSet resultSetM = preparedStatementMitglied.executeQuery();

                if(resultSetM.isClosed()){
                    throw new WebApplicationException("Sie sind nicht Mitglied dieser Band! .........................................",403);
                }

                existiert.setInt(1,bandID);
                existiert.setInt(2, Integer.valueOf(felder.get("genreid").get(0).getValue()));
                ResultSet resultSetE = existiert.executeQuery();
                if(resultSetE.getInt(1) >= 1){
                    throw new SQLException("Diese Genre ist bereits der Band mit der ID: "+ bandID + " zugeordnet");
                }


                preparedStatementGenre.setInt(1, bandID);
                preparedStatementGenre.setInt(2, Integer.valueOf(felder.get("genreid").get(0).getValue()));

                preparedStatementGenre.executeUpdate();

            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = ui.getAbsolutePathBuilder().path("bands/" + bandID + "/genres").build();
        return Response.created(uri).build();

    }

}