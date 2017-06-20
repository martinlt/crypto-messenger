package martinlt.cryptomessenger.exception;

public class SecurityException extends Exception
{
   private static final long serialVersionUID = 1L;

   public SecurityException()
   {

   }

   public SecurityException(String message)
   {
      super(message);
   }

   public SecurityException(Throwable cause)
   {
      super(cause);
   }

   public SecurityException(String message, Throwable cause)
   {
      super(message, cause);
   }

}
