(defproject cn.leancloud/ring-jetty-hystrix-adapter "0.2.6"
  :description "Setup a Hystrix (https://github.com/Netflix/Hystrix) event stream with jetty for clojure."
  :url "http://github.com/leancloud/ring-jetty-hystrix-adapter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java_src"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.eclipse.jetty/jetty-server "9.2.10.v20150310"]
                 [ring/ring-servlet "1.4.0"]
                 [org.eclipse.jetty/jetty-servlet "9.2.10.v20150310"]
                 [org.eclipse.jetty/jetty-jmx "9.2.10.v20150310"]
                 [com.netflix.hystrix/hystrix-clj "1.5.11"]
                 [com.netflix.hystrix/hystrix-metrics-event-stream "1.5.11"]])
