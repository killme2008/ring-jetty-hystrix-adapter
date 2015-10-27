(ns ring-jetty-hystrix-adapter.core
  (:import [org.eclipse.jetty.server
            Handler
            Request
            Server
            ServerConnector
            ConnectionFactory
            HttpConfiguration
            HttpConnectionFactory
            SslConnectionFactory
            SecureRequestCustomizer
            ConnectorStatistics]
           [org.eclipse.jetty.server.handler
            AbstractHandler StatisticsHandler
            ContextHandlerCollection ContextHandler]
           [org.eclipse.jetty.util.thread ThreadPool QueuedThreadPool]
           [org.eclipse.jetty.util.ssl SslContextFactory]
           [org.eclipse.jetty.util.log Log]
           [org.eclipse.jetty.jmx MBeanContainer]
           [java.lang.management ManagementFactory]
           [org.eclipse.jetty.servlet ServletContextHandler ServletHolder]
           [com.netflix.hystrix.contrib.metrics.eventstream HystrixMetricsStreamServlet])
  (:require [ring.util.servlet :as servlet]))

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

(defn- ^ServerConnector server-connector [server & factories]
  (ServerConnector. server (into-array ConnectionFactory factories)))

(defn- ^HttpConfiguration http-config [options]
  (doto (HttpConfiguration.)
    (.setSendDateHeader (:send-date-header? options true))
    (.setOutputBufferSize (:output-buffer-size options 32768))
    (.setRequestHeaderSize (:request-header-size options 8192))
    (.setResponseHeaderSize (:response-header-size options 8192))
    (.setSendServerVersion (:send-server-version? options true))))

(defn- ^ServerConnector http-connector [server options]
  (let [http-factory (HttpConnectionFactory. (http-config options))
        connector (server-connector server http-factory)]
    (when (options :stats? false)
      (.addBean connector (ConnectorStatistics.)))
    (doto connector
      (.setPort (options :port 80))
      (.setHost (options :host))
      (.setIdleTimeout (options :max-idle-time 200000)))))

(defn- ^SslContextFactory ssl-context-factory [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context ^java.security.KeyStore (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (cond
      (string? (options :truststore))
      (.setTrustStorePath context (options :truststore))
      (instance? java.security.KeyStore (options :truststore))
      (.setTrustStore context ^java.security.KeyStore (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- ^ServerConnector ssl-connector [server options]
  (let [ssl-port     (options :ssl-port 443)
        http-factory (HttpConnectionFactory.
                      (doto (http-config options)
                        (.setSecureScheme "https")
                        (.setSecurePort ssl-port)
                        (.addCustomizer (SecureRequestCustomizer.))))
        ssl-factory  (SslConnectionFactory.
                      (ssl-context-factory options)
                      "http/1.1")]
    (doto (server-connector server ssl-factory http-factory)
      (.setPort ssl-port)
      (.setHost (options :host))
      (.setIdleTimeout (options :max-idle-time 200000)))))

(defn- ^ThreadPool create-threadpool [options]
  (let [pool (QueuedThreadPool. ^Integer (options :max-threads 50))]
    (.setMinThreads pool (options :min-threads 8))
    (when (:daemon? options false)
      (.setDaemon pool true))
    pool))

(defn- ^Server create-server [options]
  (let [server (Server. (create-threadpool options))]
    (when (:http? options true)
      (.addConnector server (http-connector server options)))
    (when (or (options :ssl?) (options :ssl-port))
      (.addConnector server (ssl-connector server options)))
    server))

(defn- ^StatisticsHandler create-statistic-handler
  [^Server s app]
  (let [^MBeanContainer mbc (MBeanContainer. (ManagementFactory/getPlatformMBeanServer))]
    (doto s
      (.addEventListener mbc)
      (.addBean mbc)
      (.addBean (Log/getLog)))
    (ConnectorStatistics/addToAllConnectors s)
    (doto (StatisticsHandler.)
      (.setHandler (proxy-handler app)))))

(defn run-jetty-with-hystrix
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:

  :stats          - Whether to add a jetty statistics handler.
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
  (let [^Server s (create-server options)]
    (when-let [configurator (:configurator options)]
      (configurator s))
    (let [hystrix-holder  (ServletHolder. HystrixMetricsStreamServlet)
          hystrix-context (ServletContextHandler. ServletContextHandler/SESSIONS)
          app-context (ContextHandler.)
          contexts (ContextHandlerCollection.)]
      (doto hystrix-context
        (.addServlet  hystrix-holder "/")
        (.setContextPath (:hystrix-servlet-path options "/hystrix")))
      (doto app-context
        (.setContextPath "/")
        (.setHandler
         (if (options :stats false)
           (create-statistic-handler s app)
           (proxy-handler app))))
      (.setHandlers contexts
                    (into-array Handler [hystrix-context app-context]))
      (.setHandler s contexts))
    (.start s)
    (when (:join? options true)
      (.join s))
    s))
