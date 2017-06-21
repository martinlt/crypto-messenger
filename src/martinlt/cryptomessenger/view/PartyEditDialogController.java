package martinlt.cryptomessenger.view;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import martinlt.cryptomessenger.model.Party;

/**
 * Dialog to edit details of a party.
 */
public class PartyEditDialogController
{

   @FXML
   private TextField identifierField;
   @FXML
   private TextArea publicKeyField;
   @FXML
   private Label identifierLabel;

   private Stage dialogStage;
   private Party party;
   private boolean okClicked = false;

   /**
    * Initializes the controller class. This method is automatically called
    * after the fxml file has been loaded.
    */
   @FXML
   private void initialize()
   {
   }

   /**
    * Sets the stage of this dialog.
    *
    * @param dialogStage
    */
   public void setDialogStage(Stage dialogStage)
   {
      this.dialogStage = dialogStage;
   }

   /**
    * Sets the party to be edited in the dialog.
    *
    * @param party
    */
   public void setParty(Party party)
   {
      this.party = party;

      identifierField.setText(party.getIdentifier());
      publicKeyField.setText(party.getPublicKey());
   }

   /**
    * Returns true if the user clicked OK, false otherwise.
    *
    * @return
    */
   public boolean isOkClicked()
   {
      return okClicked;
   }

   /**
    * Called when the user clicks ok.
    */
   @FXML
   private void handleOk()
   {
      if (isInputValid()) {
         party.setIdentifier(identifierField.getText());
         // party.setPublicKey(publicKeyField.getText().replace(NEWLINE, ""));
         party.setPublicKey(publicKeyField.getText());

         okClicked = true;
         dialogStage.close();
      }
   }

   /**
    * Called when the user clicks cancel.
    */
   @FXML
   private void handleCancel()
   {
      dialogStage.close();
   }

   /**
    * Validates the user input in the text fields.
    *
    * @return true if the input is valid
    */
   private boolean isInputValid()
   {
      String errorMessage = "";

      if (identifierField.getText() == null || identifierField.getText().length() == 0) {
         errorMessage += "No valid identifier!\n";
      }
      if (publicKeyField.getText() == null || publicKeyField.getText().length() == 0) {
         errorMessage += "No valid public key!\n";
      }

      if (errorMessage.length() == 0) {
         return true;
      } else {
         // Show the error message.
         Alert alert = new Alert(AlertType.ERROR);
         alert.initOwner(dialogStage);
         alert.setTitle("Invalid Fields");
         alert.setHeaderText("Please correct invalid fields");
         alert.setContentText(errorMessage);

         alert.showAndWait();

         return false;
      }
   }

   public void disableIdentifier()
   {
      this.identifierField.setEditable(false);
      this.identifierField.setVisible(false);
      this.identifierLabel.setText(party.getIdentifier());
      this.identifierLabel.setVisible(true);
   }

   public void enableIdentifier()
   {
      this.identifierField.setEditable(true);
      this.identifierField.setVisible(true);
      this.identifierLabel.setText("");
      this.identifierLabel.setVisible(false);
   }
}
