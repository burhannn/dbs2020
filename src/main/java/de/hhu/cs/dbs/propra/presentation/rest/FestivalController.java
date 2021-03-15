package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Buehne;
import de.hhu.cs.dbs.propra.domain.model.Festival;
import de.hhu.cs.dbs.propra.domain.model.Programmpunkt;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.annotation.security.RolesAllowed;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;

@Path("/festivals")
@Produces(MediaType.APPLICATION_JSON)
public class FestivalController {
    @Inject
    private DataSource dataSource;

    @Context
    private SecurityContext securityContext;


    /*
    *
    * Anzeigen aller Fetivals
    *
    *
    */
    @GET
    public Response getFestivals(@Context UriInfo ui) {
        ArrayList<Festival> festivals = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        String sql = "SELECT * FROM Festival";

        if (parameterCounter != 0) {
            sql += " WHERE ";
        }

        if (ui.getQueryParameters().containsKey("bezeichnung")) {
            sql += "Name like ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("jahr")) {
            sql += "strftime('%Y', datum) = ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("ort")) {
            sql += "Ortname like ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }
        if (ui.getQueryParameters().containsKey("veranstalter")) {
                sql += "F_ID = (SELECT F_ID FROM Veranstalter_organisiert_Fest WHERE VeranstalterEmail LIKE ?)";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            if (ui.getQueryParameters().containsKey("bezeichnung")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("bezeichnung") + "%");
            }

            if (ui.getQueryParameters().containsKey("jahr")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("jahr"));
            }

            if (ui.getQueryParameters().containsKey("ort")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("ort"));
            }
            if (ui.getQueryParameters().containsKey("veranstalter")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("veranstalter") + "%");
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            while (resultSet.next()){
                festivals.add(new Festival(resultSet.getInt("F_ID"),
                        resultSet.getString("name"),
                        resultSet.getBytes("bild"),
                        resultSet.getString("datum")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(festivals).build();
    }

    /*
     *
     *
      Anzeigen der Bühnen eines spezifischen Fetivals
     *
    */
    @GET
    @Path("/{F_ID}/buehnen")
    public Response getFestBuehnenByID(@PathParam("F_ID") int festid, @Context UriInfo ui) throws IOException {

        String sql = "SELECT * FROM Buehne WHERE F_ID = (SELECT F_ID FROM Festival WHERE ROWID = ?)";
        ArrayList<Buehne> buehnen = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        if (parameterCounter != 0) {
            sql += " AND ";
        }

        if (ui.getQueryParameters().containsKey("bezeichnung")) {
            sql += "Name like ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }
        if (ui.getQueryParameters().containsKey("sitzplaetze")) {
            sql += "Sitzplatzanzahl >= ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }
        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            preparedStatement.setInt(bindValueCounter++,festid);
            if (ui.getQueryParameters().containsKey("bezeichnung")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("bezeichnung") + "%");
            }

            if (ui.getQueryParameters().containsKey("sitzplaetze")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("sitzplaetze"));
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            while (resultSet.next()){
                buehnen.add(new Buehne(resultSet.getString("name"),
                        resultSet.getInt("sitzplatzanzahl"),
                        resultSet.getInt("stehplatzanzahl")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(buehnen).build();
    }

    /*
     *
     *
      Anzeigen der Programmpunkte eines spezifischen Fetivals
     *
    */
    @GET
    @Path("/{F_ID}/buehnen/{buehneid}/programmpunkte")
    public Response getProgrammpunkteBuehnenByID(@PathParam("F_ID") int festid,@PathParam("buehneid") int buehneid, @Context UriInfo ui) throws IOException {

        String sql = "SELECT * FROM Programmpunkt WHERE Buehne_name = (SELECT Name FROM Buehne WHERE F_ID = (SELECT F_ID FROM Festival WHERE ROWID = ?))"
                    + " AND Buehne_name = (SELECT Name FROM Buehne WHERE ROWID = ?)";


        ArrayList<Programmpunkt> programmpunkte = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();

        if (parameterCounter != 0) {
            sql += " AND ";
        }

        if (ui.getQueryParameters().containsKey("bandname")) {
            sql += "B_ID = (SELECT B_ID FROM Band WHERE Name LIKE ?)";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("dauer")) {
            sql += "Dauer >= ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            int bindValueCounter = 1;

            preparedStatement.setInt(bindValueCounter++, festid);
            preparedStatement.setInt(bindValueCounter++, buehneid);

            if (ui.getQueryParameters().containsKey("bandname")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("bandname") + "%");
            }

            if (ui.getQueryParameters().containsKey("dauer")) {
                preparedStatement.setString(bindValueCounter++, ui.getQueryParameters().getFirst("dauer"));
            }


            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.isClosed()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            while (resultSet.next()){
                programmpunkte.add(new Programmpunkt(resultSet.getInt("P_ID"),
                        resultSet.getString("Uhrzeit"),
                        resultSet.getInt("dauer")));
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(programmpunkte).build();
    }
    // Anzeigen eines spezifischen Fetivals bzw des Bildes
    @GET
    @Path("/{F_ID}")
    @Produces("image/png")
    public Response getFestByID(@PathParam("F_ID") int festid) throws IOException {

        Festival festival;
        String sql = "SELECT * FROM Festival WHERE ROWID = ?";
        byte[] imageData;

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, festid);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                throw new WebApplicationException(404);
            }
            BufferedImage image;
            image = new BufferedImage(16, 16, TYPE_BYTE_BINARY);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            imageData = resultSet.getBytes("bild");
            festival = new Festival(resultSet.getInt("F_ID"), resultSet.getString("name"), resultSet.getBytes("bild"),
                    resultSet.getString("datum"));

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(festival).build().ok(imageData).build();
    }

    @RolesAllowed("BESUCHER")
    @POST
    @Path("/{festivalid}/tickets")
    @Consumes("multipart/form-data")
    public Response kaufeTicket(@PathParam("festivalid") int festid, @FormDataParam("datum") String datum, @FormDataParam("preis") int preis, @FormDataParam("vip") boolean vip, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        //  Ticket_id,Datum,Preis,VIP_Vermerk,Besucher_Email,Festival_ID
        String sql = "INSERT INTO Ticket(Datum, Preis, VIP_Vermerk, Besucher_Email, Festival_ID) VALUES(?, ?, ?, ?, (SELECT F_ID FROM Festival WHERE ROWID = ?))";
        int rowid ;
        if (securityContext.isUserInRole("BESUCHER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT Besucher_Email FROM Ticket WHERE Besucher_Email = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            if (securityContext.isUserInRole("BESUCHER")) {
                PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL);
                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if(resultSet.isClosed()){
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
            }

            preparedStatement.setString(1, datum);
            preparedStatement.setDouble(2, preis);
            preparedStatement.setBoolean(3, vip);

            preparedStatement.setString(4, authMail);
            preparedStatement.setInt(5, festid);

            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            rowid = generatedKeys.getInt(1);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        URI uri = ui.getAbsolutePathBuilder().path("festivals/" + rowid + "/tickets").build();

        return Response.created(uri).build();

    }
    @RolesAllowed("VERANSTALTER")
    @POST
    @Consumes("multipart/form-data")
    public Response addeFest(FormDataMultiPart formData, @FormDataParam("bild") byte[] bild, @Context UriInfo ui, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        int rowid;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        int festid;
        //  Festival
        String sqlIDvonFestival = "SELECT MAX(F_ID) FROM Festival";
        String sqlFestival = "INSERT INTO Festival(F_ID, Name, Bild, Datum, Ortname) VALUES(?, ?, ?, ?, (SELECT Name FROM Ort WHERE ROWID = ?))";
        // Veranstalter_organisiert_Fest
        String sqlVeranstalterOrga = "INSERT INTO Veranstalter_organisiert_Fest(VeranstalterEmail, F_ID) VALUES(?, ?)";
        if (securityContext.isUserInRole("VERANSTALTER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Veranstalter WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatementIDFest = connection.prepareStatement(sqlIDvonFestival);
             PreparedStatement preparedStatementFest = connection.prepareStatement(sqlFestival);
             PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {

            ResultSet res = preparedStatementIDFest.executeQuery();
            festid = res.getInt(1) + 1;      //Leider hardgecodet
            if (securityContext.isUserInRole("VERANSTALTER")) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
                connection.setAutoCommit(false);

                preparedStatementFest.setInt(1, festid);
                preparedStatementFest.setString(2, felder.get("bezeichnung").get(0).getValue());
                preparedStatementFest.setBytes(3, bild);
                preparedStatementFest.setString(4, felder.get("datum").get(0).getValue());
                preparedStatementFest.setInt(5, Integer.valueOf(felder.get("ortid").get(0).getValue()));

                preparedStatementFest.executeUpdate();

                PreparedStatement preparedStatementOrga = connection.prepareStatement(sqlVeranstalterOrga);
                preparedStatementOrga.setString(1, authMail);
                preparedStatementOrga.setInt(2, festid); // setzen der Festival ID

                preparedStatementOrga.executeUpdate();
                //ResultSet generatedKeys = preparedStatementFest.getGeneratedKeys();
                //rowid = generatedKeys.getInt(1);

                connection.commit();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = ui.getAbsolutePathBuilder().path("festivals/").build();
        return Response.created(uri).build();

    }
    // Bühnen dem Festival hinzufügen
    @RolesAllowed("VERANSTALTER")
    @POST
    @Path("/{F_ID}/buehnen")
    @Consumes("multipart/form-data")
    public Response addeFestBuehnenByID(FormDataMultiPart formData, @PathParam("F_ID") int festid, @Context UriInfo ui, @Context SecurityContext securityContext) {

        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        String sqlBuehne = "INSERT INTO Buehne(Name, Sitzplatzanzahl, Stehplatzanzahl, F_ID) VALUES(?, ?, ?, (SELECT F_ID FROM Festival WHERE ROWID = ?))";
        // Veranstalter_organisiert_Fest
        String sqlVeranstalterOrga = "SELECT VeranstalterEmail FROM Veranstalter_organisiert_Fest WHERE F_ID = ? AND VeranstalterEmail = ? ";
        if (securityContext.isUserInRole("VERANSTALTER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Veranstalter WHERE UserEmail = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlBuehne);
             PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {

            if (securityContext.isUserInRole("VERANSTALTER")) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSetA = authorizationStatement.executeQuery();


                if (resultSetA.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
                connection.setAutoCommit(false);
                PreparedStatement preparedStatementOrga = connection.prepareStatement(sqlVeranstalterOrga);
                preparedStatementOrga.setInt(1, festid);
                preparedStatementOrga.setString(2,authMail);

                ResultSet resultSetB = preparedStatementOrga.executeQuery();

                //if(resultSetB.getString("VeranstalterEmail") != authMail){
                if(resultSetB.isClosed()){ // Wenn Kein Ergebnis aus der Abfrage
                    throw new SQLException("Sie sind nicht Organisator des Festivals ! ..............");

                }

                preparedStatement.setString(1, felder.get("bezeichnung").get(0).getValue());
                preparedStatement.setInt(2, Integer.valueOf(felder.get("sitzplaetze").get(0).getValue()));
                preparedStatement.setInt(3, Integer.valueOf(felder.get("stehplaetze").get(0).getValue()));
                preparedStatement.setInt(4, festid);

                preparedStatement.executeUpdate();

                connection.commit();
            }

        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        URI uri = ui.getAbsolutePathBuilder().path("festivals/"+ festid + "/buehnen").build();
        return Response.created(uri).build();

    }
    // Einer Bühne eines Festivals einen Programmpunkt hinzufügen
    @RolesAllowed("VERANSTALTER")
    @POST
    @Path("/{F_ID}/buehnen/{buehneid}/programmpunkte")
    @Consumes("multipart/form-data")
    public Response addeProgrammpunkte(FormDataMultiPart formData, @PathParam("F_ID") int festid, @PathParam("buehneid") int buehneid, @Context UriInfo ui, @Context SecurityContext securityContext) {

        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        int p_id;
        Map<String, List<FormDataBodyPart>> felder = formData.getFields();
        String sqlProgrammpunkt = "INSERT INTO Programmpunkt(P_ID, Uhrzeit, Dauer, Buehne_name, B_ID) VALUES(?, ?, ?, (SELECT Name FROM Buehne WHERE ROWID = ?), (SELECT B_ID FROM Band WHERE ROWID = ?))";
        // Veranstalter_organisiert_Fest
        String sqlVeranstalterOrga = "SELECT VeranstalterEmail FROM Veranstalter_organisiert_Fest WHERE F_ID = ? AND VeranstalterEmail = ? ";
        String sqlP_ID = "SELECT MAX(P_ID) FROM Programmpunkt";
        if (securityContext.isUserInRole("VERANSTALTER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Veranstalter WHERE UserEmail = ? )";


            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sqlProgrammpunkt);
                 PreparedStatement preparedStatementPID = connection.prepareStatement(sqlP_ID);
                 PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL)) {


                ResultSet res = preparedStatementPID.executeQuery();
                p_id = res.getInt(1) + 1;      //Leider hardgecodet

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSetA = authorizationStatement.executeQuery();

                if (resultSetA.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
                connection.setAutoCommit(false);
                PreparedStatement preparedStatementOrga = connection.prepareStatement(sqlVeranstalterOrga);
                preparedStatementOrga.setInt(1, festid);
                preparedStatementOrga.setString(2, authMail);

                ResultSet resultSetB = preparedStatementOrga.executeQuery();

                //if(resultSetB.getString("VeranstalterEmail") != authMail){
                if (resultSetB.isClosed()) { // Wenn Kein Ergebnis aus der Abfrage
                    throw new SQLException("Sie sind nicht Organisator des Festivals ! ..............");

                }

                preparedStatement.setInt(1,p_id);
                preparedStatement.setString(2, felder.get("startzeitpunkt").get(0).getValue());
                preparedStatement.setInt(3, Integer.valueOf(felder.get("dauer").get(0).getValue()));
                preparedStatement.setInt(4, buehneid);
                preparedStatement.setInt(5, Integer.valueOf(felder.get("bandid").get(0).getValue()));

                preparedStatement.executeUpdate();

                connection.commit();


            } catch (SQLException e) {
                throw new WebApplicationException(e.getMessage(), 400);
            }
        }
        URI uri = ui.getAbsolutePathBuilder().path("festivals/"+ festid + "/buehnen/" + buehneid + "/programmpunkte").build();
        return Response.created(uri).build();

    }
    // Eigenes Festival ändern
    @RolesAllowed("VERANSTALTER")
    @PATCH
    @Path("/{F_ID}")
    @Consumes("multipart/form-data")
    public Response updateFestival(@PathParam("F_ID") int festid, @FormDataParam("bezeichnung") String bezeichnung, @FormDataParam("datum") String datum, @FormDataParam("bild") byte[] bild) {

        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        String sqlVeranstalterOrga = "SELECT VeranstalterEmail FROM Veranstalter_organisiert_Fest WHERE F_ID = ? AND VeranstalterEmail = ? ";
        ArrayList<String> formParams = new ArrayList<>();
        if (bezeichnung != null) {
            formParams.add("name");
        }
        if (datum != null) {
            formParams.add("datum");
        }
        if (bild != null) {
            formParams.add("bild");
        }

        String sql = "UPDATE Festival SET ";


        for (int i = 0; i < formParams.size(); i++) {
            sql += formParams.get(i) + " = ? ";
            if (i < formParams.size() - 1) {
                sql += ", ";
            }
        }

        sql += " WHERE ROWID = ?";

        if (securityContext.isUserInRole("VERANSTALTER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT UserEmail FROM Veranstalter WHERE UserEmail = ? )";

            try (Connection connection = this.dataSource.getConnection();
                 PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL);
                 PreparedStatement preparedStatementOrga = connection.prepareStatement(sqlVeranstalterOrga);
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                authorizationStatement.setString(1, authMail);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSetA = authorizationStatement.executeQuery();

                if (resultSetA.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
                preparedStatementOrga.setInt(1, festid);
                preparedStatementOrga.setString(2, authMail);

                ResultSet resultSetB = preparedStatementOrga.executeQuery();

                if (resultSetB.isClosed()) { // Wenn Kein Ergebnis aus der Abfrage
                    throw new SQLException("Sie sind nicht Organisator des Festivals ! .......................");

                }
                int bindValueCounter = 1;

                if (bezeichnung != null) {
                    preparedStatement.setString(bindValueCounter++, bezeichnung);
                }
                if (datum != null) {
                    preparedStatement.setString(bindValueCounter++, datum);
                }
                if (bild != null) {
                    preparedStatement.setBytes(bindValueCounter++, bild);
                }

                preparedStatement.setInt(bindValueCounter++, festid);
                int updated = preparedStatement.executeUpdate();

                if (updated == 0) {
                    throw new WebApplicationException(404);
                }

            } catch (SQLException e) {
                throw new WebApplicationException(e.getMessage(), 400);
            }
        }
        return Response.status(204).build();
    }
}