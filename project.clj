(defproject ring-jetty-hystrix-adapter "0.1.0"
  :description "Setup a Hystrix (https://github.com/Netflix/Hystrix) event stream with jetty for clojure."
  :url "http://github.com/killme2008/ring-jetty-hystrix-adapter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.eclipse.jetty/jetty-servlet "7.6.13.v20130916"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [hystrix-event-stream-clj "0.1.3"
                  :exclusions [com.netflix.hystrix/hystrix-clj]]
                 [com.netflix.hystrix/hystrix-clj "1.4.11"]])
