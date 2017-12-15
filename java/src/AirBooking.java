/*
 * Template JAVA User Interface
 * =============================
 *
 * Database Management Systems
 * Department of Computer Science &amp; Engineering
 * University of California - Riverside
 *
 * Target DBMS: 'Postgres'
 *
 */


import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.text.NumberFormat;
import java.util.Vector;
/**
 * This class defines a simple embedded SQL utility class that is designed to
 * work with PostgreSQL JDBC drivers.
 *
 */

public class AirBooking extends JFrame{

	/*---------------Class Variables---------------*/

	//reference to physical database connection
	private Connection _connection = null;
	static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private JTabbedPane tabbedPanel = new JTabbedPane();
	private List<JPanel> PanelList = new ArrayList<JPanel>();


	/*---------------Functions---------------*/
	
	//------Error Handling Functions------//
	//Use this to show the user that their input was incorrect
	private static void displayPopUp(String message, String title)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
	}
	//Checks if a given string has all letters inside. Works for multiple words)
	private boolean isAlpha(String text)
	{
		return text.matches("[a-zA-Z\\s\\.]+");
	}

	//Returns the number of passengers that exist in the passenger database.	
	private int getNumberOfPassengers() 
	{
		try
		{
			String seqQuery = "SELECT Count(fullName) FROM Passenger";
			List<List<String>> countResult = executeQueryAndReturnResult(seqQuery);			
			return Integer.valueOf(countResult.get(0).get(0));
		}
		catch(SQLException ex)
		{	
			
			System.out.println(ex.getMessage());
			System.out.println(ex.getErrorCode());
			return -1;
		}
	}

	//Returns the number of ratings that exist in the rating database.	
	private int getNumberOfRatings() 
	{
		try
		{
			String seqQuery = "SELECT Count(rID) FROM Ratings";
			List<List<String>> countResult = executeQueryAndReturnResult(seqQuery);			
			return Integer.valueOf(countResult.get(0).get(0));
		}
		catch(SQLException ex)
		{	
			
			System.out.println(ex.getMessage());
			System.out.println(ex.getErrorCode());
			return -1;
		}
	}

	//------Component Making Functions------//
	//Creates a JSpinner that only allows numeric input. You can set the min and max of the spinner
	private JSpinner createDigitSpinner(int defaultVal, int min, int max) 
	{
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultVal,min,max,1));
		//Dimension d = spinner.getPreferredSize();
		//d.width = 90;
		//spinner.setPreferredSize(d);
		JSpinner.NumberEditor jsEditor = (JSpinner.NumberEditor) spinner.getEditor();
		jsEditor.getFormat().setGroupingUsed(false); //To prevent 2018 becoming 2,018
		final Document jsDoc = jsEditor.getTextField().getDocument();
		if (jsDoc instanceof PlainDocument)
		{
			AbstractDocument doc = new PlainDocument()
			{
				private static final long serialVersionUID = 1L;
				@Override
				public void setDocumentFilter(DocumentFilter filter)
				{
					if (filter instanceof MyDocumentFilter)
					{
						super.setDocumentFilter(filter);
					}
				}
			};
			doc.setDocumentFilter(new MyDocumentFilter());
			jsEditor.getTextField().setDocument(doc);

		}
		return spinner;
	} 
	//Creates a filter so that the spinner will only accept numeric input from user. 
	private static class MyDocumentFilter extends DocumentFilter
	{
		@Override
		public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
		    if (stringContainsOnlyDigits(string)) {
			super.insertString(fb, offset, string, attr);
		    }
		}

		@Override
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
		    super.remove(fb, offset, length);
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
		    if (stringContainsOnlyDigits(text)) {
			super.replace(fb, offset, length, text, attrs);
		    }
		}

		private boolean stringContainsOnlyDigits(String text) {
		    for (int i = 0; i < text.length(); i++) {
			if (!Character.isDigit(text.charAt(i))) {
				displayPopUp("Please only use numeric input", "Invalid Input");
				return false;
			}
		    }
		    return true;
		}
	}
	//Pulls data based on query and returns a Jtable
	public JTable createDBTable(String query) throws SQLException 
	{
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		/*
		 *  obtains the metadata object for the returned result set.  The metadata
		 *  contains row and column info.
		 */
		ResultSetMetaData rsmd = rs.getMetaData ();
		int numCol = rsmd.getColumnCount ();
		int rowCount = 0;
		
		//Have to use Vector because JTable don't allow List
		Vector<Vector<String>> result  = new Vector<Vector<String>>(); 
		Vector<String> columnHeader = new Vector<String>();
		
		//iterates through the result set and output them to standard out.
		boolean outputHeader = true;
		while (rs.next()){
			if(outputHeader){
				for(int i = 1; i <= numCol; i++)
					columnHeader.add(rsmd.getColumnName(i));
			    outputHeader = false;
			}
			Vector<String> record = new Vector<String>(); 
			for (int i=1; i<=numCol; ++i) 
				record.add(rs.getString (i)); 
			result.add(record); 
			++rowCount;
		}//end while
		stmt.close ();
		
		//Create the Table
		DefaultTableModel dTableModel = new DefaultTableModel(result, columnHeader){
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false; //Prevents the user from editting the cell
			}
		};

		JTable table = new JTable(dTableModel);
		table.setAutoCreateRowSorter(true);
		return table;
	}
	
	//------Create Panel Functions------//
	/* 1) Add Passenger */
	private void createPanel1()
	{
		int panelIndex = 0;
		//Constant to define the exact length a passport ID needs to be.	
		final int PASSPORT_ID_LENGTH = 10;
		//This is where you add all the components you need for that specific panel
		//Create components
		
		JScrollPane queryDBTable = new JScrollPane();
		
		//General information components
		JLabel firstNameTxt = new JLabel("First Name");
		JLabel lastNameTxt = new JLabel("Last Name");
		JLabel passportTxt = new JLabel("Passport ID");
		JLabel countryTxt = new JLabel("Country");

		JTextField firstNameField = new JTextField(20);
		JTextField lastNameField = new JTextField(20);
		JTextField countryField = new JTextField(20);

		JTextField passportField = new JTextField(20);
		//Limit number of characters of passport field to 1	
		passportField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				if(passportField.getText().length() >= PASSPORT_ID_LENGTH)
					e.consume();
			}
		});
	
		//Date of birth components
		JLabel monthTxt = new JLabel("Month: ");
		JLabel dateTxt = new JLabel("Date: ");
		JLabel yearTxt = new JLabel("Year: ");

		JSpinner monthNum = createDigitSpinner(2, 1, 12);
		JSpinner dateNum = createDigitSpinner(2, 1, 31);
		JSpinner yearNum = createDigitSpinner(1900, 1900, 9999);
		monthNum.setValue(1);	
		dateNum.setValue(1);
		yearNum.setValue(2017);



		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					String name = firstNameField.getText() + " " + lastNameField.getText();
					String passportId = passportField.getText();
					String country = countryField.getText();	
				        String birthdate = String.valueOf(monthNum.getValue()) + '/';
					birthdate +=  String.valueOf(dateNum.getValue()) + '/';
					birthdate += String.valueOf(yearNum.getValue());
					
					if(firstNameField.getText().isEmpty())
					{
						displayPopUp("Please enter a first name.", "Input Error");
						return;
					}
					else if(lastNameField.getText().isEmpty())
					{
						displayPopUp("Please enter a last name.", "Input Error");
						return;
					}
					else if(country.isEmpty())
					{
						displayPopUp("Please enter a country.", "Input Error");
						return;
					}
					else if(passportId.isEmpty())
					{
						displayPopUp("Please enter a passport ID.", "Input Error");
						return;
					}		
					else if(!isAlpha(name))
					{
						displayPopUp("Names must contain only letters.", "Input Error");
						return;
					}
					else if(!isAlpha(country))
					{
						displayPopUp("Countries must contain only letters.", "Input Error");
						return;
					}
					else if(!isAlpha(passportId))
					{
						displayPopUp("Passport IDs must contain only letters.", "Input Error");
						return;
					}
					else if(passportId.length() != PASSPORT_ID_LENGTH)
					{

						displayPopUp("Passport ID must be " + String.valueOf(PASSPORT_ID_LENGTH) + " characters long. ", "Input Error");
						return;
					}
					//Execute the Query and Make Table
					//Replace the string with your SQL Query
					String query = "INSERT INTO Passenger (passNum, fullName, bdate, country) ";
					query += "VALUES ( ";
					query += "\'" + passportId + "\'" + ", ";
					query += "\'" + name + "\'" + ", ";
					query += "\'" + birthdate + "\'" + ", ";
					query += "\'" + country + "\'" + ")";
					//Get initial number of entries in Passengers
					String passengerSeq = "pIDseq";
					int latestOldID = getNumberOfPassengers();
					if (latestOldID == -1)
					{
						displayPopUp("There was an error in adding the passenger. Please try again. Error Code: 0", "Error");
					}
					//Insert passenger info onto database
					executeUpdate(query);	
					//Check if passenger is inside the database
					int latestNewID = getNumberOfPassengers();
				        if (latestNewID > latestOldID)	
					{
						displayPopUp("Passenger added!", "Added Passenger");
					}
					else
					{
						displayPopUp("There was an error in adding the passenger. Please try again. Error Code:1", "Error");
					}

					refreshPanel(panelIndex);
					/*//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(query)); 
					PanelList.get(panelIndex).add(queryDBTable, BorderLayout.CENTER);

					//Refresh the frame to show the added table
					validate(); 
					repaint();*/ 

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
					System.out.println(ex.getErrorCode());
					if (ex.getErrorCode() == 0)
					{
						displayPopUp("Passenger already exists!", "Error");
					}
				}
			}
		});
		//Add components
		PanelList.get(panelIndex).add(firstNameTxt);
		PanelList.get(panelIndex).add(firstNameField);
		PanelList.get(panelIndex).add(lastNameTxt);
		PanelList.get(panelIndex).add(lastNameField);
		PanelList.get(panelIndex).add(passportTxt);
		PanelList.get(panelIndex).add(passportField);
		PanelList.get(panelIndex).add(monthTxt);
		PanelList.get(panelIndex).add(monthNum);
		PanelList.get(panelIndex).add(dateTxt);
		PanelList.get(panelIndex).add(dateNum);
		PanelList.get(panelIndex).add(yearTxt);
		PanelList.get(panelIndex).add(yearNum);
		PanelList.get(panelIndex).add(countryTxt);
		PanelList.get(panelIndex).add(countryField);

		PanelList.get(panelIndex).add(submitButt);
			
	}

	/* 2) Book Flight */
	private void createPanel2()
	{
		int panelIndex = 1;
		int numPassengers = getNumberOfPassengers() - 1;
		//Create components


		JLabel passTxt = new JLabel("Passenger ID: ");
		JSpinner passInput = createDigitSpinner(1,0, numPassengers);
		passInput.setValue(0);

		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table

		JLabel originTxt = new JLabel("Origin: ");
		JLabel destTxt = new JLabel("Destination: ");
		JTextField originInput = new JTextField("", 15);
		JTextField destInput = new JTextField("", 15);

		JLabel monthTxt = new JLabel("Month: ");
		JLabel dateTxt = new JLabel("Date: ");
		JLabel yearTxt = new JLabel("Year: ");


		JSpinner monthNum = createDigitSpinner(2, 1, 12);
		JSpinner dateNum = createDigitSpinner(2, 1, 31);
		JSpinner yearNum = createDigitSpinner(1900, 1900, 9999);

		monthNum.setValue(1);	
		dateNum.setValue(1);
		yearNum.setValue(2017);

		JButton submitButt = new JButton("Book Flight");
		JButton searchButt = new JButton("Search for Available Flights");
		searchButt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					
					String originIn = originInput.getText();
					String destIn = destInput.getText();
					if (originIn.length() == 0)
					{
						displayPopUp("Please enter an origin.", "Invalid Input");
						return;
					}
					if (destIn.length() == 0)
					{
						displayPopUp("Please enter a destination.", "Invalid Input");
						return;
					}
					if (!isAlpha(originIn) || !isAlpha(destIn))
					{
						displayPopUp("Location can only use letters.", "Invalid Input");
						return;
					}

					String departIn = monthNum.getValue() + "/" + dateNum.getValue() + "/" + yearNum.getValue();
					//Find if that number is less than the flight seat's, its available
					String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination,";
					flightAvailQuery += " COALESCE(a.departure, date\'" + departIn +  "\') AS departure, ";
					flightAvailQuery += "COALESCE(a.booked, 0) AS booked, Flight.seats, ";
					flightAvailQuery += "COALESCE(Flight.seats - a.booked, Flight.seats) AS available ";
					flightAvailQuery += "FROM Flight ";
					flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, Booking.departure, COUNT(Booking.flightNum) as booked ";
					flightAvailQuery += "FROM Booking WHERE Booking.departure = \'" + departIn + "\' ";
					flightAvailQuery += "GROUP BY Booking.flightNum, Booking.departure) AS a ";
					flightAvailQuery += "ON a.flightNum = Flight.flightNum ";
					flightAvailQuery += "WHERE Flight.origin = \'" + originIn;
					flightAvailQuery += "\' AND Flight.destination = \'" + destIn + "\'";

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(flightAvailQuery)); 
					PanelList.get(panelIndex).add(queryDBTable);
					PanelList.get(panelIndex).add(submitButt);
					//Refresh the frame to show the added table
					validate(); 
					repaint(); 

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
				}

			}
		});
		submitButt.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					
					String passQuery = "SELECT pID, fullName FROM Passenger WHERE pID =" + passInput.getValue();
					List<List<String>> passInfo = executeQueryAndReturnResult(passQuery);
					if (passInfo.size() == 0)
					{
						displayPopUp("That id isn't in our system.", "Error");
						return;
					}

					//Input into the Booking database
					//Booking number is 5 chars, 4 nums, 1 char
					//Create random booking number:
					Random r = new Random();
					String bookingString = "";
					int count = 0;
					while (count < 5)
					{
						bookingString += (char) (r.nextInt(26) + 'A');
						count++;
					}
					count = 0;
					while (count < 4)
					{
						bookingString += r.nextInt(9);
						count++;
					}
					bookingString += (char) (r.nextInt(26) + 'A');

					JViewport viewport = queryDBTable.getViewport();
					JTable flightTable = (JTable)viewport.getView();
					int row = flightTable.getSelectedRow();
					if (row == -1)
					{
						displayPopUp("Please select a flight to book", "Error");
						return;
					}
					String flightNum = flightTable.getModel().getValueAt(row, 0).toString();
					String departDate = flightTable.getModel().getValueAt(row, 3).toString();
					//Insert into Booking table
					//bookRef, depart, flightNum, pid
					//allAvailFlights indexes are:
					//0-flightNum, 1-origin, 2-dest, 3-depart, 4-booked, 5-total, 6-free 
					String insertQuery = "INSERT into Booking VALUES (\'";
					insertQuery += bookingString + "\', date \'";
					insertQuery += departDate + "\', \'";
					insertQuery += flightNum + "\', ";
					insertQuery += passInfo.get(0).get(0) + ")";
					
					executeUpdate(insertQuery);
					
					String checkInsert = "SELECT bookRef FROM Booking WHERE bookRef = ";
					checkInsert += "\'" + bookingString + "\'";

					int rows = executeQuery(checkInsert);
					refreshPanel(panelIndex);
					if (rows > 0)
						displayPopUp("Successfully booked flight. Thank you!", "Booked Flight");
					else 
						displayPopUp("There was an error in the system. Please try again.", "Error");

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getErrorCode());
					if (ex.getErrorCode() == 0)
					{
						displayPopUp("You already booked that flight!", "Error");
					}
					System.out.println(ex.getMessage());
				}

			}
		});
		
		//Add components
		PanelList.get(panelIndex).add(passTxt);
		PanelList.get(panelIndex).add(passInput);

		PanelList.get(panelIndex).add(originTxt);
		PanelList.get(panelIndex).add(originInput);
		PanelList.get(panelIndex).add(destTxt);
		PanelList.get(panelIndex).add(destInput);

		PanelList.get(panelIndex).add(monthTxt);
		PanelList.get(panelIndex).add(monthNum);
		PanelList.get(panelIndex).add(dateTxt);
		PanelList.get(panelIndex).add(dateNum);
		PanelList.get(panelIndex).add(yearTxt);
		PanelList.get(panelIndex).add(yearNum);

		PanelList.get(panelIndex).add(searchButt);
	}

	/* 3) Review Flight */
	private void createPanel3()
	{
		int panelIndex = 2;
		int numPassengers = getNumberOfPassengers() - 1;
		JPanel Panel = new JPanel();
		
		//General information components
		JLabel passengerIDTxt = new JLabel("Passenger ID");
		JLabel flightNumTxt = new JLabel("Flight ID");
		JLabel ratingTxt = new JLabel("Score");
		
		JTextField flightNumField = new JTextField(20);

		JSpinner passengerIDSpinner = createDigitSpinner(1, 0, numPassengers);
		JSpinner ratingSpinner = createDigitSpinner(1, 0, 5);
		
		passengerIDSpinner.setValue(0);
		ratingSpinner.setValue(0);
		//This is where you add all the components you need for that specific panel
		//Crate comment text area
		String commentBoxText = "Type your comments here.";
		JTextArea commentBox = new JTextArea(10,80); //(height, width) 
		commentBox.setText(commentBoxText);
		commentBox.setLineWrap(true); //Wrap the text inside the text area
		commentBox.setWrapStyleWord(true); //So the words dont get broken up when it doesn't fit in the line
		commentBox.addFocusListener(new FocusListener() 
		{
			public void focusGained(FocusEvent e)
			{
				if(commentBox.getText().equals(commentBoxText))
				{
					commentBox.setText("");
				}
			}
			
			//Does nothing, but we need this since jswing requires both functions
			public void focusLost(FocusEvent e)
			{

			}
		});
			
		
		//this scroll panel allows the text area to scroll on overflow as needed
		JScrollPane commentScroll = new JScrollPane(commentBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);


		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					int pID = Integer.valueOf(String.valueOf(passengerIDSpinner.getValue()));
					String flightNum = flightNumField.getText();
					String rating = String.valueOf(ratingSpinner.getValue());	
				        String comment = commentBox.getText();
					int score = Integer.valueOf(String.valueOf(ratingSpinner.getValue()));
					
					if(flightNum.isEmpty())
					{
						displayPopUp("Please enter a flight number.", "Input Error");
						return;
					}
					if(comment == commentBoxText)
					{
						comment = "";
					}
					//Check if flight number exists
					String query = "SELECT flightNum FROM Flight WHERE ";
					query += "flightNum = \'" + flightNum + "\'";

					int result = executeQuery(query);
					if(result == 0)
					{
						displayPopUp("Flight number does not exist.", "Input Error");
						return;
					}
					
					//Check if passenger attended flight
					query = "SELECT pID, flightnum FROM booking WHERE ";
					query += "pID = " + pID + "AND ";
					query += "flightNum = \'" + flightNum + "\'";
					result = executeQuery(query);
					if(result == 0)
					{
						displayPopUp("Passenger did not attend this flight!", "Submission1 Error");
						return;
					}
					
					//Check if passenger already reviewed this flight
					query = "SELECT pID FROM ratings WHERE ";
					query += "pID = " + pID;
					query += " AND flightnum = \'" + flightNum + "\'";
					result = executeQuery(query);

					if(result > 0)
					{
						displayPopUp("There is already a review made by this passenger!", "Submission Error");
						return;
					}
					
					//Main Query
					int latestOldRatingIDs = getNumberOfRatings();
					query = "INSERT INTO Ratings (pID, flightNum, score, comment) ";
					query += "VALUES( ";
					query += pID + ", ";
					query += "\'" + flightNum + "\'" + ", ";
					query += "\'" + score + "\'" + ", ";
					query += "\'" + comment + "\'" + ")";
			
					executeUpdate(query);
					//Check if the comment went through	
					int latestNewRatingIDs = getNumberOfRatings();
					if(latestNewRatingIDs > latestOldRatingIDs)
					{
						displayPopUp("Review added!", "Success");
					}
					else
					{
						displayPopUp("There was an error in adding the review. Please try again.", "Error");
					}
					//Check if passenger is inside the database
					refreshPanel(panelIndex);
				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
					System.out.println(ex.getErrorCode());
					if (ex.getErrorCode() == 0)
					{
						displayPopUp("Internal error. Please try again.", "Error");
					}
				}
			}
		});
		//Add components
		PanelList.get(panelIndex).add(passengerIDTxt);
		PanelList.get(panelIndex).add(passengerIDSpinner);
		PanelList.get(panelIndex).add(flightNumTxt);
		PanelList.get(panelIndex).add(flightNumField);
		PanelList.get(panelIndex).add(ratingTxt);
		PanelList.get(panelIndex).add(ratingSpinner);
		PanelList.get(panelIndex).add(commentScroll);
		PanelList.get(panelIndex).add(submitButt);
	}
	

	/* 4) List Flights from Origin to Destination */
	private void createPanel4()
	{
		int panelIndex = 3;
		JLabel originTxt = new JLabel("Origin: ");
		JLabel destTxt = new JLabel("Destination: ");
		JTextField originInput = new JTextField("Madrid", 15);
		JTextField destInput = new JTextField("Seattle", 15);

		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table

		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					
					String originIn = originInput.getText();
					String destIn = destInput.getText();
					if (originIn.length() == 0)
					{
						displayPopUp("Please enter an origin", "Invalid Input");
						return;
					}
					if (destIn.length() == 0)
					{
						displayPopUp("Please enter a destination", "Invalid Input");
						return;
					}
					if (!isAlpha(originIn) || !isAlpha(destIn))
					{
						displayPopUp("Location can only use letters.", "Invalid Input");
						return;
					}

					//Execute the Query and Make Table
					String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination, Flight.plane, Flight.duration ";
					flightAvailQuery += "FROM Flight ";
					flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, COUNT(Booking.flightNum) as Total ";
					flightAvailQuery += "FROM Booking ";
					flightAvailQuery += "GROUP BY Booking.flightNum) AS a ";
					flightAvailQuery += "ON a.flightNum = Flight.flightNum AND a.Total < Flight.seats ";
					flightAvailQuery += "WHERE Flight.origin = \'" + originIn;
					flightAvailQuery += "\' AND Flight.destination = \'" + destIn + "\'";

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(flightAvailQuery)); 
					PanelList.get(panelIndex).add(queryDBTable);


					//Refresh the frame to show the added table
					validate(); 
					repaint(); 

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
				}
			}
		});

		//Add components
		PanelList.get(panelIndex).add(originTxt);
		PanelList.get(panelIndex).add(originInput);
		PanelList.get(panelIndex).add(destTxt);
		PanelList.get(panelIndex).add(destInput);
		PanelList.get(panelIndex).add(submitButt);
	}

	/* 5) List Most Popular Destinations */
	private void createPanel5()
	{
		int panelIndex = 4;
		//This is where you add all the components you need for that specific panel	
		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table
		JLabel topDestsTxt = new JLabel("Please enter the number of popular destinations you would like to see:");
	
		JSpinner topDestsSpinner = createDigitSpinner(2, 1, 999);
		topDestsSpinner.setValue(1);		

		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
				        int topDests = Integer.valueOf(String.valueOf(topDestsSpinner.getValue()));
					
				
					//Execute the Query and Make Table
					//Replace the string with your SQL Query
					String query = "SELECT destination, COUNT(flightnum) AS flightamount ";
					query += "FROM Flight ";
					query += "GROUP BY destination ";
					query += "ORDER BY COUNT(flightnum) DESC ";
					query += "LIMIT " + Integer.toString(topDests);

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(query)); 
					PanelList.get(panelIndex).add(queryDBTable, BorderLayout.CENTER);

					//Refresh the frame to show the added table
					validate(); 
					repaint();

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
					System.out.println(ex.getErrorCode());
					if (ex.getErrorCode() == 0)
					{
						displayPopUp("Internal error. Please contact support or try again.", "Error");
					}
				}
			}
		});
		PanelList.get(panelIndex).add(topDestsTxt);
		PanelList.get(panelIndex).add(topDestsSpinner);
		PanelList.get(panelIndex).add(submitButt);
	}

	/* 6) List Highest Rated Destinations */
	private void createPanel6()
	{
		int panelIndex = 5;
		//This is where you add all the components you need for that specific panel
		JLabel title = new JLabel("Please enter how many top reviews you want to see: ");
		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table
		JSpinner numRate = createDigitSpinner(2, 1, 9000);
		numRate.setValue(1);
		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Get required fields
					Object numIn = numRate.getValue(); 
					//Execute the Query and Make Table
					//Groups flight numbers and shows the average score of each.
					String grabNReview = "SELECT Ratings.flightNum, ";
					grabNReview += "AVG(Ratings.score) as avg, ";
					grabNReview += "COUNT(Ratings.flightNum) as total ";
					grabNReview += "FROM Ratings GROUP BY Ratings.flightNum ";
					grabNReview += "ORDER BY avg DESC, total DESC LIMIT " + numIn;
					//System.out.print(grabNReview);

					String displayInfo = "SELECT Airline.name, Flight.flightNum, Flight.origin, ";
					displayInfo += "Flight.destination, Flight.plane, a.avg ";
					displayInfo += "FROM Airline, Flight ";
					displayInfo += "INNER JOIN (";
					displayInfo += grabNReview + ") AS a ";
					displayInfo += "ON Flight.flightNum = a.flightNum ";
					displayInfo += "WHERE Flight.airID = Airline.airID ";
					displayInfo += "ORDER BY a.avg DESC, a.total DESC";

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(displayInfo)); 
					PanelList.get(panelIndex).add(queryDBTable);

					//Refresh the frame to show the added table
					validate(); 
					repaint(); 

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
				}
			}
		});

		//Add components
		PanelList.get(panelIndex).add(title);
		PanelList.get(panelIndex).add(numRate);
		PanelList.get(panelIndex).add(submitButt);
	}

	/* 7) List Flights to Destination in order of Duration */
	private void createPanel7()
	{
		int panelIndex = 6;
		//This is where you add all the components you need for that specific panel
		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table
		JLabel originTxt = new JLabel("Origin");
		JLabel destinationTxt = new JLabel("Destination");
		JLabel flightTxt = new JLabel("Flights to See:");

		JTextField originField = new JTextField("Beijing", 15);
		JTextField destinationField = new JTextField("San Francisco", 15);
		
		JSpinner flightSpinner = createDigitSpinner(2, 1, 999);
		flightSpinner.setValue(1);
		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					String origin = originField.getText();
					String destination = destinationField.getText();
					int flights = Integer.valueOf(String.valueOf(flightSpinner.getValue()));
					if(origin.isEmpty())
					{
						displayPopUp("Please enter an origin.", "Input Error");
						return;
					}
					else if(destination.isEmpty())
					{
						displayPopUp("Please enter a destination.", "Input Error");
						return;
					}
					else if(!isAlpha(origin))
					{
						displayPopUp("Origins must contain letters.", "Input Error");
						return;
					}
					else if(!isAlpha(destination))
					{
						displayPopUp("Destinations must contain letters.", "Input Error");
						return;
					}
					String checkQuery = "SELECT destination FROM Flight WHERE destination = ";
					checkQuery += "\'" + destination + "\'";
					int result = executeQuery(checkQuery);
					if(result == 0)
					{
						displayPopUp("There are no flights with the destination provided.", "Search Error");
						return;
					}	
				
					//Execute the Query and Make Table
					//Replace the string with your SQL Query
					String locationDuration = "SELECT name, flightNum, origin, destination, plane, duration ";
					locationDuration += "FROM Airline, Flight ";
					locationDuration += "WHERE Airline.airID = Flight.airId ";
					locationDuration += "AND origin = \'" + origin + "\'";
					locationDuration += "AND destination = \'" + destination + "\'";
					locationDuration += "ORDER BY duration ";
					locationDuration += "LIMIT " + flights; 

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(locationDuration)); 
					PanelList.get(panelIndex).add(queryDBTable, BorderLayout.CENTER);

					//Refresh the frame to show the added table
					validate(); 
					repaint();

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
					System.out.println(ex.getErrorCode());
					if (ex.getErrorCode() == 0)
					{
						displayPopUp("Internal error. Please contact support or try again.", "Error");
					}
				}
			}
		});
		PanelList.get(panelIndex).add(originTxt);
		PanelList.get(panelIndex).add(originField);
		PanelList.get(panelIndex).add(destinationTxt);
		PanelList.get(panelIndex).add(destinationField);
		PanelList.get(panelIndex).add(flightTxt);
		PanelList.get(panelIndex).add(flightSpinner);
		PanelList.get(panelIndex).add(submitButt);
	}

	/* 8) Find Number of Available Seats On a Given Flight */
	private void createPanel8()
	{
		int panelIndex = 7;
		//This is where you add all the components you need for that specific panel
		JLabel title = new JLabel("Please enter in a date.");
		JLabel monthTxt = new JLabel("Month: ");
		JLabel dateTxt = new JLabel("Date: ");
		JLabel yearTxt = new JLabel("Year: ");


		JSpinner monthNum = createDigitSpinner(2, 1, 12);
		JSpinner dateNum = createDigitSpinner(2, 1, 31);
		JSpinner yearNum = createDigitSpinner(1900, 1900, 9999);
		monthNum.setValue(1);	
		dateNum.setValue(1);
		yearNum.setValue(2017);		
		JScrollPane queryDBTable = new JScrollPane();
		queryDBTable.setPreferredSize(new Dimension(1200, 500)); //Sets the size of the table

		JButton submitButt = new JButton("Submit"); //This button will run the query
		submitButt.addActionListener(new ActionListener() //onclick function
		{
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					//Validate Required Fields
					String departIn = monthNum.getValue() + "/" + dateNum.getValue() + "/" + yearNum.getValue();
					//Execute the Query and Make Table
					//Replace the string with your SQL Query
					String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination,";
					flightAvailQuery += " COALESCE(a.departure, date\'" + departIn +  "\') AS departure, ";
					flightAvailQuery += "COALESCE(a.booked, 0) AS booked, Flight.seats, ";
					flightAvailQuery += "COALESCE(Flight.seats - a.booked, Flight.seats) AS available ";
					flightAvailQuery += "FROM Flight ";
					flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, Booking.departure, COUNT(Booking.flightNum) as booked ";
					flightAvailQuery += "FROM Booking WHERE Booking.departure = \'" + departIn + "\' ";
					flightAvailQuery += "GROUP BY Booking.flightNum, Booking.departure) AS a ";
					flightAvailQuery += "ON a.flightNum = Flight.flightNum ";

					//Create the Table
					if (queryDBTable != null)
						PanelList.get(panelIndex).remove(queryDBTable);
					queryDBTable.getViewport().add(createDBTable(flightAvailQuery)); 
					PanelList.get(panelIndex).add(queryDBTable);

					//Refresh the frame to show the added table
					validate(); 
					repaint(); 

				}
				catch(SQLException ex)
				{
					System.out.println(ex.getMessage());
				}
			}
		});

		//Add components
		PanelList.get(panelIndex).add(title);
		PanelList.get(panelIndex).add(monthTxt);
		PanelList.get(panelIndex).add(monthNum);
		PanelList.get(panelIndex).add(dateTxt);
		PanelList.get(panelIndex).add(dateNum);
		PanelList.get(panelIndex).add(yearTxt);
		PanelList.get(panelIndex).add(yearNum);
		PanelList.get(panelIndex).add(submitButt);
	}
	
	//------Constructor------//
	public AirBooking(String dbname, String dbport, String user, String passwd) throws SQLException {
		System.out.print("Connecting to database...");
		try
		{
			// constructs the connection URL
			String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
			System.out.println ("Connection URL: " + url + "\n");
			
			// obtain a physical connection
		        this._connection = DriverManager.getConnection(url, user, passwd);
	        	System.out.println("Done");
		

			/*-----------Start of creating the application-----------*/
			this.setSize(1320,820); //Frame size (Frame aka Window)

			Toolkit tk = Toolkit.getDefaultToolkit(); //Allows us to get the size of the montior screen
			Dimension dim = tk.getScreenSize(); 

			//Centers the frame in the middle of the screen
			int xPos = (dim.width / 2) - (this.getWidth() / 2);
			int yPos = (dim.height / 2) - (this.getHeight() / 2);
			this.setLocation(xPos, yPos);

			this.setResizable(false); //Restricts resizing the frame
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //Closes application when click "X" button
			this.setTitle("Flight Database"); //Name of application

			//Create panels for each query (8)
			int i = 0;
			while (i < 8)
			{
				PanelList.add(new JPanel());
				i += 1;
			}
			
			/*Add the components to the first tab
			 *Only need to do this one because the tabbedPanel onchange listener
			 * will handle adding the components to the other panels
			 */
			createPanel1(); 

			//Create a tab for each panel
			tabbedPanel.add("Add Passenger", PanelList.get(0));
			tabbedPanel.add("Book Flight", PanelList.get(1));
			tabbedPanel.add("Review Flight", PanelList.get(2));
			tabbedPanel.add("Flights from Origin to Destination", PanelList.get(3));
			tabbedPanel.add("Most Popular Destinations", PanelList.get(4));
			tabbedPanel.add("Highest Rated Destinations", PanelList.get(5));
			tabbedPanel.add("Flights to Destinations in order of Duration", PanelList.get(6));
			tabbedPanel.add("Number of Available Seats on a Flight", PanelList.get(7));
			
			//Will reset the panel to default of the selected tab onchange
			tabbedPanel.addChangeListener(new ChangeListener()
			{
				public void stateChanged(ChangeEvent evt)
				{
					int index = tabbedPanel.getSelectedIndex();
					refreshPanel(index);
				}
			});

			this.add(tabbedPanel); //Add tabs to the frame
		
			this.setVisible(true); //Set the frame to visible
			
		}catch(Exception e){
			System.err.println("Error - Unable to Connect to Database: " + e.getMessage());
			System.out.println("Make sure you started postgres on this machine");
			System.exit(-1);
		}
	}

	//Function to reset the panel in each tab
	public void refreshPanel(int index)
	{
		PanelList.get(index).removeAll();
		createPanelAtIndex(index);
		revalidate();
		repaint();
	}

	//Helper function for refreshPanel
	public void createPanelAtIndex(int index)
	{
		switch(index)
		{
			case 0:
				createPanel1();
				break;
			case 1:
				createPanel2();
				break;
			case 2:
				createPanel3();
				break;
			case 3:
				createPanel4();
				break;
			case 4:
				createPanel5();
				break;
			case 5:
				createPanel6();
				break;
			case 6:
				createPanel7();
				break;
			case 7:
				createPanel8();
				break;
		}
	}


	/*-------------------------End of Java Swing Stuff-------------------------*/


	/**
	 * Method to execute an update SQL statement.  Update SQL instructions
	 * includes CREATE, INSERT, UPDATE, DELETE, and DROP.
	 * 
	 * @param sql the input SQL string
	 * @throws java.sql.SQLException when update failed
	 * */
	public void executeUpdate (String sql) throws SQLException { 
		// creates a statement object
		Statement stmt = this._connection.createStatement ();

		// issues the update instruction
		stmt.executeUpdate (sql);

		// close the instruction
	    stmt.close ();
	}//end executeUpdate

	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and outputs the results to
	 * standard out.
	 * 
	 * @param query the input query string
	 * @return the number of rows returned
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public int executeQueryAndPrintResult (String query) throws SQLException {
		//creates a statement object
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		/*
		 *  obtains the metadata object for the returned result set.  The metadata
		 *  contains row and column info.
		 */
		ResultSetMetaData rsmd = rs.getMetaData ();
		int numCol = rsmd.getColumnCount ();
		int rowCount = 0;
		
		//iterates through the result set and output them to standard out.
		boolean outputHeader = true;
		while (rs.next()){
			if(outputHeader){
				for(int i = 1; i <= numCol; i++){
					System.out.print(rsmd.getColumnName(i) + "\t");
			    }
			    System.out.println();
			    outputHeader = false;
			}
			for (int i=1; i<=numCol; ++i)
				System.out.print (rs.getString (i) + "\t");
			System.out.println ();
			++rowCount;
		}//end while
		stmt.close ();
		return rowCount;
	}
	
	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and returns the results as
	 * a list of records. Each record in turn is a list of attribute values
	 * 
	 * @param query the input query string
	 * @return the query result as a list of records
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public List<List<String>> executeQueryAndReturnResult (String query) throws SQLException { 
		//creates a statement object 
		Statement stmt = this._connection.createStatement (); 
		
		//issues the query instruction 
		ResultSet rs = stmt.executeQuery (query); 
	 
		/*
		 * obtains the metadata object for the returned result set.  The metadata 
		 * contains row and column info. 
		*/ 
		ResultSetMetaData rsmd = rs.getMetaData (); 
		int numCol = rsmd.getColumnCount (); 
		int rowCount = 0; 
	 
		//iterates through the result set and saves the data returned by the query. 
		boolean outputHeader = false;
		List<List<String>> result  = new ArrayList<List<String>>(); 
		while (rs.next()){
			List<String> record = new ArrayList<String>(); 
			for (int i=1; i<=numCol; ++i) 
				record.add(rs.getString (i)); 
			result.add(record); 
		}//end while 
		stmt.close (); 
		return result; 
	}//end executeQueryAndReturnResult
	
	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and returns the number of results
	 * 
	 * @param query the input query string
	 * @return the number of rows returned
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public int executeQuery (String query) throws SQLException {
		//creates a statement object
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		int rowCount = 0;

		//iterates through the result set and count nuber of results.
		if(rs.next()){
			rowCount++;
		}//end while
		stmt.close ();
		return rowCount;
	}
	
	/**
	 * Method to fetch the last value from sequence. This
	 * method issues the query to the DBMS and returns the current 
	 * value of sequence used for autogenerated keys
	 * 
	 * @param sequence name of the DB sequence
	 * @return current value of a sequence
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	
	public int getCurrSeqVal(String sequence) throws SQLException {
		Statement stmt = this._connection.createStatement ();
		
		ResultSet rs = stmt.executeQuery (String.format("Select currval('%s')", sequence));
		if (rs.next()) return rs.getInt(1);
		return -1;
	}

	/**
	 * Method to close the physical connection if it is open.
	 */
	public void cleanup(){
		try{
			if (this._connection != null){
				this._connection.close ();
			}//end if
		}catch (SQLException e){
	         // ignored.
		}//end try
	}//end cleanup


	/**
	 * The main execution method
	 * 
	 * @param args the command line arguments this inclues the <mysql|pgsql> <login file>
	 */
	public static void main (String[] args) {
		if (args.length != 3) {
			System.err.println (
				"Usage: " + "java [-classpath <classpath>] " + AirBooking.class.getName () +
		            " <dbname> <port> <user>");
			return;
		}//end if
		
		AirBooking esql = null;
		
		try{
			
			try {
				Class.forName("org.postgresql.Driver");
			}catch(Exception e){

				System.out.println("Where is your PostgreSQL JDBC Driver? " + "Include in your library path!");
				e.printStackTrace();
				return;
			}
			
			String dbname = args[0];
			String dbport = args[1];
			String user = args[2];
			

			esql = new AirBooking (dbname, dbport, user, "");
			
			boolean keepon = true;
			while(keepon){
				System.out.println("MAIN MENU");
				System.out.println("---------");
				System.out.println("1. Add Passenger");
				System.out.println("2. Book Flight");
				System.out.println("3. Review Flight");
				System.out.println("4. Insert or Update Flight");
				System.out.println("5. List Flights From Origin to Destination");
				System.out.println("6. List Most Popular Destinations");
				System.out.println("7. List Highest Rated Destinations");
				System.out.println("8. List Flights to Destination in order of Duration");
				System.out.println("9. Find Number of Available Seats on a given Flight");
				System.out.println("10. < EXIT");
				
				switch (readChoice()){
					case 1: AddPassenger(esql); break;
					case 2: BookFlight(esql); break;
					case 3: TakeCustomerReview(esql); break;
					case 4: InsertOrUpdateRouteForAirline(esql); break;
					case 5: ListAvailableFlightsBetweenOriginAndDestination(esql); break;
					case 6: ListMostPopularDestinations(esql); break;
					case 7: ListHighestRatedRoutes(esql); break;
					case 8: ListFlightFromOriginToDestinationInOrderOfDuration(esql); break;
					case 9: FindNumberOfAvailableSeatsForFlight(esql); break;
					case 10: keepon = false; break;
				}
			}
		}catch(Exception e){
			System.err.println (e.getMessage ());
		}finally{
			try{
				if(esql != null) {
					System.out.print("Disconnecting from database...");
					esql.cleanup ();
					System.out.println("Done\n\nBye !");
				}//end if				
			}catch(Exception e){
				// ignored.
			}
		}
	}

	public static int readChoice() {
		int input;
		// returns only if a correct value is given.
		do {
			System.out.print("Please make your choice: ");
			try { // read the integer, parse it and break.
				input = Integer.parseInt(in.readLine());
				break;
			}catch (Exception e) {
				System.out.println("Your input is invalid!");
				continue;
			}//end try
		}while (true);
		return input;
	}//end readChoice
	
	public static void AddPassenger(AirBooking esql){//1
		//Add a new passenger to the database
		try{
			String query = "INSERT INTO Passenger (passNum, fullName, bdate, country) ";
			System.out.print("Enter Name: ");
			String name = in.readLine();
			System.out.print("Passport Number: ");
			String passportno = in.readLine();
			System.out.print("Birth Date: ");
			String birthdate = in.readLine();
			System.out.print("Country: ");
			String country = in.readLine();
			query += "VALUES ( " + "\'" + passportno + "\'" + ", " + "\'" + name + "\'" + ", " + "\'" + birthdate + "\'" + ", " + "\'" + country + "\'" + ")";
			
			esql.executeUpdate(query);
			String insertQuery = "pIDseq";
			int newid = esql.getCurrSeqVal(insertQuery);
			System.out.println(String.valueOf(newid));
			System.out.print("Passenger added!");
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	

	public static void BookFlight(AirBooking esql){//2
		//Book Flight for an existing customer
		try{

			List<List<String>> passInfo = null;
			do
			{
				System.out.print("Please enter passenger ID (enter -1 to exit to menu): " );

				int passID = Integer.parseInt(in.readLine());
				if (passID == -1) return;

				String passQuery = "SELECT pID, fullName FROM Passenger WHERE pID =" + passID;
				passInfo = esql.executeQueryAndReturnResult(passQuery);
				if (passInfo.size() == 0)
				{
					System.out.print("Invalid passenger ID.");
					continue;
				}
				else
				{
					System.out.print("Are you " + passInfo.get(0).get(1) + "? (y/n): ");
					String answer = in.readLine();
					if ("y".equals(answer)) break;
					
				}
			}while(true);
			System.out.print("__________________________________________\n");
			System.out.print("Welcome " + passInfo.get(0).get(1) + "\n");
			System.out.print("Please select your origin: ");
			String originIn = in.readLine(); 
			System.out.print("Please select your destination: ");
			String destIn = in.readLine();
 			System.out.print("Please enter a departure date (MM/DD/YYYY): ");
			String departIn = in.readLine(); 

			//Find if that number is less than the flight seat's, its available
			String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination,";
			flightAvailQuery += " COALESCE(a.departure, date\'" + departIn +  "\') AS departure, ";
			flightAvailQuery += "COALESCE(a.booked, 0) AS booked, Flight.seats, ";
			flightAvailQuery += "COALESCE(Flight.seats - a.booked, Flight.seats) AS available ";
			flightAvailQuery += "FROM Flight ";
			flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, Booking.departure, COUNT(Booking.flightNum) as booked ";
			flightAvailQuery += "FROM Booking WHERE Booking.departure = \'" + departIn + "\' ";
			flightAvailQuery += "GROUP BY Booking.flightNum, Booking.departure) AS a ";
			flightAvailQuery += "ON a.flightNum = Flight.flightNum ";
			flightAvailQuery += "WHERE Flight.origin = \'" + originIn;
			flightAvailQuery += "\' AND Flight.destination = \'" + destIn + "\'";

			//esql.executeQueryAndPrintResult(flightAvailQuery);
			
			List<List<String>> allAvailFlights = esql.executeQueryAndReturnResult(flightAvailQuery);

			int flightIndex = -2;

			List<String> rowHeader = new ArrayList<String>();
			rowHeader.add("Number");
			rowHeader.add("Flight");
			rowHeader.add("Origin");
			rowHeader.add("Destination");
			rowHeader.add("Departure");
			rowHeader.add("Booked");
			rowHeader.add("Total");
			rowHeader.add("Available");
			do
			{
				int index = 0;
				System.out.print("---------------List of Available Flights---------------\n");
				for (String header: rowHeader) System.out.print(header + "\t");
				System.out.print("\n");
				for (List<String> row : allAvailFlights)
				{
					System.out.print("#" + index + ":\t");
					for(String temp: row) System.out.print(temp.replaceAll("\\s+","")+ "\t");
					System.out.print('\n');
					index++;
				}
				System.out.print("\nWhich flight would you like to book?(type -1 to exit) \n");
				flightIndex = Integer.parseInt(in.readLine());
				if (flightIndex < 0) return;
				else if(flightIndex >= allAvailFlights.size())
				{
					System.out.print("\n That is an invalid number.");
					continue;
				} 
				System.out.print("\nAre you sure you want to book this flight?(y/n)\n");
				for(String temp: allAvailFlights.get(flightIndex)) System.out.print(temp.replaceAll("\\s+","") + "\t");
				System.out.print('\n');
				String answer = in.readLine();
				if ("y".equals(answer)) break;
			}while(true);

			//Input into the Booking database
			//Booking number is 5 chars, 4 nums, 1 char
			//Create random booking number:
			Random r = new Random();
			String bookingString = "";
			int count = 0;
			while (count < 5)
			{
				bookingString += (char) (r.nextInt(26) + 'A');
				count++;
			}
			count = 0;
			while (count < 4)
			{
				bookingString += r.nextInt(9);
				count++;
			}
			bookingString += (char) (r.nextInt(26) + 'A');
			System.out.print(bookingString + "\n");

			//Insert into Booking table
			//bookRef, depart, flightNum, pid
			//allAvailFlights indexes are:
			//0-flightNum, 1-origin, 2-dest, 3-depart, 4-booked, 5-total, 6-free 
			String insertQuery = "INSERT into Booking VALUES (\'";
			insertQuery += bookingString + "\', date \'";
			insertQuery += allAvailFlights.get(flightIndex).get(3) + "\', \'";
			insertQuery += allAvailFlights.get(flightIndex).get(0) + "\', ";
			insertQuery += passInfo.get(0).get(0) + ")";
			
			esql.executeUpdate(insertQuery);
			
			String checkInsert = "SELECT bookRef FROM Booking WHERE bookRef = ";
			checkInsert += "\'" + bookingString + "\'";

			int rows = esql.executeQuery(checkInsert);
			if (rows > 0) System.out.print("Successfully booked! Thank you.\n");
			else System.out.print("There seems to be an error. Please try again.\n");
			
		}catch(Exception e){
			System.err.println(e.getMessage());	
		}	
	}
	
	public static void TakeCustomerReview(AirBooking esql){//3
		//Insert customer review into the ratings table
		try {
			System.out.print("Enter Passenger ID: ");
			int pID = Integer.parseInt(in.readLine());
			String query = "SELECT pID FROM Passenger WHERE pID = " + pID;
			int result = esql.executeQuery(query);
			//Check if passenger exists
			if(result == 0)
			{
				System.out.print("ERROR: Passenger does not exist.\n");
				return;
			}
			//Check if flight number exists
			System.out.print("Flight Number: ");
			String flightNum = in.readLine();
			query = "SELECT flightNum FROM Flight WHERE flightNum = \'" + flightNum + "\'";
			result = esql.executeQuery(query);
			//Check if flight number exists
			if(result == 0)
			{
				System.out.print("ERROR: Flight number does not exist.\n");
				return;
			}
			//Now check if the listing actually exists in the bookings (For fraud ratings)
			query = "SELECT pID, flightnum FROM booking WHERE ";
			query += "pID = " + pID + " AND ";
			query += "flightNum = \'" + flightNum + "\'";
			result = esql.executeQuery(query);
			if(result == 0)
			{
				System.out.print("ERROR: Passenger did not attend this flight!\n");
				return;
			} 
			System.out.print("Score: ");
			int score = Integer.parseInt(in.readLine());
			//Check if score is valid (0-5)
			if(score < 0 || score > 5)
			{
				System.out.print("ERROR: Score cannot be lower than 0 or higher than 5.\n");
				return;
			}
			//Look for comment
			System.out.print("Comment: ");
			String comment = in.readLine();
			query = "INSERT INTO Ratings (pID, flightNum, score, comment) ";
			query += "VALUES ( " + pID + ", " + "\'" + flightNum + "\'" + ", " + score + ", " + "\'" + comment + "\'" + ")";
			esql.executeQuery(query);
			System.out.print("Review added.");
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	public static void InsertOrUpdateRouteForAirline(AirBooking esql){//4
		//Insert a new route for the airline
	}
	
	public static void ListAvailableFlightsBetweenOriginAndDestination(AirBooking esql) throws Exception{//5
		//List all flights between origin and distination (i.e. flightNum,origin,destination,plane,duration) 
		System.out.print("Please select your origin: ");
		String originIn = in.readLine(); 
		System.out.print("Please select your destination: ");
		String destIn = in.readLine(); 
		
		String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination, Flight.plane, Flight.duration ";
		flightAvailQuery += "FROM Flight ";
		flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, COUNT(Booking.flightNum) as Total ";
		flightAvailQuery += "FROM Booking ";
		flightAvailQuery += "GROUP BY Booking.flightNum) AS a ";
		flightAvailQuery += "ON a.flightNum = Flight.flightNum AND a.Total < Flight.seats ";
		flightAvailQuery += "WHERE Flight.origin = \'" + originIn;
		flightAvailQuery += "\' AND Flight.destination = \'" + destIn + "\'";
		//System.out.print(flightAvailQuery);
		try
		{
			esql.executeQueryAndPrintResult(flightAvailQuery);
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}

	}
	
	public static void ListMostPopularDestinations(AirBooking esql){//6
		//Print the k most popular destinations based on the number of flights offered to them (i.e. destination, choices)
		try {
			System.out.print("Give the number of popular destinations you would like to see.");
			int dests = Integer.parseInt(in.readLine());
			if (dests < 1)
			{
				System.out.print ("Please provide at least 1 destination.");
				return;
			}
			String query = "SELECT destination, COUNT(flightnum) AS flightamount ";
			query += "FROM Flight ";
			query += "GROUP BY destination ";
			query += "ORDER BY COUNT(flightnum) DESC ";
			query += "LIMIT " + Integer.toString(dests);
			//esql.executeQueryAndPrintResult(query);
			List<List<String>> result = esql.executeQueryAndReturnResult(query);
			System.out.println("The most popular destinations are: ");
			for(int i = 0; i < result.size(); i++)
			{
				//Print the destinations (There should only be two elements per destination)
				List<String> destination = result.get(i);
				for(int j = 0; j < destination.size() - 1; j++)
				{
					System.out.println(destination.get(j) + "| " + destination.get(j+1) + " flights");
				}
			}
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	public static void ListHighestRatedRoutes(AirBooking esql){//7
		try{
			System.out.print("Please enter number of highest rated routes you want: ");
			
			int numIn = Integer.parseInt(in.readLine());
			if (numIn < 0)
			{
				System.out.print("Error: Please enter a number higher than 0.");
				return;
			}	
			//Groups flight numbers and shows the average score of each.
			String grabNReview = "SELECT Ratings.flightNum, ";
			grabNReview += "AVG(Ratings.score) as avg, ";
			grabNReview += "COUNT(Ratings.flightNum) as total ";
			grabNReview += "FROM Ratings GROUP BY Ratings.flightNum ";
			grabNReview += "ORDER BY avg DESC, total DESC LIMIT " + numIn;
			//System.out.print(grabNReview);

			String displayInfo = "SELECT Airline.name, Flight.flightNum, Flight.origin, ";
			displayInfo += "Flight.destination, Flight.plane, a.avg ";
			displayInfo += "FROM Airline, Flight ";
			displayInfo += "INNER JOIN (";
			displayInfo += grabNReview + ") AS a ";
			displayInfo += "ON Flight.flightNum = a.flightNum ";
			displayInfo += "WHERE Flight.airID = Airline.airID ";
			displayInfo += "ORDER BY a.avg DESC, a.total DESC";

			esql.executeQueryAndPrintResult(displayInfo);
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}

	}
	public static void ListFlightFromOriginToDestinationInOrderOfDuration(AirBooking esql){//8
		//List flight to destination in order of duration (i.e. Airline name, flightNum, origin, destination, duration, plane)
        //TODO: If you haven't already, make sure there are multiple entries with the same origin and destination to test this out!!!
		try{
			//Find the origin
			System.out.print("What is the origin of the flight?\n");
			String origin = in.readLine();

			String query = "SELECT origin FROM Flight WHERE origin = ";
			query += "\'" + origin + "\'";
			int result = esql.executeQuery(query);
			if(result == 0)
			{
				System.out.print("There are no flights with the origin listed.");
				return;
			}
			//Find the destination
			System.out.print("What is the destination?\n");
			String destination = in.readLine();

			query = "SELECT destination FROM Flight WHERE destination = ";
			query += "\'" + destination + "\'";
			result = esql.executeQuery(query);
			if(result == 0)
			{
				System.out.print("There are no flights with the destination provided.");
				return;
			}
            
			System.out.print("Provide the number of flights you would like to see: ");
			int flights = Integer.parseInt(in.readLine());
			if (flights < 1)
			{
				System.out.print("Please provide at least 1 flight to view.\n");
				return;
			}
			query = "SELECT name, flightNum, origin, destination, plane, duration ";
			query += "FROM Airline, Flight ";
			query += "WHERE Airline.airId = Flight.airId ";
			query += "AND origin = \'" + origin + "\'";
			query += "AND destination = \'" + destination + "\'";
			query += "ORDER BY duration ";
			query += "LIMIT " + Integer.toString(flights);
			List<List<String>> output = esql.executeQueryAndReturnResult(query);
			if(output.size() == 0)
			{
				System.out.print("There are no flights from " + origin + " to " + destination + ".\n");
				return;
			}
			System.out.print("Here are the flights from " + origin + " to " + destination + ".\n");
			for(int i = 0; i < output.size(); i++)
			{
				List<String> flight = output.get(i);
				for(int j = 0; j < flight.size(); j++)
				{
					System.out.print(flight.get(j) + "|");
				}
				System.out.print("\n");
			}

		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	public static void FindNumberOfAvailableSeatsForFlight(AirBooking esql){//9
		try
		{
			System.out.print("Please enter a date: ");
			String departIn = in.readLine(); 
			
			//Find if that number is less than the flight seat's, its available
			String flightAvailQuery = "SELECT Flight.flightNum, Flight.origin, Flight.destination,";
			flightAvailQuery += " COALESCE(a.departure, date\'" + departIn +  "\') AS departure, ";
			flightAvailQuery += "COALESCE(a.booked, 0) AS booked, Flight.seats, ";
			flightAvailQuery += "COALESCE(Flight.seats - a.booked, Flight.seats) AS available ";
			flightAvailQuery += "FROM Flight ";
			flightAvailQuery += "LEFT JOIN (SELECT Booking.flightNum, Booking.departure, COUNT(Booking.flightNum) as booked ";
			flightAvailQuery += "FROM Booking WHERE Booking.departure = \'" + departIn + "\' ";
			flightAvailQuery += "GROUP BY Booking.flightNum, Booking.departure) AS a ";
			flightAvailQuery += "ON a.flightNum = Flight.flightNum ";
			esql.executeQueryAndPrintResult(flightAvailQuery);

		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
		

		
	}

}

