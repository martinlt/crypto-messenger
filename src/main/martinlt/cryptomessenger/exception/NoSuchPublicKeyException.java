package martinlt.cryptomessenger.exception;

public class NoSuchPublicKeyException extends SecurityException
{
   private static final long serialVersionUID = 1L;

   public NoSuchPublicKeyException()
   {

   }

   public NoSuchPublicKeyException(String message)
   {
      super(message);
   }

   public NoSuchPublicKeyException(Throwable cause)
   {
      super(cause);
   }

   public NoSuchPublicKeyException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
