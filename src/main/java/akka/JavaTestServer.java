/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka;

import static akka.pattern.Patterns.ask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.Foreach;
import akka.http.model.japi.Http;
import akka.http.model.japi.HttpRequest;
import akka.http.model.japi.HttpResponse;
import akka.http.model.japi.IncomingConnection;
import akka.http.model.japi.ServerBinding;
import akka.stream.javadsl.Source;
import akka.stream.scaladsl2.Drain;
import akka.stream.scaladsl2.FlowMaterializer;
import akka.stream.scaladsl2.SimpleDrain;

public abstract class JavaTestServer
{
   public static class Foo<In> extends akka.stream.javadsl.SimpleDrain<In>
   {
      private final SimpleDrain<In> delegate;

      public static <In> Foo<In> create(Subscriber<In> subscriber)
      {
         return new Foo<In>(new akka.stream.scaladsl2.SubscriberDrain<In>(subscriber));
      }

      public Foo(SimpleDrain<In> delegate)
      {
         this.delegate = delegate;
      }

      @Override
      public SimpleDrain<In> asScala()
      {
         return delegate;
      }

      @Override
      public Drain<In> delegate()
      {
         return delegate;
      }
   }

   public static void main(String[] args) throws IOException, InterruptedException
   {
      ActorSystem system = ActorSystem.create();

      final FlowMaterializer materializer = FlowMaterializer.create(system);

      ActorRef httpManager = Http.get(system).manager();
      Future<Object> binding = ask(httpManager, Http.bind("localhost", 8080), 1000);
      binding.foreach(new Foreach<Object>()
      {
         @Override
         public void each(Object result) throws Throwable
         {
            ServerBinding binding = (ServerBinding) result;
            System.out.println("Bound to " + binding.localAddress());

            Source.from(binding.getConnectionStream()).foreach(
               new akka.stream.javadsl.japi.Procedure<IncomingConnection>()
               {
                  @Override
                  public void apply(IncomingConnection conn) throws Exception
                  {
                     System.out.println("New incoming connection from " + conn.remoteAddress());

                     Subscriber<HttpResponse> responseSubscriber = conn.getResponseSubscriber();

                     Foo<HttpResponse> drain = Foo.create(responseSubscriber);// SubscriberDrain.create(responseSubscriber);

                     Publisher<HttpRequest> requestPublisher = conn.getRequestPublisher();

                     Source.from(requestPublisher)
                        .map(new akka.stream.javadsl.japi.Function<HttpRequest, HttpResponse>()
                        {
                           @Override
                           public HttpResponse apply(HttpRequest request) throws Exception
                           {
                              System.out.println("Handling request to " + request.getUri());
                              return JavaApiTestCases.handleRequest(request);
                           }
                        }).runWith(drain, materializer);
                  }
               }, materializer);
         }
      }, system.dispatcher());

      System.out.println("Press ENTER to stop.");
      new BufferedReader(new InputStreamReader(System.in)).readLine();

      system.shutdown();
   }
}
