
package akka;

import akka.http.model.japi.HttpMethods;
import akka.http.model.japi.HttpRequest;
import akka.http.model.japi.HttpResponse;
import akka.http.model.japi.StatusCodes;
import akka.http.model.japi.Uri;
import akka.http.model.japi.Util;
import akka.http.model.japi.headers.Authorization;
import akka.http.model.japi.headers.HttpCredentials;

public class JavaApiTestCases
{
   /** Builds a request for use on the client side */
   public static HttpRequest buildRequest()
   {
      return HttpRequest.create().withMethod(HttpMethods.POST).withUri("/send");
   }

   /** A simple handler for an Http server */
   public static HttpResponse handleRequest(HttpRequest request)
   {
      if (request.method() == HttpMethods.GET)
      {
         Uri uri = request.getUri();
         if (uri.path().equals("/hello"))
         {
            String name = Util.getOrElse(uri.parameter("name"), "Mister X");

            return HttpResponse.create().withEntity("Hello " + name + "!");
         }
         else
            return HttpResponse.create().withStatus(404).withEntity("Not found");
      }
      else
         return HttpResponse.create().withStatus(StatusCodes.METHOD_NOT_ALLOWED).withEntity("Unsupported method");
   }

   /** Adds authentication to an existing request */
   public static HttpRequest addAuthentication(HttpRequest request)
   {
      return request.addHeader(Authorization.create(HttpCredentials.createBasicHttpCredential("username", "password")));
   }

   /** Removes cookies from an existing request */
   public static HttpRequest removeCookies(HttpRequest request)
   {
      return request.removeHeader("Cookie");
   }

   /** Build a uri to send a form */
   public static Uri createUriForOrder(String orderId, String price, String amount)
   {
      return Uri.create().path("/order").addParameter("orderId", orderId).addParameter("price", price)
         .addParameter("amount", amount);
   }

   public static Uri addSessionId(Uri uri)
   {
      return uri.addParameter("session", "abcdefghijkl");
   }
}
