package de.hhu.cs.dbs.propra.presentation.rest;

import de.hhu.cs.dbs.propra.domain.model.Ticket;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class TicketController {
    @Inject
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /*
     * Anzeigen aller eigenen Tickets
     */
    @RolesAllowed({"BESUCHER"})
    @Path("/tickets")
    @GET
    public Response getTickets(@Context UriInfo ui,  @Context SecurityContext securityContext) {
        ArrayList<Ticket> tickets = new ArrayList<>();
        int parameterCounter = ui.getQueryParameters().size();
        String authMail = securityContext.getUserPrincipal().getName();

        String sql = "SELECT * FROM Ticket WHERE Besucher_Email LIKE ?";


        if (parameterCounter != 0) {
            sql += " AND ";
        }
        if (ui.getQueryParameters().containsKey("f_bezeichnung")) {
            sql += "Festival_ID = (SELECT F_ID FROM Festival WHERE Name LIKE ?)";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }
        if (ui.getQueryParameters().containsKey("vip")) {
            sql += "VIP_Vermerk = ?";
            if (--parameterCounter != 0) {
                sql += " AND ";
            }
        }

        if (ui.getQueryParameters().containsKey("preis")) {
            sql += "Preis >= ?";
        }

        try (Connection connection = this.dataSource.getConnection()) {

            if (securityContext.isUserInRole("BESUCHER")) {

                String authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT Besucher_Email FROM Ticket WHERE Besucher_Email = ? )";
                PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL);
                authorizationStatement.setString(1, authMail);

                ResultSet authRs = authorizationStatement.executeQuery();
                if (authRs.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            int bindValueCounter = 1;

            preparedStatement.setString(bindValueCounter++,authMail);

            if (ui.getQueryParameters().containsKey("f_bezeichnung")) {
                preparedStatement.setString(bindValueCounter++, "%" + ui.getQueryParameters().getFirst("f_bezeichnung") + "%");
            }

            if (ui.getQueryParameters().containsKey("vip")) {
                preparedStatement.setBoolean(bindValueCounter++, Boolean.valueOf(ui.getQueryParameters().getFirst("vip")));
            }

            if (ui.getQueryParameters().containsKey("preis")) {
                preparedStatement.setDouble(bindValueCounter++, Double.valueOf(ui.getQueryParameters().getFirst("preis")));
            }


            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                tickets.add(new Ticket(resultSet.getInt("Ticket_ID"),
                        resultSet.getString("datum"),
                        resultSet.getDouble("preis"),
                        resultSet.getBoolean("VIP_Vermerk")));
            }


        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        return Response.status(200).entity(tickets).build();
    }
    @RolesAllowed("BESUCHER")
    @DELETE
    @Path("/tickets/{ticketid}")
    @Consumes("multipart/form-data")
    public Response deleteTicket(@PathParam("ticketid") int ticketid, @Context SecurityContext securityContext) {
        String authMail = securityContext.getUserPrincipal().getName();
        String authorizationSQL = null;
        String sql = "DELETE FROM Ticket WHERE ROWID = ?";

        if (securityContext.isUserInRole("BESUCHER")) {
            authorizationSQL = "SELECT count(Email) FROM User WHERE Email IN (SELECT Besucher_Email FROM Ticket WHERE Besucher_Email = ? AND Ticket_ID = ? )";
        }

        try (Connection connection = this.dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {


            if (securityContext.isUserInRole("BESUCHER")) {
                PreparedStatement authorizationStatement = connection.prepareStatement(authorizationSQL);
                authorizationStatement.setString(1, authMail);
                authorizationStatement.setInt(2, ticketid);
                authorizationStatement.closeOnCompletion();
                ResultSet resultSet = authorizationStatement.executeQuery();

                if (resultSet.getInt(1) == 0) {
                    throw new WebApplicationException(403);
                }
            }

            preparedStatement.setInt(1, ticketid);
            int deleted = preparedStatement.executeUpdate();

            if (deleted == 0) {
                throw new WebApplicationException(404);
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        return Response.status(204).build();
    }



}