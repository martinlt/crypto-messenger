package uk.co.myeyesonly.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import uk.co.myeyesonly.MainApp;
import uk.co.myeyesonly.model.Party;

public class PartyOverviewController
{
   @FXML
   private ComboBox<Party> partyComboBox;

   @FXML
   private Label publicKeyLabel;

   @FXML
   private Label outputLabel;

   @FXML
   private TextArea messageLabel;

   // Reference to the main application.
   private MainApp mainApp;

   /**
    * The constructor. The constructor is called before the initialize() method.
    */
   public PartyOverviewController()
   {
   }

   /**
    * Initializes the controller class. This method is automatically called
    * after the fxml file has been loaded.
    */
   @FXML
   private void initialize()
   {
      // Clear party details.
      showPartyDetails(null);
   }

   /**
    * Is called by the main application to give a reference back to itself.
    *
    * @param mainApp
    */
   public void setMainApp(MainApp mainApp)
   {
      this.mainApp = mainApp;

      ObservableList<Party> partyData = mainApp.getPartyData();

      if (partyData == null)
         System.out.println("ERROR: no parties found");
      else {

         // Add observable list data to the table
         partyComboBox.setItems(this.mainApp.getPartyData());
      }
   }

   /**
    * Fills all text fields to show details about the party. If the specified
    * party is null, all text fields are cleared.
    *
    * @param party
    *           the party or null
    */
   private void showPartyDetails(Party party)
   {
      if (party != null) {
         // Fill the labels with info from the party object.
         publicKeyLabel.setText(party.getPublicKey());

      } else {
         // Party is null, remove all the text.
         publicKeyLabel.setText("");

      }
   }

   @FXML
   private void handleComboBoxAction()
   {
      showPartyDetails(partyComboBox.getSelectionModel().getSelectedItem());
   }

   @FXML
   private void handleCopyToClipboard()
   {
      final Clipboard clipboard = Clipboard.getSystemClipboard();
      final ClipboardContent content = new ClipboardContent();
      content.putString(outputLabel.getText());
      clipboard.setContent(content);
   }

   @FXML
   private void handleEncrypt()
   {
      Party party = partyComboBox.getSelectionModel().getSelectedItem();
      if(party != null) {
         String message = messageLabel.getText();
         if(message != null) {
            try {
            mainApp.encryptMessage(message, party.getIdentifier());

            outputLabel.setText(mainApp.getCipherText());
            } catch(Exception ex) {
               ex.printStackTrace();
            }

         }
      }
   }
}
