package flightapp;

import java.io.IOException;
import java.lang.ref.Cleaner.Cleanable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.text.html.HTMLDocument.HTMLReader.PreAction;

/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;
  
  // clear tables
  private static final String CLEAR_USERS_SQL = "DELETE FROM Users_jlu29";
  private static final String CLEAR_RESERVATIONS_SQL = "DELETE FROM Reservations_jlu29";
  private PreparedStatement clearUsers;
  private PreparedStatement clearReservations;
  
  // create
  private static final String CREATE_SQL = "INSERT INTO Users_jlu29 (username, hashedPassword, balance) VALUES (?, ?, ?)";
  private PreparedStatement create;
  private static final String NAME_EXISTS_SQL = "SELECT * FROM Users_jlu29 WHERE username = ?";
  private PreparedStatement nameExists;
  
  // login
  private static final String GET_PASSWORD_STRING_SQL = "SELECT hashedPassword FROM Users_jlu29 WHERE username = ?";
  private PreparedStatement getPasswordString;
  
  // search
  private static final String SEARCH_DIRECT_SQL = "SELECT TOP ( ? ) fid,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
  + "FROM Flights WHERE origin_city = ? AND dest_city = ? AND day_of_month =  ? AND canceled = 0"
  + "ORDER BY actual_time ASC, fid ASC";
  private PreparedStatement searchDirect;
  private static final String SEARCH_INDIRECT_SQL = "SELECT TOP ( ? ) "
  + "F1.fid AS f1_fid, F1.day_of_month AS f1_day_of_month, F1.carrier_id AS f1_carrier_id, F1.flight_num AS f1_flight_num, "
  + "F1.origin_city AS f1_origin_city, F1.dest_city AS f1_dest_city, F1.actual_time AS f1_actual_time, F1.capacity AS f1_capacity, F1.price AS f1_price, "
  + "F2.fid AS f2_fid, F2.day_of_month AS f2_day_of_month, F2.carrier_id AS f2_carrier_id, F2.flight_num AS f2_flight_num, "
  + "F2.origin_city AS f2_origin_city, F2.dest_city AS f2_dest_city, F2.actual_time AS f2_actual_time, F2.capacity AS f2_capacity, F2.price AS f2_price "
  + "FROM Flights AS F1 JOIN Flights AS F2 ON F1.day_of_month = F2.day_of_month AND F1.dest_city = F2.origin_city "
  + "WHERE F1.origin_city = ? AND F2.dest_city = ? AND F1.day_of_month = ? AND F1.canceled = 0 AND F2.canceled = 0 "
  + "ORDER BY (F1.actual_time + F2.actual_time) ASC, F1.fid ASC, F2.fid ASC";
  private PreparedStatement searchIndirect;
  //private static final String GET_CARRIER_NAME = "SELECT name FROM Carriers WHERE cid = ?";
  //private PreparedStatement getCarrierName;
  
  // book
  private static final String HAVE_SAME_DAY_FLIGHT_SQL = "SELECT * FROM Reservations_jlu29 JOIN Flights ON fid1 = fid WHERE uid = ? AND day_of_month = ?";
  private PreparedStatement haveSameDayFlight;
  private static final String GET_NEXT_RESERVATION_NUM_SQL = "SELECT MAX(rid) FROM Reservations_jlu29";
  private PreparedStatement getNextResNum;
  private static final String RESERVE_SQL = "INSERT INTO Reservations_jlu29 (rid, paid, uid, fid1, fid2) VALUES (?, 0, ?, ?, ?)";
  private PreparedStatement reserve;
  private static final String GET_NUM_FLIGHT_RESERVATIONS_SQL = "SELECT count(*) AS num FROM Reservations_jlu29 WHERE fid1 = ? OR fid2 = ?";
  private PreparedStatement getNumFlightReservations;
  
  //pay
  private static final String GET_BALANCE_SQL = "SELECT balance FROM Users_jlu29 WHERE username = ?";
  private PreparedStatement getBalance; 
  private static final String GET_RESERVATION_INFO_SQL = "SELECT * FROM Reservations_jlu29 WHERE rid = ?";
  private PreparedStatement getResInfo;
  private static final String GET_TOTAL_COST_SQL = "SELECT price FROM Flights WHERE fid = ?";
  private PreparedStatement getTotalCost;
  private static final String UPDATE_PAID_SQL = "UPDATE Reservations_jlu29 SET paid = 1 WHERE rid = ?";
  private PreparedStatement updatePaid;
  private static final String UPDATE_BALANCE_SQL = "UPDATE Users_jlu29 SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalance;
  
  // reservations
  private static final String GET_RESERVATIONS_SQL = "SELECT * FROM Reservations_jlu29 WHERE uid = ?";
  private PreparedStatement getReservations;
  private static final String GET_FLIGHT_INFO_SQL = "SELECT * FROM Flights WHERE fid = ?";
  private PreparedStatement getFlightInfo;


  //
  // Instance variables
  //
  private boolean sessionOngoing = false;
  private String currentUser;
  private List<Itinerary> itineraries = new ArrayList<>();

  protected Query() throws SQLException, IOException {
    prepareStatements();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() throws SQLException {
    try {
      // TODO: YOUR CODE HERE
      clearReservations.executeUpdate();
      clearUsers.executeUpdate();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);
    // TODO: YOUR CODE HERE

    //cleartables
    clearUsers = conn.prepareStatement(CLEAR_USERS_SQL);
    clearReservations = conn.prepareStatement(CLEAR_RESERVATIONS_SQL);
    
    //create
    create = conn.prepareStatement(CREATE_SQL);
    nameExists = conn.prepareStatement(NAME_EXISTS_SQL);
    
    //login
    getPasswordString = conn.prepareStatement(GET_PASSWORD_STRING_SQL);

    //search 
    searchDirect = conn.prepareStatement(SEARCH_DIRECT_SQL);
    searchIndirect = conn.prepareStatement(SEARCH_INDIRECT_SQL);
    //getCarrierName = conn.prepareStatement(GET_CARRIER_NAME);

    //book
    haveSameDayFlight = conn.prepareStatement(HAVE_SAME_DAY_FLIGHT_SQL);
    getNextResNum = conn.prepareStatement(GET_NEXT_RESERVATION_NUM_SQL);
    reserve = conn.prepareStatement(RESERVE_SQL);
    getNumFlightReservations = conn.prepareStatement(GET_NUM_FLIGHT_RESERVATIONS_SQL);

    //pay
    getBalance = conn.prepareStatement(GET_BALANCE_SQL);
    getResInfo = conn.prepareStatement(GET_RESERVATION_INFO_SQL);
    getTotalCost = conn.prepareStatement(GET_TOTAL_COST_SQL);
    updatePaid = conn.prepareStatement(UPDATE_PAID_SQL);
    updateBalance = conn.prepareStatement(UPDATE_BALANCE_SQL);

    //reservations
    getReservations = conn.prepareStatement(GET_RESERVATIONS_SQL);
    getFlightInfo = conn.prepareStatement(GET_FLIGHT_INFO_SQL);
  }

  
  
  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
    // TODO: YOUR CODE HERE
    try {
      if (sessionOngoing) {
        return "User already logged in\n";
      }
      conn.setAutoCommit(false);
      getPasswordString.clearParameters();
      getPasswordString.setString(1, username);
      ResultSet result = getPasswordString.executeQuery();
      if (!result.next()) {
        conn.rollback();
        return "Login failed\n";
      }
      byte[] saltedHashed = result.getBytes("hashedPassword");
      result.close();
      if (PasswordUtils.plaintextMatchesSaltedHash(password, saltedHashed)) {
        sessionOngoing = true;
        currentUser = username;
        conn.commit();
        return "Logged in as " + username + "\n";
      }
      conn.rollback();
      return "Login failed\n";

    } catch (SQLException e) {
      try {
        if (isDeadlock(e)) {
          System.out.println("deadlock");
          return transaction_login(username, password);
        } else {
          conn.rollback();
          e.printStackTrace();
          return "Login failed\n";
        }
      } catch (SQLException rb) {
        rb.printStackTrace();
        return "Login failed\n";
      }
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException ac) {
          ac.printStackTrace();
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE
    try {
      if (initAmount < 0) {
        return "Failed to create user\n";
      }
      conn.setAutoCommit(false);
      nameExists.clearParameters();
      nameExists.setString(1, username);
      ResultSet results = nameExists.executeQuery();
      if (results.next()) {
        conn.rollback();
        results.close();
        return "Failed to create user\n";
      }
      results.close();
      create.clearParameters();
      create.setString(1, username);
      create.setBytes(2, PasswordUtils.saltAndHashPassword(password));
      create.setInt(3, initAmount);
      create.executeUpdate();
      conn.commit();
      return "Created user " + username + "\n";
    } catch (SQLException e) {
      try {
        if (isDeadlock(e)) {
          System.out.println("deadlock");
          return transaction_createCustomer(username, password, initAmount);
        } else {
          conn.rollback();
          e.printStackTrace();
          return "Failed to create user\n";
        }
      } catch (SQLException rb) {
        rb.printStackTrace();
        return "Failed to create user\n";
      }
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException ac) {
          ac.printStackTrace();
      }
    }
  }
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // TODO: YOUR CODE HERE
    itineraries.clear();
    StringBuffer sb = new StringBuffer();
    try {
      searchDirect.clearParameters();
      searchDirect.setInt(1, numberOfItineraries);
      searchDirect.setString(2, originCity);
      searchDirect.setString(3, destinationCity);
      searchDirect.setInt(4, dayOfMonth);
      ResultSet result = searchDirect.executeQuery();
      int count = 0;
      while (result.next()) {
        // create flight object
        Itinerary itin = new Itinerary(new Flight(result));
        itineraries.add(itin);
        count++;
      }
      result.close();
      if (count < numberOfItineraries && !directFlight) {
        searchIndirect.clearParameters();
        searchIndirect.setInt(1, numberOfItineraries - count);
        searchIndirect.setString(2, originCity);
        searchIndirect.setString(3, destinationCity);
        searchIndirect.setInt(4, dayOfMonth);
        ResultSet resultIn = searchIndirect.executeQuery();
        while (resultIn.next()) {
          Itinerary itin = new Itinerary(new Flight(resultIn, "f1_"), new Flight(resultIn, "f2_"));
          itineraries.add(itin);
          count++;
        }
      }
      if (count == 0) {
        return "No flights match your selection\n";
      }

      Collections.sort(itineraries);
      for (int i = 0; i < count; i++) {
        sb.append("Itinerary " + i + ": " + itineraries.get(i).numFlights() + " flight(s), " +  itineraries.get(i).totalDuration + " minutes\n"
          + itineraries.get(i).toString());
      }
      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to search\n";
    } finally {
      if (!sessionOngoing) {
        itineraries.clear();
      }
    }
  }
  /* See QueryAbstract.java for javadoc */
  // public String transaction_search(String originCity, String destinationCity, 
  //                                  boolean directFlight, int dayOfMonth,
  //                                  int numberOfItineraries) {
    // WARNING: the below code is insecure (it's susceptible to SQL injection attacks) AND only
    // handles searches for direct flights.  We are providing it *only* as an example of how
    // to use JDBC; you are required to replace it with your own secure implementation.
    //
    // TODO: YOUR CODE HERE

  //   StringBuffer sb = new StringBuffer();

  //   try {
  //     // one hop itineraries
  //     String unsafeSearchSQL = "SELECT TOP (" + numberOfItineraries
  //       + ") day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
  //       + "FROM Flights " + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'"
  //       + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
  //       + "ORDER BY actual_time ASC";

  //     Statement searchStatement = conn.createStatement();
  //     ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

  //     while (oneHopResults.next()) {
  //       int result_dayOfMonth = oneHopResults.getInt("day_of_month");
  //       String result_carrierId = oneHopResults.getString("carrier_id");
  //       String result_flightNum = oneHopResults.getString("flight_num");
  //       String result_originCity = oneHopResults.getString("origin_city");
  //       String result_destCity = oneHopResults.getString("dest_city");
  //       int result_time = oneHopResults.getInt("actual_time");
  //       int result_capacity = oneHopResults.getInt("capacity");
  //       int result_price = oneHopResults.getInt("price");

  //       sb.append("Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: "
  //                 + result_flightNum + " Origin: " + result_originCity + " Destination: "
  //                 + result_destCity + " Duration: " + result_time + " Capacity: " + result_capacity
  //                 + " Price: " + result_price + "\n");
  //     }
  //     oneHopResults.close();
  //   } catch (SQLException e) {
  //     e.printStackTrace();
  //   }

  //   return sb.toString();
  // }

  /* See QueryAbstract.java for javadoc */
  public String transaction_book(int itineraryId) {
    // TODO: YOUR CODE HERE
    try {
      if (!sessionOngoing) {
        return "Cannot book reservations, not logged in\n";
      }
      if (itineraryId < 0 || itineraryId >= itineraries.size()) {
        return "No such itinerary " + itineraryId + "\n";
      }
      
      conn.setAutoCommit(false);
      Itinerary book = itineraries.get(itineraryId);
      // handle case if there are two flights on the same day
      haveSameDayFlight.clearParameters();
      haveSameDayFlight.setString(1, currentUser);
      haveSameDayFlight.setInt(2, book.f1.dayOfMonth); 
      ResultSet result = haveSameDayFlight.executeQuery();
      if (result.next()) {
        conn.rollback();
        return "You cannot book two flights in the same day\n";
      }

      // check if seats available
      getNumFlightReservations.clearParameters();
      getNumFlightReservations.setInt(1, book.f1.fid);
      getNumFlightReservations.setInt(2, book.f1.fid);
      result = getNumFlightReservations.executeQuery();
      result.next();
      int numFlightReservations = result.getInt("num");
      Boolean canBook = (checkFlightCapacity(book.f1.fid) - numFlightReservations) >= 1;
      if (book.f2 != null) {
        getNumFlightReservations.clearParameters();
        getNumFlightReservations.setInt(1, book.f1.fid);
        getNumFlightReservations.setInt(2, book.f1.fid);
        result = getNumFlightReservations.executeQuery();
        result.next();
        numFlightReservations = result.getInt("num");
        canBook = canBook && (checkFlightCapacity(book.f2.fid) - numFlightReservations >= 1);
      }

      if (!canBook) {
        conn.rollback();
        return "Booking failed\n";
      }
    
      result = getNextResNum.executeQuery();
      int resNum = 0;
      if (result.next()) {
        resNum = result.getInt(1) + 1;
      }
      result.close();

      reserve.setInt(1, resNum);
      reserve.setString(2, currentUser);
      reserve.setInt(3, book.f1.fid);
      if (book.f2 == null) {
        reserve.setNull(4, java.sql.Types.INTEGER);
      } else {
        reserve.setInt(4, book.f2.fid);
      }
      reserve.executeUpdate();
      
      conn.commit();
      return "Booked flight(s), reservation ID: " + resNum + "\n";  
    } catch (SQLException e) {
      try {
        if (isDeadlock(e)) {
          System.out.println("deadlock");
          return transaction_book(itineraryId);
        } else {
          conn.rollback();
          e.printStackTrace();
          return "Booking failed\n";
        }
      } catch (SQLException rb) {
        rb.printStackTrace();
        return "Booking failed\n";
      }
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException ac) {
          ac.printStackTrace();
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE
    try {
      if (!sessionOngoing) {
        return "Cannot pay, not logged in\n";
      }
      
      conn.setAutoCommit(false);
      getResInfo.clearParameters();
      getResInfo.setInt(1, reservationId);
      ResultSet result = getResInfo.executeQuery();
      if (!result.next()) {
        conn.rollback();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + currentUser + "\n";
      }
      int f1 = result.getInt("fid1");
      int f2 = result.getInt("fid2");
      if (result.wasNull()) {
        // set f2 to impossible fid value
        f2 = -1;
      }

      if (result.getInt("paid") == 1 || !result.getString("uid").equals(currentUser)) {
        conn.rollback();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + currentUser + "\n";
      }

      getBalance.clearParameters();
      getBalance.setString(1, currentUser);
      result = getBalance.executeQuery();
      result.next();
      int userBalance = result.getInt("balance");

      getTotalCost.clearParameters();
      getTotalCost.setInt(1, f1);
      result = getTotalCost.executeQuery();
      result.next();
      int totalCost = result.getInt("price");
      if (f2 != -1) {
        getTotalCost.clearParameters();
        getTotalCost.setInt(1, f2);
        result = getTotalCost.executeQuery();
        result.next();
        totalCost += result.getInt("price");
      }
      result.close();

      if (totalCost > userBalance) {
        conn.rollback();
        return "User has only " + userBalance + " in account but itinerary costs " + totalCost + "\n";
      }

      // update reservation paid and user balance
      updatePaid.clearParameters();
      updatePaid.setInt(1, reservationId);
      updatePaid.executeUpdate();
      updateBalance.clearParameters();
      updateBalance.setInt(1, (userBalance - totalCost));
      updateBalance.setString(2, currentUser);
      updateBalance.executeUpdate();   

      conn.commit();
      return "Paid reservation: " + reservationId + " remaining balance: " + (userBalance - totalCost) + "\n";
    } catch (SQLException e) {
      try {
        if (isDeadlock(e)) {
          System.out.println("deadlock");
          return transaction_pay(reservationId);
        } else {
          conn.rollback();
          e.printStackTrace();
          return "Failed to pay for reservation " + reservationId + "\n"; 
        }
      } catch (SQLException rb) {
        rb.printStackTrace();
        return "Failed to pay for reservation " + reservationId + "\n"; 
      }
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException ac) {
          ac.printStackTrace();
      }
    }
  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE
    try {
      
      if (!sessionOngoing) {
        return "Cannot view reservations, not logged in\n";
      }
      StringBuffer sb = new StringBuffer();
      conn.setAutoCommit(false);
      getReservations.clearParameters();
      getReservations.setString(1, currentUser);
      ResultSet result = getReservations.executeQuery();
      if (!result.next()) {
        conn.rollback();
        return "No reservations found\n";
      } 

      int rid = result.getInt("rid");
      int paid = result.getInt("paid");
      int fid1 = result.getInt("fid1");
      int fid2 = result.getInt("fid2");
      if (result.wasNull()) {
        fid2 = -1;
      }
      
      ResultSet res1 = null;
      ResultSet res2 = null;

      getFlightInfo.clearParameters();
      getFlightInfo.setInt(1, fid1);
      res1 = getFlightInfo.executeQuery();
      res1.next();

      Flight f1 = new Flight(res1);
      Flight f2 = null;
      
      sb.append("Reservation " + rid + " paid: " + (paid == 1) + ":\n" 
        + f1.toString() + "\n");

      if (fid2 != -1) {
        getFlightInfo.clearParameters();
        getFlightInfo.setInt(1, fid2);
        res2 = getFlightInfo.executeQuery();
        res2.next();
        f2 = new Flight(res2);
        sb.append(f2.toString() + "\n");
      }

      while (result.next()) {
        rid = result.getInt("rid");
        paid = result.getInt("paid");
        fid1 = result.getInt("fid1");
        fid2 = result.getInt("fid2");
        if (result.wasNull()) {
          fid2 = -1;
        }
        
        getFlightInfo.clearParameters();
        getFlightInfo.setInt(1, fid1);
        res1 = getFlightInfo.executeQuery();
        res1.next();
        f1 = new Flight(res1);
        f2 = null;
        sb.append("Reservation " + rid + " paid: " + (paid == 1) + ":\n" 
          + f1.toString() + "\n");
        if (fid2 != -1) {
          getFlightInfo.clearParameters();
          getFlightInfo.setInt(1, fid2);
          res2 = getFlightInfo.executeQuery();
          res2.next();
          f2 = new Flight(res2);
          sb.append(f2.toString() + "\n");
        }
      }
      result.close();
      res1.close();
      if (res2 != null) {
        res2.close();
      }

      conn.commit();
      return sb.toString();

    } catch (SQLException e) {
      try {
        if (isDeadlock(e)) {
          System.out.println("deadlock");
          return transaction_reservations();
        } else {
          conn.rollback();
          e.printStackTrace();
          return "Failed to retrieve reservations\n";
        }
      } catch (SQLException rb) {
        rb.printStackTrace();
        return "Failed to retrieve reservations\n";
      }
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException ac) {
          ac.printStackTrace();
      }
    }
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }
  

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    Flight(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }
    
    Flight (ResultSet result, String table) throws SQLException {
      this(result.getInt(table + "fid"), result.getInt(table + "day_of_month"), result.getString(table + "carrier_id"),
      result.getString(table + "flight_num"), result.getString(table + "origin_city"), result.getString(table + "dest_city"),
      result.getInt(table + "actual_time"), result.getInt(table + "capacity"), result.getInt(table + "price"));
    }

    Flight (ResultSet result) throws SQLException {
      this(result.getInt("fid"), result.getInt("day_of_month"), result.getString("carrier_id"),
      result.getString("flight_num"), result.getString("origin_city"), result.getString("dest_city"),
      result.getInt("actual_time"), result.getInt("capacity"), result.getInt("price"));
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }
  }

  class Itinerary implements Comparable<Itinerary> {
    private Flight f1;
    private Flight f2;
    private int totalDuration;

    Itinerary(Flight f1, Flight f2) {
      this.f1 = f1;
      this.f2 = f2;
      this.totalDuration = f1.time + f2.time;
    }

    Itinerary(Flight f1) {
      this.f1 = f1;
      this.f2 = null;
      this.totalDuration = f1.time;
    }

    public int numFlights() {
      if (f2 != null) {
        return 2;
      }
      return 1;
    }

    @Override
    public String toString() {
      String res = this.f1.toString() + "\n"; 
      if (f2 != null) {
        return res + f2.toString() + "\n";
      }
      return res;
    }

    public int compareTo(Itinerary other) {
      // TODO Auto-generated method stub
      return this.totalDuration - other.totalDuration;
    }
  }
}
