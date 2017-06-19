package martinlt.cryptomessenger.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class for a Party.
 */
public class Party
{

   private final StringProperty identifier;
   private final StringProperty publicKey;

   /**
    * Default constructor.
    */
   public Party()
   {
      this(null, null);
   }

   /**
    * Constructor with some initial data.
    *
    * @param identifier
    * @param publicKey
    */
   public Party(String identifier, String publicKey)
   {
      this.identifier = new SimpleStringProperty(identifier);
      this.publicKey = new SimpleStringProperty(publicKey);
   }

   public StringProperty identifierProperty()
   {
      return identifier;
   }

   public StringProperty publicKeyProperty()
   {
      return publicKey;
   }

   public String getIdentifier()
   {
      return identifier.get();
   }

   public String getPublicKey()
   {
      return publicKey.get();
   }

   @Override
   public String toString() {
       return identifier.get();
   }


}
