(defproject ring-jetty-hystrix-adapter "0.2.0-beta1"
  :description "Setup a Hystrix (https://github.com/Netflix/Hystrix) event stream with jetty for clojure."
  :url "http://github.com/killme2008/ring-jetty-hystrix-adapter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
                 [ring/ring-servlet "1.4.0"]
                 [org.eclipse.jetty/jetty-servlet "9.2.10.v20150310"]
                 [org.eclipse.jetty/jetty-jmx "9.2.10.v20150310"]
                 [hystrix-event-stream-clj "0.1.3"
                  :exclusions [com.netflix.hystrix/hystrix-clj]]
                 [com.netflix.hystrix/hystrix-clj "1.4.11"]])
