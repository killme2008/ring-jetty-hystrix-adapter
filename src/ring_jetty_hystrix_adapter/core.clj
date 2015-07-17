(ns ring-jetty-hystrix-adapter.core
  (:import (org.eclipse.jetty.server Server Connector Request Handler)
           (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.server.handler AbstractHandler ContextHandlerCollection ContextHandler)
           [org.eclipse.jetty.servlet ServletContextHandler ServletHolder]
           [com.netflix.hystrix.contrib.metrics.eventstream HystrixMetricsStreamServlet]
           (org.eclipse.jetty.server.nio SelectChannelConnector))
  (:require [ring.util.servlet :as servlet]
            [ring.adapter.jetty :as jetty]))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (servlet/update-servlet-response response response-map)
          (.setHandled base-request true))))))


(defn run-jetty-with-hystrix
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:

  :hystrix-servlet-path - hystrix event stream serlvet path, default is /hystrix.stream
  :configurator   - a function called with the Jetty Server instance
  :port           - the port to listen on (defaults to 80)
  :host           - the hostname to listen on
  :join?          - blocks the thread until server ends (defaults to true)
  :daemon?        - use daemon threads (defaults to false)
  :ssl?           - allow connections over HTTPS
  :ssl-port       - the SSL port to listen on (defaults to 443, implies :ssl?)
  :keystore       - the keystore to use for SSL connections
  :key-password   - the password to the keystore
  :truststore     - a truststore to use for SSL connections
  :trust-password - the password to the truststore
  :max-threads    - the maximum number of threads to use (default 50)
  :min-threads    - the minimum number of threads to use (default 8)
  :max-queued     - the maximum number of requests to queue (default unbounded)
  :max-idle-time  - the maximum idle time in milliseconds for a connection (default 200000)
  :client-auth    - SSL client certificate authenticate, may be set to :need,
                    :want or :none (defaults to :none)"
  [app options]
  (let [^Server s (#'jetty/create-server options)
        ^QueuedThreadPool p (QueuedThreadPool. ^Integer (options :max-threads 50))]
    (when (:daemon? options false)
      (.setDaemon p true))
    (when-let [configurator (:configurator options)]
      (configurator s))
    (let [hystrix-holder  (ServletHolder. HystrixMetricsStreamServlet)
          hystrix-context (ServletContextHandler. ServletContextHandler/SESSIONS)
          app-context (ContextHandler.)
          contexts (ContextHandlerCollection.)]
      (.addServlet hystrix-context hystrix-holder (:hystrix-servlet-path options "/hystrix.stream"))
      (doto app-context
        (.setContextPath "/")
        (.setHandler (proxy-handler app)))
      (.setHandlers contexts
                    (into-array Handler [hystrix-context app-context]))
      (doto s
        (.setThreadPool p)
        (.setHandler contexts)))
    (.start s)
    (when (:join? options true)
      (.join s))
    s))
